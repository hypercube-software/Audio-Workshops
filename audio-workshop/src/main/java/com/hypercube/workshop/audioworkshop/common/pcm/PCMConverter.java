package com.hypercube.workshop.audioworkshop.common.pcm;

import com.hypercube.workshop.audioworkshop.common.format.PCMFormat;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.nio.ByteBuffer;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PCMConverter {
    private static final NumberFormat formatter = NumberFormat.getNumberInstance(Locale.ENGLISH);

    static {
        formatter.setMaximumFractionDigits(28);
    }

    public static String format(float value) {
        return formatter.format(value);
    }

    public static String format(double value) {
        return formatter.format(value);
    }

    public static List<String> format(float[] values) {
        return IntStream.range(0, values.length)
                .boxed()
                .map(i -> formatter.format(values[i]))
                .toList();
    }

    public static List<String> format(double[] values) {
        return Arrays.stream(values)
                .boxed()
                .map(formatter::format)
                .toList();
    }

    public static PCMtoSampleFunction getPCMtoSampleFunction(PCMFormat format) {
        switch (format.getEncoding()) {
            case SIGNED -> {
                switch (format.getBitDepth()) {
                    case BIT_DEPTH_8 -> {
                        return PCMConverter::signed8BitToSample;
                    }
                    case BIT_DEPTH_16 -> {
                        return PCMConverter::signed16BitToSample;
                    }
                    case BIT_DEPTH_24 -> {
                        return PCMConverter::signed24BitToSample;
                    }
                    case BIT_DEPTH_32 -> {
                        return PCMConverter::signed32BitToSample;
                    }
                }
            }
            case UNSIGNED -> {
                switch (format.getBitDepth()) {
                    case BIT_DEPTH_8 -> {
                        return PCMConverter::unsigned8BitToSample;
                    }
                    case BIT_DEPTH_16 -> {
                        return PCMConverter::unsigned16BitToSample;
                    }
                    case BIT_DEPTH_24 -> {
                        return PCMConverter::unsigned24BitToSample;
                    }
                    case BIT_DEPTH_32 -> {
                        return PCMConverter::unsigned32BitToSample;
                    }
                }
            }
        }
        throw new UnsupportedOperationException();
    }

    public static SampleToPCMFunction getSampleToPCMFunction(PCMFormat format) {
        switch (format.getEncoding()) {
            case SIGNED -> {
                switch (format.getBitDepth()) {
                    case BIT_DEPTH_8 -> {
                        return PCMConverter::sampleToSigned8Bits;
                    }
                    case BIT_DEPTH_16 -> {
                        return PCMConverter::sampleToSigned16Bits;
                    }
                    case BIT_DEPTH_24 -> {
                        return PCMConverter::sampleToSigned24Bits;
                    }
                    case BIT_DEPTH_32 -> {
                        throw new UnsupportedOperationException();
                    }
                }
            }
            case UNSIGNED -> {
                switch (format.getBitDepth()) {
                    case BIT_DEPTH_8 -> {
                        throw new UnsupportedOperationException();
                    }
                    case BIT_DEPTH_16 -> {
                        throw new UnsupportedOperationException();
                    }
                    case BIT_DEPTH_24 -> {
                        throw new UnsupportedOperationException();
                    }
                    case BIT_DEPTH_32 -> {
                        throw new UnsupportedOperationException();
                    }
                }
            }
        }
        throw new UnsupportedOperationException();
    }

    public static void sampleToSigned8Bits(double[][] normalizedInput, ByteBuffer pcmBuffer, int nbSamples, int nbChannels) {
        pcmBuffer.rewind();
        for (int s = 0; s < nbSamples; s++) {
            for (int channel = 0; channel < nbChannels; channel++) {
                double sample = normalizedInput[channel][s] * (double) 0x80;
                pcmBuffer.put((byte) sample);
            }
        }
    }

    public static void sampleToSigned16Bits(double[][] normalizedInput, ByteBuffer pcmBuffer, int nbSamples, int nbChannels) {
        pcmBuffer.rewind();
        for (int s = 0; s < nbSamples; s++) {
            for (int channel = 0; channel < nbChannels; channel++) {
                double sample = normalizedInput[channel][s] * (double) 0x8000;
                pcmBuffer.putShort((short) sample);
            }
        }
    }

    public static void sampleToSigned24Bits(double[][] normalizedInput, ByteBuffer pcmBuffer, int nbSamples, int nbChannels) {
        pcmBuffer.rewind();
        for (int s = 0; s < nbSamples; s++) {
            for (int channel = 0; channel < nbChannels; channel++) {
                int sample = (int) (normalizedInput[channel][s] * (double) 0x800000);
                pcmBuffer.putShort((short) (sample >> 8));
                pcmBuffer.put((byte) (sample & 0xff));
            }
        }
    }

    private static void signed8BitToSample(ByteBuffer pcmBuffer, double[][] normalizedOutput, int nbSamples, int nbChannels) {
        pcmBuffer.rewind();
        for (int s = 0; s < nbSamples; s++) {
            for (int channel = 0; channel < nbChannels; channel++) {
                int sample = pcmBuffer.get();
                normalizedOutput[channel][s] = sample / (double) 0x80;
            }
        }
    }

    private static void signed16BitToSample(ByteBuffer pcmBuffer, double[][] normalizedOutput, int nbSamples, int nbChannels) {
        pcmBuffer.rewind();
        for (int s = 0; s < nbSamples; s++) {
            for (int channel = 0; channel < nbChannels; channel++) {
                int sample = pcmBuffer.getShort();
                normalizedOutput[channel][s] = sample / (double) 0x8000;
            }
        }
    }

    private static void signed24BitToSample(ByteBuffer pcmBuffer, double[][] normalizedOutput, int nbSamples, int nbChannels) {
        pcmBuffer.rewind();
        for (int s = 0; s < nbSamples; s++) {
            for (int channel = 0; channel < nbChannels; channel++) {
                int sample_h = pcmBuffer.get() & 0xFF;
                int sample_l = pcmBuffer.getShort() & 0xFFFF;
                int sample = (sample_h << 16 | sample_l);
                if ((sample & 0x800000) != 0) {
                    sample = sample | 0xFF000000;
                }
                normalizedOutput[channel][s] = sample / (double) 0x800000;
            }
        }
    }

    private static void signed32BitToSample(ByteBuffer pcmBuffer, double[][] normalizedOutput, int nbSamples, int nbChannels) {
        pcmBuffer.rewind();
        for (int s = 0; s < nbSamples; s++) {
            for (int channel = 0; channel < nbChannels; channel++) {
                int sample = pcmBuffer.getInt();
                normalizedOutput[channel][s] = sample / (double) 0x80000000;
            }
        }
    }

    private static void unsigned8BitToSample(ByteBuffer pcmBuffer, double[][] normalizedOutput, int nbSamples, int nbChannels) {
        pcmBuffer.rewind();
        for (int s = 0; s < nbSamples; s++) {
            for (int channel = 0; channel < nbChannels; channel++) {
                int sample = (pcmBuffer.get() & 0xFF) - 0x80;
                normalizedOutput[channel][s] = sample / (double) 0x80;
            }
        }
    }

    private static void unsigned16BitToSample(ByteBuffer pcmBuffer, double[][] normalizedOutput, int nbSamples, int nbChannels) {
        pcmBuffer.rewind();
        for (int s = 0; s < nbSamples; s++) {
            for (int channel = 0; channel < nbChannels; channel++) {
                int sample = (pcmBuffer.getShort() & 0xFFFF) - 0x8000;
                normalizedOutput[channel][s] = sample / (double) 0x8000;
            }
        }
    }

    private static void unsigned24BitToSample(ByteBuffer pcmBuffer, double[][] normalizedOutput, int nbSamples, int nbChannels) {
        pcmBuffer.rewind();
        for (int s = 0; s < nbSamples; s++) {
            for (int channel = 0; channel < nbChannels; channel++) {
                int sample_h = pcmBuffer.get() & 0xFF;
                int sample_l = pcmBuffer.getShort() & 0xFFFF;
                int sample = ((sample_h << 16 | sample_l) & 0xFFFFFF) - 0x800000;
                normalizedOutput[channel][s] = sample / (double) 0x800000;
            }
        }
    }

    private static void unsigned32BitToSample(ByteBuffer pcmBuffer, double[][] normalizedOutput, int nbSamples, int nbChannels) {
        pcmBuffer.rewind();
        for (int s = 0; s < nbSamples; s++) {
            for (int channel = 0; channel < nbChannels; channel++) {
                int sample = pcmBuffer.getInt() - 0x80000000;
                normalizedOutput[channel][s] = sample / (double) 0x80000000;
            }
        }
    }
}
