package com.hypercube.workshop.audioworkshop.api.format;

import com.hypercube.workshop.audioworkshop.api.pcm.BitDepth;
import com.hypercube.workshop.audioworkshop.api.pcm.PCMEncoding;
import lombok.Getter;

import javax.sound.sampled.AudioFormat;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * This class is all about the specification of audio format
 * <pre>It is used as a replacement of the JDK one {@link AudioFormat} which not handy to use in many aspects
 */
@Getter
public class PCMFormat {

    protected final int sampleRate;
    protected final BitDepth bitDepth;
    protected final int nbChannels;
    protected final PCMEncoding encoding;
    protected final ByteOrder byteOrder;
    protected final int bytesPerSamples;

    public PCMFormat(int sampleRate, BitDepth bitDepth, int nbChannels, PCMEncoding encoding, ByteOrder byteOrder) {
        if (sampleRate < 8000) {
            throw new IllegalArgumentException("Sample rate seems wrong: %d".formatted(sampleRate));
        }
        this.sampleRate = sampleRate;
        this.bitDepth = bitDepth;
        this.nbChannels = nbChannels;
        this.encoding = encoding;
        this.byteOrder = byteOrder;
        this.bytesPerSamples = bitDepth.getBytes();
    }

    public AudioFormat getAudioFormat() {
        return new AudioFormat(sampleRate, bitDepth.getBits(), nbChannels, encoding == PCMEncoding.SIGNED, byteOrder == ByteOrder.BIG_ENDIAN);
    }

    public boolean isBigEndian() {
        return byteOrder == ByteOrder.BIG_ENDIAN;
    }

    public int getSampleSizeInBits() {
        return bitDepth.getBits();
    }

    public ByteBuffer wrapPCMBuffer(byte[] buffer) {
        return ByteBuffer.wrap(buffer)
                .order(byteOrder);
    }


    public int getFrameSizeInBytes() {
        return bytesPerSamples * nbChannels;
    }

    public int millisecondsToSamples(double ms) {
        return (int) (sampleRate * ms / 1000.0f);
    }

    public double samplesToMilliseconds(long sample) {
        return sample * 1000.0f / sampleRate;
    }

    public static double toDb(double sample) {
        return 20 * Math.log10(sample);
    }
}
