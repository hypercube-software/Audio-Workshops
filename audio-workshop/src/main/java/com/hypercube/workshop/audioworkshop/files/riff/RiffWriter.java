package com.hypercube.workshop.audioworkshop.files.riff;

import com.hypercube.workshop.audioworkshop.common.format.PCMFormat;
import com.hypercube.workshop.audioworkshop.common.pcm.PCMMarker;
import com.hypercube.workshop.audioworkshop.files.io.SeekableBinaryOutputStream;
import com.hypercube.workshop.audioworkshop.files.riff.chunks.Chunks;
import com.hypercube.workshop.audioworkshop.files.riff.chunks.RiffFmtChunk;
import com.hypercube.workshop.audioworkshop.files.riff.chunks.markers.adtl.CuePointLabel;
import com.hypercube.workshop.audioworkshop.files.riff.chunks.markers.cue.CuePoint;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Stack;
import java.util.stream.IntStream;

@Slf4j
public class RiffWriter implements Closeable {

    @Getter
    private final File file;

    private SeekableBinaryOutputStream out;

    private Stack<Long> chunkSizeStack = new Stack<>();

    public RiffWriter(File file) throws IOException {
        this.file = file;
        file.getParentFile()
                .mkdirs();
        openWAVE();
    }

    private void openWAVE() throws IOException {
        out = new SeekableBinaryOutputStream(new FileOutputStream(file));
        beginChunk(RiffConstants.RIFF);
        out.writeChunkId(Chunks.WAVE);
    }

    public void beginChunk(String chunkId) throws IOException {
        align();
        out.writeChunkId(chunkId);
        chunkSizeStack.push(out.position());
        out.writeIntLE(0);
    }

    public void endChunk() throws IOException {
        long end = out.position();
        long pos = chunkSizeStack.pop();
        long size = end - pos - 4;
        out.seek(pos);
        out.writeIntLE((int) size);
        out.seek(end);
    }

    public void writeFmtChunk(PCMFormat format) throws IOException {
        RiffFmtChunk fmt = new RiffFmtChunk(format.getNbChannels(), format.getSampleRate(), format.getBitDepth(), WaveGUIDCodecs.WMMEDIASUBTYPE_PCM);
        writeFmtChunk(fmt);
    }

    public void writeFmtChunk(RiffFmtChunk fmt) throws IOException {
        final int WAVEFORMATEXTENSIBLE_SIZE = 40;
        final int WAVEFORMATEX_SIZE = 18;

        beginChunk(fmt.getId());
        // WAVEFORMATEX https://learn.microsoft.com/en-us/previous-versions/dd757713(v=vs.85)
        out.writeShortLE(fmt.getFormatTag());
        out.writeShortLE(fmt.getNChannels());
        out.writeIntLE(fmt.getNSamplesPerSec());
        out.writeIntLE(fmt.getNAvgBytesPerSec());
        out.writeShortLE(fmt.getNBlockAlign());
        out.writeShortLE(fmt.getBitsPerSample());
        if (fmt.getCodec() != null) {
            out.writeShortLE(WAVEFORMATEXTENSIBLE_SIZE - WAVEFORMATEX_SIZE);
            // WAVEFORMATEXTENSIBLE https://learn.microsoft.com/en-us/windows/win32/api/mmreg/ns-mmreg-waveformatextensible?redirectedfrom=MSDN
            out.writeShortLE(fmt.getBitsPerSample());
            out.writeIntLE(fmt.getChannelMask());
            out.writeBytes(WaveGUIDCodecs.getBytes(fmt.getCodec()));
        }
        endChunk();
    }

    @Override
    public void close() throws IOException {
        endChunk();
        out.close();
    }

    private void align() throws IOException {
        if (out.position() % 2 != 0) {
            out.writeByte(0);
        }
    }

    public void writeIXML(String xml) throws IOException {
        beginChunk(Chunks.IXML);
        out.writeBytes(xml.getBytes(StandardCharsets.US_ASCII));
        endChunk();
    }

    /**
     * Save markers using CUE and ADTL chunks
     *
     * @param markers A map of marker label -> sample position
     */
    public void writeMarkers(List<PCMMarker> markers) throws IOException {
        var ordererdMarkers = markers.stream()
                .sorted((m1, m2) -> Long.compare(m1.samplePosition(), m2.samplePosition()))
                .toList();

        var cuePoints = IntStream.range(0, ordererdMarkers.size())
                .boxed()
                .map(cuePointId ->
                {
                    var marker = ordererdMarkers.get(cuePointId);
                    return new CuePoint(cuePointId + 1, 0, Chunks.DATA, 0, 0, (int) marker.samplePosition());
                })
                .toList();
        writeCue(cuePoints);

        var adtLabels = IntStream.range(0, ordererdMarkers.size())
                .boxed()
                .map(cuePointId ->
                {
                    var marker = ordererdMarkers.get(cuePointId);
                    return new CuePointLabel(cuePointId + 1, marker.label());
                })
                .toList();
        writeAdtLabels(adtLabels);
    }

    private void writeAdtLabels(List<CuePointLabel> adtLabels) throws IOException {
        beginChunk(Chunks.LIST);
        out.writeChunkId(Chunks.LIST_TYPE_ADTL);
        for (CuePointLabel c : adtLabels) {
            beginChunk(Chunks.ADTL_LABEL);
            out.writeIntLE(c.dwIdentifier());
            out.writeASCII(c.label());
            endChunk();
        }
        endChunk();
    }

    private void writeCue(List<CuePoint> cuePoints) throws IOException {
        beginChunk(Chunks.CUE);
        out.writeIntLE(cuePoints.size());
        for (CuePoint c : cuePoints) {
            out.writeIntLE(c.identifier());
            out.writeIntLE(c.position());
            out.writeChunkId(c.chunkId());
            out.writeIntLE(c.chunkStart());
            out.writeIntLE(c.blockStart());
            out.writeIntLE(c.sampleOffset());
        }
        endChunk();
    }

    /**
     * Write a LIST CHUNK width multiple sub chunks INFO
     *
     * @param infos a list of key1,value1,key2,value2,key3,value3...
     */
    public void writeLISTInfo(List<String> infos) throws IOException {
        beginChunk(Chunks.LIST);
        out.writeChunkId(Chunks.LIST_TYPE_INFO);
        for (int i = 0; i < infos.size(); i += 2) {
            String field = infos.get(i + 0);
            String value = infos.get(i + 1);
            beginChunk(field);
            out.writeASCII(value);
            endChunk();
        }
        endChunk();
    }

    public void write(byte[] data, int nbRead) throws IOException {
        out.writeBytes(data, nbRead);
    }

    public void write(byte[] data, int offset, int nbRead) throws IOException {
        out.writeBytes(data, offset, nbRead);
    }

    public long getPosition() throws IOException {
        return out.position();
    }

    public void writeShortLE(int value) throws IOException {
        out.writeShortLE(value);
    }

    public void writeShortBE(int value) throws IOException {
        out.writeShortBE(value);
    }

    public void writeIntLE(int value) throws IOException {
        out.writeIntLE(value);
    }

    public void writeIntBE(int value) throws IOException {
        out.writeIntBE(value);
    }

    public void writeByte(int value) throws IOException {
        out.writeByte(value);
    }

    public void writeBytes(byte[] pcmData) throws IOException {
        out.writeBytes(pcmData);
    }
}
