package com.hypercube.workshop.audioworkshop.files.wav;

import java.util.Arrays;

/**
 * https://videolan.videolan.me/vlc/vlc__codecs_8h_source.html
 */
public enum WaveChannels {
    SPEAKER_FRONT_LEFT(0x1), SPEAKER_FRONT_RIGHT(0x2), SPEAKER_FRONT_CENTER(0x4), SPEAKER_LOW_FREQUENCY(0x8), SPEAKER_BACK_LEFT(0x10), SPEAKER_BACK_RIGHT(0x20), SPEAKER_FRONT_LEFT_OF_CENTER(0x40), SPEAKER_FRONT_RIGHT_OF_CENTER(0x80), SPEAKER_BACK_CENTER(0x100), SPEAKER_SIDE_LEFT(0x200), SPEAKER_SIDE_RIGHT(0x400), SPEAKER_TOP_CENTER(0x800), SPEAKER_TOP_FRONT_LEFT(0x1000), SPEAKER_TOP_FRONT_CENTER(0x2000), SPEAKER_TOP_FRONT_RIGHT(0x4000), SPEAKER_TOP_BACK_LEFT(0x8000), SPEAKER_TOP_BACK_CENTER(0x10000), SPEAKER_TOP_BACK_RIGHT(0x20000), SPEAKER_RESERVED(0x80000000), UNKNOWN(0);
    private int value;

    WaveChannels(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static WaveChannels valueOf(int wChannel) {
        return Arrays.stream(WaveChannels.values())
                .filter(e -> e.getValue() == wChannel)
                .findFirst()
                .orElseGet(() -> {
                    WaveChannels c = UNKNOWN;
                    c.value = wChannel;
                    return c;
                });
    }

    public static String getMask(int dwChannelMask) {
        StringBuilder channels = new StringBuilder();
        for (var channel : Arrays.asList(WaveChannels.values())) {
            if ((dwChannelMask & channel.getValue()) != 0) {
                if (channels.length() > 0)
                    channels.append(",");
                channels.append(channel.name());
            }
        }
        return channels.toString();
    }

}
