package com.hypercube.workshop.audioworkshop.files.flac;

import com.hypercube.workshop.audioworkshop.files.io.SeekableBinaryOutputStream;
import com.hypercube.workshop.audioworkshop.files.riff.RiffFileInfo;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
public class FlacWriter implements Closeable {
    @Getter
    private final File file;
    private SeekableBinaryOutputStream out;

    public FlacWriter(File file) throws IOException {
        this.file = file;
        open();
    }

    private void open() throws IOException {
        out = new SeekableBinaryOutputStream(new FileOutputStream(file));
        out.writeChunkId("fLaC");
    }

    public void writeBlock(boolean lastOne, FlacBlockType type, byte[] block)
            throws IOException {
        if (lastOne)
            out.writeByte(type.ordinal() | 0x80);
        else
            out.writeByte(type.ordinal() & 0x7F);
        out.writeInt24BE(block.length);
        out.writeBytes(block);
    }

    /**
     * Write a custom APPLICATION chunk containing the original WAV data surrounding the PCM data
     */
    public void writeORGD(RiffFileInfo origMeta) throws IOException {
        out.writeByte(FlacBlockType.APPLICATION.ordinal() | 0x80);
        long p0 = out.position();
        out.writeInt24BE(0);
        long p1 = out.position();
        out.writeChunkId("ORGD");
        byte[] strData = origMeta.getFilename()
                .getBytes(StandardCharsets.UTF_8);
        out.writeIntBE(strData.length);
        out.writeBytes(strData);
        out.writeIntBE(origMeta.getProlog().length);
        out.writeBytes(origMeta.getProlog());
        out.writeIntBE(origMeta.getEpilog().length);
        out.writeBytes(origMeta.getEpilog());
        long p2 = out.position();
        long remain = (p2 - p1) % 8;
        out.seek(p0);
        int blockSize = (int) (p2 - p1 + remain);
        out.writeInt24BE(blockSize);
        log.trace("Application ORGD size: %d/0x%X".formatted(blockSize, blockSize));
        out.seek(p2);
        out.writeZeros((int) remain);
    }

    public void writeBytes(byte[] data, int size) throws IOException {
        out.writeBytes(data, size);
    }

    // https://xiph.org/vorbis/doc/v-comment.html
    public void writeVorbisTags(Map<String, String> map) throws IOException {
        out.writeByte(FlacBlockType.VORBIS_COMMENT.ordinal());
        long p1 = out.position();
        out.writeInt24BE(0);
        writeVorbisString("media-transcoder");
        out.writeIntLE(map.size());

        for (var entry : map.entrySet()) {
            writeVorbisString(entry.getKey() + "=" + entry.getValue());
        }
        // there is no framing_bit in FLAC
        long p2 = out.position();
        out.seek(p1);
        int blockSize = (int) (p2 - p1 - 3);
        out.writeInt24BE(blockSize);
        log.trace("Vorbis tags size: %d/0x%X".formatted(blockSize, blockSize));
        out.seek(p2);
    }

    private void writeVorbisString(String value) throws IOException {
        byte[] str = value.getBytes(StandardCharsets.UTF_8);
        out.writeIntLE(str.length);
        out.writeBytes(str);
    }

    @Override
    public void close() throws IOException {
        if (out != null) {
            out.close();
        }
    }
}
