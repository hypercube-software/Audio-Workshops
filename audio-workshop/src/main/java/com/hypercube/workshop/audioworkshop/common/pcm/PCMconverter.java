package com.hypercube.workshop.audioworkshop.common.pcm;

import com.hypercube.workshop.audioworkshop.common.line.AudioLineFormat;

import java.nio.ByteBuffer;

public class PCMconverter {
    public static PCMtoSampleFunction getPCMtoSampleFunction(AudioLineFormat format) {
        switch (format.getEncoding()) {
            case SIGNED -> {
                switch (format.getBitDepth()) {
                    case BIT_DEPTH_8 -> {
                        return PCMconverter::signed8BitToSample;
                    }
                    case BIT_DEPTH_16 -> {
                        return PCMconverter::signed16BitToSample;
                    }
                    case BIT_DEPTH_24 -> {
                        return PCMconverter::signed24BitToSample;
                    }
                    case BIT_DEPTH_32 -> {
                        return PCMconverter::signed32BitToSample;
                    }
                }
            }
            case UNSIGNED -> {
                switch (format.getBitDepth()) {
                    case BIT_DEPTH_8 -> {
                        return PCMconverter::unsigned8BitToSample;
                    }
                    case BIT_DEPTH_16 -> {
                        return PCMconverter::unsigned16BitToSample;
                    }
                    case BIT_DEPTH_24 -> {
                        return PCMconverter::unsigned24BitToSample;
                    }
                    case BIT_DEPTH_32 -> {
                        return PCMconverter::unsigned32BitToSample;
                    }
                }
            }
        }
        throw new UnsupportedOperationException();
    }

    public static SampleToPCMFunction getSampleToPCMFunction(AudioLineFormat format) {
        switch (format.getEncoding()) {
            case SIGNED -> {
                switch (format.getBitDepth()) {
                    case BIT_DEPTH_8 -> {
                        return PCMconverter::sampleToSigned8Bits;
                    }
                    case BIT_DEPTH_16 -> {
                        return PCMconverter::sampleToSigned16Bits;
                    }
                    case BIT_DEPTH_24 -> {
                        return PCMconverter::sampleToSigned24Bits;
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

    public static void sampleToSigned8Bits(float[][] normalizedInput, ByteBuffer pcmBuffer, int nbSamples, int nbChannels) {
        pcmBuffer.rewind();
        for (int s = 0; s < nbSamples; s++) {
            for (int channel = 0; channel < nbChannels; channel++) {
                float sample = normalizedInput[channel][s] * (float) 0x80;
                pcmBuffer.put((byte) sample);
            }
        }
    }

    public static void sampleToSigned16Bits(float[][] normalizedInput, ByteBuffer pcmBuffer, int nbSamples, int nbChannels) {
        pcmBuffer.rewind();
        for (int s = 0; s < nbSamples; s++) {
            for (int channel = 0; channel < nbChannels; channel++) {
                float sample = normalizedInput[channel][s] * (float) 0x8000;
                pcmBuffer.putShort((short) sample);
            }
        }
    }

    public static void sampleToSigned24Bits(float[][] normalizedInput, ByteBuffer pcmBuffer, int nbSamples, int nbChannels) {
        pcmBuffer.rewind();
        for (int s = 0; s < nbSamples; s++) {
            for (int channel = 0; channel < nbChannels; channel++) {
                int sample = (int) (normalizedInput[channel][s] * (float) 0x800000);
                pcmBuffer.putShort((short) (sample >> 8));
                pcmBuffer.put((byte) (sample & 0xff));
            }
        }
    }

    private static void signed8BitToSample(ByteBuffer pcmBuffer, float[][] normalizedOutput, int nbSamples, int nbChannels) {
        pcmBuffer.rewind();
        for (int s = 0; s < nbSamples; s++) {
            for (int channel = 0; channel < nbChannels; channel++) {
                int sample = pcmBuffer.get();
                normalizedOutput[channel][s] = sample / (float) 0x80;
            }
        }
    }

    private static void signed16BitToSample(ByteBuffer pcmBuffer, float[][] normalizedOutput, int nbSamples, int nbChannels) {
        pcmBuffer.rewind();
        for (int s = 0; s < nbSamples; s++) {
            for (int channel = 0; channel < nbChannels; channel++) {
                int sample = pcmBuffer.getShort();
                normalizedOutput[channel][s] = sample / (float) 0x8000;
            }
        }
    }

    private static void signed24BitToSample(ByteBuffer pcmBuffer, float[][] normalizedOutput, int nbSamples, int nbChannels) {
        pcmBuffer.rewind();
        for (int s = 0; s < nbSamples; s++) {
            for (int channel = 0; channel < nbChannels; channel++) {
                int sample_h = pcmBuffer.get() & 0xFF;
                int sample_l = pcmBuffer.getShort() & 0xFFFF;
                int sample = (sample_h << 16 | sample_l);
                if ((sample & 0x800000) != 0) {
                    sample = sample | 0xFF000000;
                }
                normalizedOutput[channel][s] = sample / (float) 0x800000;
            }
        }
    }

    private static void signed32BitToSample(ByteBuffer pcmBuffer, float[][] normalizedOutput, int nbSamples, int nbChannels) {
        pcmBuffer.rewind();
        for (int s = 0; s < nbSamples; s++) {
            for (int channel = 0; channel < nbChannels; channel++) {
                int sample = pcmBuffer.getInt();
                normalizedOutput[channel][s] = sample / (float) 0x80000000;
            }
        }
    }

    private static void unsigned8BitToSample(ByteBuffer pcmBuffer, float[][] normalizedOutput, int nbSamples, int nbChannels) {
        pcmBuffer.rewind();
        for (int s = 0; s < nbSamples; s++) {
            for (int channel = 0; channel < nbChannels; channel++) {
                int sample = (pcmBuffer.get() & 0xFF) - 0x80;
                normalizedOutput[channel][s] = sample / (float) 0x80;
            }
        }
    }

    private static void unsigned16BitToSample(ByteBuffer pcmBuffer, float[][] normalizedOutput, int nbSamples, int nbChannels) {
        pcmBuffer.rewind();
        for (int s = 0; s < nbSamples; s++) {
            for (int channel = 0; channel < nbChannels; channel++) {
                int sample = (pcmBuffer.getShort() & 0xFFFF) - 0x8000;
                normalizedOutput[channel][s] = sample / (float) 0x8000;
            }
        }
    }

    private static void unsigned24BitToSample(ByteBuffer pcmBuffer, float[][] normalizedOutput, int nbSamples, int nbChannels) {
        pcmBuffer.rewind();
        for (int s = 0; s < nbSamples; s++) {
            for (int channel = 0; channel < nbChannels; channel++) {
                int sample_h = pcmBuffer.get() & 0xFF;
                int sample_l = pcmBuffer.getShort() & 0xFFFF;
                int sample = ((sample_h << 16 | sample_l) & 0xFFFFFF) - 0x800000;
                normalizedOutput[channel][s] = sample / (float) 0x800000;
            }
        }
    }

    private static void unsigned32BitToSample(ByteBuffer pcmBuffer, float[][] normalizedOutput, int nbSamples, int nbChannels) {
        pcmBuffer.rewind();
        for (int s = 0; s < nbSamples; s++) {
            for (int channel = 0; channel < nbChannels; channel++) {
                int sample = pcmBuffer.getInt() - 0x80000000;
                normalizedOutput[channel][s] = sample / (float) 0x80000000;
            }
        }
    }
}
