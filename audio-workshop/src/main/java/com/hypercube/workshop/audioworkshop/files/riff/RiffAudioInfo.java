package com.hypercube.workshop.audioworkshop.files.riff;

import com.hypercube.workshop.audioworkshop.files.riff.chunks.RiffChunk;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class RiffAudioInfo {
    /**
     * Originating filename
     */
    private String filename;
    /**
     * this does not include all the channels, here sample = single channel sample
     */
    private int bitPerSample;
    /**
     * this includes all the channels, here sample = multichannel sample
     */
    private int bytePerSample;
    /**
     * number of multichannel samples in the file
     */
    private int nbSamples;
    /**
     * number of audio bytes
     */
    private int nbAudioBytes;
    /**
     * number of multichannel samples per seconds
     */
    private int sampleRate;
    /**
     * Number of audio channels
     */
    private int nbChannels;
    /**
     * duration in seconds
     */
    private float duration;

    /**
     * duration in HH:MM::SS
     */
    private String durationString;

    /**
     * BPM beats per seconds
     */
    private float tempo;
    /**
     * Codec informations
     */
    private WaveCodecs codec;

    /**
     * Sub-code when codec is WAVE_FORMAT_EXTENSIBLE
     */
    private UUID subCodec;

    /**
     * Which channels are present in the file
     */
    private int channelsMask;

    /**
     * Number of beats in this file (if it is a loop)
     */
    private int nbBeats;
    /**
     * root note in MIDI scale (0-127), 60 = C3
     */
    private int rootNote;
    /**
     * lowcase for minor, uppercase for major example: c# = c sharp minor key
     */

    private String key;
    /**
     * Time signature denominator
     */
    private int meterDenominator;
    /**
     * Time signature numerator
     */
    private int meterNumerator;

    /**
     * Originating 'fmt ' Chunk
     */
    private RiffChunk fmtChunk;

    /**
     * Originating 'data' Chunk
     */
    private RiffChunk dataChunk;

    /**
     * True if the sample is used in a sampler region
     */
    private boolean used;

    public void computeDuration() {
        nbSamples = nbAudioBytes / bytePerSample;
        duration = nbSamples / (float) sampleRate;

        long ms = (long) ((duration - Math.floor(duration)) * 1000);
        long seconds = (long) duration;
        long hh = seconds / 3600;
        long mm = (seconds % 3600) / 60;
        long ss = seconds % 60;
        durationString = String.format("%02d:%02d:%02d.%d", hh, mm, ss, ms);
    }

    public boolean missingChannelsAssignement() {
        return nbChannels > 2 &&
                (codec != WaveCodecs.WAVE_FORMAT_EXTENSIBLE
                        || channelsMask == 0);
    }

    public String getCodecString() {
        String subCodecString = "";
        if (getSubCodec() != null) {
            subCodecString = getSubCodec()
                    .toString();
        }
        return getCodec() + (getCodec() == WaveCodecs.WAVE_FORMAT_EXTENSIBLE ? " " + subCodecString : "");
    }
}
