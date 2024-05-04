package com.hypercube.workshop.synthripper.config;

import com.hypercube.workshop.audioworkshop.common.utils.CachedRegExp;
import lombok.Getter;
import lombok.Setter;

import javax.sound.sampled.AudioFormat;
import java.util.regex.Matcher;

@Getter
@Setter
public class AudioSettings {
    private int nbChannels;
    private int bitDepth;
    private int sampleRate;
    private String channelMapping;
    private ChannelMap channelMap = new ChannelMap();

    public AudioFormat getAudioFormat() {
        return new AudioFormat(sampleRate, bitDepth, nbChannels, true, false);
    }

    public AudioFormat getWavFormat() {
        Matcher matcher = CachedRegExp.get("([0-9]+):([0-9]+)", channelMapping);
        while (matcher.find()) {
            int srcChannel = Integer.parseInt(matcher.group(1));
            int dstChannel = Integer.parseInt(matcher.group(2));
            ChannelMapping channelMapping = new ChannelMapping(srcChannel, dstChannel);
            channelMap.put(srcChannel, channelMapping);
        }
        return new AudioFormat(sampleRate, bitDepth, channelMap.size(), true, false);
    }
}
