package com.hypercube.workshop.audioworkshop.common.line;

import com.hypercube.workshop.audioworkshop.common.pcm.BitDepth;
import lombok.Getter;

import javax.sound.sampled.AudioFormat;
import java.nio.ByteOrder;

@Getter
public abstract class AudioLine {
    protected final ByteOrder order;
    protected final AudioFormat format;
    protected final int sampleRate;
    protected final BitDepth bitDepth;
    protected final int nbChannels;
    protected final int sampleBufferSize;
    protected final int byteBufferSize;
    protected final int bytesPerSamples;

    protected AudioLine(AudioFormat format, int bufferDurationMs) {
        this.sampleRate = (int) format.getSampleRate();
        this.nbChannels = format.getChannels();
        this.bitDepth = BitDepth.valueOf(format.getSampleSizeInBits());
        this.sampleBufferSize = (bufferDurationMs * sampleRate) / 1000;
        this.bytesPerSamples = bitDepth.getBytes();
        this.byteBufferSize = sampleBufferSize * bytesPerSamples * format.getChannels();
        this.order = format.isBigEndian() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
        this.format = format;
    }
}
