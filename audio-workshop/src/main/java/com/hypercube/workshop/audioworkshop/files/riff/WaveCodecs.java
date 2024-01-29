package com.hypercube.workshop.audioworkshop.files.riff;

import lombok.Getter;

import java.util.Arrays;

/**
 * <a href="https://videolan.videolan.me/vlc/vlc__codecs_8h_source.html">vlc__codecs_8h_source.html</a>
 */
@Getter
public enum WaveCodecs {
    UNKNOWN(0x00), PCM(0x01), WAVE_FORMAT_EXTENSIBLE(0xFFFE), MICROSOFT_ADPCM(0x02), IEEE754_FLOAT(0x03), ITU_G711_ALAW(0x06), ITU_G711_ULAW(0x07), IMA_ADPCM(0x11),
    ITU_G723_ADPCM_YAMAHA(0x16), GSM_610(0x31), ITU_G721_ADPCM(0x40), ITU_G721_MPEG(0x50);

    private int value;

    WaveCodecs(int value) {
        this.value = value;
    }

    public static WaveCodecs valueOf(int wFormatTag) {
        return Arrays.stream(WaveCodecs.values())
                .filter(e -> e.getValue() == wFormatTag)
                .findFirst()
                .orElseGet(() -> {
                    WaveCodecs c = UNKNOWN;
                    c.value = wFormatTag;
                    return c;
                });
    }

}
