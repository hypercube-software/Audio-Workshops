package com.hypercube.workshop.synthripper.config;

import com.hypercube.workshop.audioworkshop.common.format.PCMBufferFormat;
import com.hypercube.workshop.audioworkshop.common.pcm.BitDepth;
import com.hypercube.workshop.audioworkshop.common.pcm.PCMEncoding;
import com.hypercube.workshop.audioworkshop.common.utils.CachedRegExp;
import lombok.Getter;
import lombok.Setter;

import java.nio.ByteOrder;
import java.util.regex.Matcher;

@Getter
@Setter
public class AudioSettings {
    private int nbChannels;
    private int bitDepth;
    private int sampleRate;
    private String channelMapping;
    private int bufferDurationMs;
    private ChannelMap channelMap = new ChannelMap();

    public PCMBufferFormat getAudioFormat() {
        return new PCMBufferFormat(bufferDurationMs, sampleRate, BitDepth.valueOf(bitDepth), nbChannels, PCMEncoding.SIGNED, ByteOrder.LITTLE_ENDIAN);
    }

    public PCMBufferFormat getWavFormat() {
        Matcher matcher = CachedRegExp.get("([0-9]+):([0-9]+)", channelMapping);
        while (matcher.find()) {
            int srcChannel = Integer.parseInt(matcher.group(1));
            int dstChannel = Integer.parseInt(matcher.group(2));
            channelMap.put(srcChannel, new ChannelMapping(srcChannel, dstChannel));
        }
        return new PCMBufferFormat(bufferDurationMs, sampleRate, BitDepth.valueOf(bitDepth), channelMap.size(), PCMEncoding.SIGNED, ByteOrder.LITTLE_ENDIAN);
    }
}
