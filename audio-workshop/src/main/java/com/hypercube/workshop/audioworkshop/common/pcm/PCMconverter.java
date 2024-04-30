package com.hypercube.workshop.audioworkshop.common.pcm;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class PCMconverter {
    public static void convert(float[][] normalizedInput, byte[] pcmOutput, int nbSamples, int nbChannels, BitDepth bitdepth, boolean bigEndian, boolean signed) {
        ByteBuffer buffer = ByteBuffer.wrap(pcmOutput);
        buffer.order(bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
        if (signed) {
            switch (bitdepth) {
                case BIT_DEPTH_8 -> {
                    for (int s = 0; s < nbSamples; s++) {
                        for (int channel = 0; channel < nbChannels; channel++) {
                            float sample = normalizedInput[channel][s] * (float) 0x80;
                            buffer.put((byte) sample);
                        }
                    }
                }
                case BIT_DEPTH_16 -> {
                    for (int s = 0; s < nbSamples; s++) {
                        for (int channel = 0; channel < nbChannels; channel++) {
                            float sample = normalizedInput[channel][s] * (float) 0x8000;
                            buffer.putShort((short) sample);
                        }
                    }
                }
                case BIT_DEPTH_24 -> {
                    for (int s = 0; s < nbSamples; s++) {
                        for (int channel = 0; channel < nbChannels; channel++) {
                            int sample = (int) (normalizedInput[channel][s] * (float) 0x800000);
                            buffer.putShort((short) (sample >> 8));
                            buffer.put((byte) (sample & 0xff));
                        }
                    }
                }
                case BIT_DEPTH_32 -> {
                    throw new UnsupportedOperationException();
                }
            }
        } else {
            switch (bitdepth) {
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

    public static void convert(byte[] pcmInput, float[][] normalizedOutput, int nbSamples, int nbChannels, BitDepth bitdepth, boolean bigEndian, boolean signed) {
        ByteBuffer buffer = ByteBuffer.wrap(pcmInput);
        buffer.order(bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
        if (signed) {
            switch (bitdepth) {
                case BIT_DEPTH_8 -> {
                    for (int s = 0; s < nbSamples; s++) {
                        for (int channel = 0; channel < nbChannels; channel++) {
                            int sample = buffer.get();
                            normalizedOutput[channel][s] = sample / (float) 0x80;
                        }
                    }
                }
                case BIT_DEPTH_16 -> {
                    for (int s = 0; s < nbSamples; s++) {
                        for (int channel = 0; channel < nbChannels; channel++) {
                            int sample = buffer.getShort();
                            normalizedOutput[channel][s] = sample / (float) 0x8000;
                        }
                    }
                }
                case BIT_DEPTH_24 -> {
                    for (int s = 0; s < nbSamples; s++) {
                        for (int channel = 0; channel < nbChannels; channel++) {
                            int sample_h = buffer.get() & 0xFF;
                            int sample_l = buffer.getShort() & 0xFFFF;
                            int sample = (sample_h << 16 | sample_l);
                            if ((sample & 0x800000) != 0) {
                                sample = sample | 0xFF000000;
                            }
                            normalizedOutput[channel][s] = sample / (float) 0x800000;
                        }
                    }
                }
                case BIT_DEPTH_32 -> {
                    for (int s = 0; s < nbSamples; s++) {
                        for (int channel = 0; channel < nbChannels; channel++) {
                            int sample = buffer.getInt();
                            normalizedOutput[channel][s] = sample / (float) 0x80000000;
                        }
                    }
                }
            }
        } else {
            switch (bitdepth) {
                case BIT_DEPTH_8 -> {
                    for (int s = 0; s < nbSamples; s++) {
                        for (int channel = 0; channel < nbChannels; channel++) {
                            int sample = (buffer.get() & 0xFF) - 0x80;
                            normalizedOutput[channel][s] = sample / (float) 0x80;
                        }
                    }
                }
                case BIT_DEPTH_16 -> {
                    for (int s = 0; s < nbSamples; s++) {
                        for (int channel = 0; channel < nbChannels; channel++) {
                            int sample = (buffer.getShort() & 0xFFFF) - 0x8000;
                            normalizedOutput[channel][s] = sample / (float) 0x8000;
                        }
                    }
                }
                case BIT_DEPTH_24 -> {
                    for (int s = 0; s < nbSamples; s++) {
                        for (int channel = 0; channel < nbChannels; channel++) {
                            int sample_h = buffer.get() & 0xFF;
                            int sample_l = buffer.getShort() & 0xFFFF;
                            int sample = ((sample_h << 16 | sample_l) & 0xFFFFFF) - 0x800000;
                            normalizedOutput[channel][s] = sample / (float) 0x800000;
                        }
                    }
                }
                case BIT_DEPTH_32 -> {
                    for (int s = 0; s < nbSamples; s++) {
                        for (int channel = 0; channel < nbChannels; channel++) {
                            int sample = buffer.getInt() - 0x80000000;
                            normalizedOutput[channel][s] = sample / (float) 0x80000000;
                        }
                    }
                }
            }
        }
    }
}
