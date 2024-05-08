package com.hypercube.workshop.audioworkshop.files.riff;

import lombok.Getter;

import java.util.Arrays;

/**
 * <a href="https://videolan.videolan.me/vlc/vlc__codecs_8h_source.html">vlc__codecs_8h_source.html</a>
 */
@Getter
public enum WaveChannels {
    //@formatter:off
    SPEAKER_FRONT_LEFT(0x1,"Front Left"),
    SPEAKER_FRONT_RIGHT(0x2,"Front Right"),
    SPEAKER_FRONT_CENTER(0x4,"Center"),
    SPEAKER_LOW_FREQUENCY(0x8,"Sub woofer"),
    SPEAKER_BACK_LEFT(0x10,"Rear Lef"),
    SPEAKER_BACK_RIGHT(0x20,"Rear Right"),
    SPEAKER_FRONT_LEFT_OF_CENTER(0x40,"Front left of Center"),
    SPEAKER_FRONT_RIGHT_OF_CENTER(0x80,"Front right of Center "),
    SPEAKER_BACK_CENTER(0x100,"Rear Center"),
    SPEAKER_SIDE_LEFT(0x200,"Side Left"),
    SPEAKER_SIDE_RIGHT(0x400,"Side Right"),
    SPEAKER_TOP_CENTER(0x800,"Top Center"),
    SPEAKER_TOP_FRONT_LEFT(0x1000,"Top Front Left"),
    SPEAKER_TOP_FRONT_CENTER(0x2000,"Top Front Center"),
    SPEAKER_TOP_FRONT_RIGHT(0x4000,"To Dront Right"),
    SPEAKER_TOP_BACK_LEFT(0x8000,"To Rear Left"),
    SPEAKER_TOP_BACK_CENTER(0x10000,"Rear Center"),
    SPEAKER_TOP_BACK_RIGHT(0x20000,"Rear Right"),
    SPEAKER_RESERVED(0x80000000,"Reserved"),
    UNKNOWN(0,"Unknown");
    //@formatter:on
    private int value;
    private String shortTitle;

    WaveChannels(int value, String shortTitle) {
        this.value = value;
        this.shortTitle = shortTitle;
    }

    public static WaveChannels valueOfOrdinal(int channel) {
        return Arrays.stream(WaveChannels.values())
                .filter(e -> e.ordinal() == channel)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid ordinal value %d for enum WaveChannels".formatted(channel)));
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
        for (var channel : WaveChannels.values()) {
            if ((dwChannelMask & channel.getValue()) != 0) {
                if (!channels.isEmpty())
                    channels.append(",");
                channels.append(channel.name());
            }
        }
        return channels.toString();
    }

    public static int toMask(int nChannels) {
        if (nChannels == 1) {
            return WaveChannels.SPEAKER_FRONT_CENTER.value;
        } else if (nChannels == 2) {
            return WaveChannels.SPEAKER_FRONT_LEFT.value | WaveChannels.SPEAKER_FRONT_RIGHT.value;
        } else if (nChannels == 3) {
            return WaveChannels.SPEAKER_FRONT_CENTER.value | WaveChannels.SPEAKER_FRONT_LEFT.value | WaveChannels.SPEAKER_BACK_RIGHT.value;
        } else if (nChannels == 4) {
            return WaveChannels.SPEAKER_FRONT_LEFT.value | WaveChannels.SPEAKER_FRONT_RIGHT.value | WaveChannels.SPEAKER_BACK_LEFT.value | WaveChannels.SPEAKER_BACK_RIGHT.value;
        } else if (nChannels == 5) {
            return WaveChannels.SPEAKER_FRONT_CENTER.value | WaveChannels.SPEAKER_FRONT_LEFT.value | WaveChannels.SPEAKER_FRONT_RIGHT.value | WaveChannels.SPEAKER_BACK_LEFT.value | WaveChannels.SPEAKER_BACK_RIGHT.value;
        } else if (nChannels == 6) {
            return WaveChannels.SPEAKER_FRONT_CENTER.value | WaveChannels.SPEAKER_BACK_CENTER.value | WaveChannels.SPEAKER_FRONT_LEFT.value | WaveChannels.SPEAKER_FRONT_RIGHT.value | WaveChannels.SPEAKER_BACK_LEFT.value | WaveChannels.SPEAKER_BACK_RIGHT.value;
        } else {
            return 0;
        }
    }
}
