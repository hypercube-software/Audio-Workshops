package com.hypercube.workshop.audioworkshop.files.riff.chunks;

import com.hypercube.workshop.audioworkshop.files.riff.WaveChannels;
import com.hypercube.workshop.audioworkshop.files.riff.WaveCodecs;
import com.hypercube.workshop.audioworkshop.files.riff.WaveGUIDCodecs;
import lombok.Getter;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

@Getter
public class RiffFmtChunk extends RiffChunk {
    public static final int CONTENT_SIZE = 40;
    public static final int WAVEFORMATEX_SIZE = 18;
    final int wFormatTag;
    final int nChannels;
    final int nSamplesPerSec;
    final int nAvgBytesPerSec;
    final int nBlockAlign;
    final int wBitsPerSample;
    final int dwChannelMask;
    final UUID codec;

    public RiffFmtChunk(int nChannels, int nSamplesPerSec, int wBitsPerSample, UUID codec) {
        super(null, Chunks.FORMAT, 0, CONTENT_SIZE);
        this.wFormatTag = WaveCodecs.WAVE_FORMAT_EXTENSIBLE.getValue();
        this.nChannels = nChannels;
        this.nSamplesPerSec = nSamplesPerSec;
        this.nBlockAlign = (nChannels * wBitsPerSample) / 8;
        this.nAvgBytesPerSec = nSamplesPerSec * nBlockAlign;
        this.wBitsPerSample = wBitsPerSample;
        this.dwChannelMask = WaveChannels.toMask(nChannels);
        this.codec = codec;
    }

    public byte[] getBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(CONTENT_SIZE);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        // WAVEFORMATEX https://learn.microsoft.com/en-us/previous-versions/dd757713(v=vs.85)
        buffer.putShort((short) wFormatTag);
        buffer.putShort((short) nChannels);
        buffer.putInt(nSamplesPerSec);
        buffer.putInt(nAvgBytesPerSec);
        buffer.putShort((short) nBlockAlign);
        buffer.putShort((short) wBitsPerSample);
        buffer.putShort((short) (CONTENT_SIZE - WAVEFORMATEX_SIZE));
        // WAVEFORMATEXTENSIBLE https://learn.microsoft.com/en-us/windows/win32/api/mmreg/ns-mmreg-waveformatextensible?redirectedfrom=MSDN
        buffer.putShort((short) wBitsPerSample);
        buffer.putInt(dwChannelMask);
        buffer.put(WaveGUIDCodecs.getBytes(codec));
        return buffer.array();
    }
}
