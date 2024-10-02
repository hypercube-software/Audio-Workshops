package com.hypercube.workshop.audioworkshop.files.riff.chunks;

import com.hypercube.workshop.audioworkshop.common.pcm.BitDepth;
import com.hypercube.workshop.audioworkshop.files.riff.WaveChannels;
import com.hypercube.workshop.audioworkshop.files.riff.WaveCodecs;
import com.hypercube.workshop.audioworkshop.files.riff.WaveGUIDCodecs;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Setter
@Getter
public class RiffFmtChunk extends RiffChunk {
    public static final int CONTENT_SIZE = 40;
    public static final int WAVEFORMATEX_SIZE = 18;
    private int formatTag;
    private int nChannels;
    private int nSamplesPerSec;
    private int nAvgBytesPerSec;
    private int nBlockAlign;
    private int bitsPerSample;
    private int channelMask;
    private UUID codec;

    public RiffFmtChunk(RiffChunk parent, String id, int contentStart, int contentSize) {
        super(parent, id, contentStart, contentSize);
    }

    /**
     * @param nChannels      Number of channels
     * @param nSamplesPerSec Sample rate
     * @param bitDepth       Bit per sample
     * @param codec          See {@link WaveGUIDCodecs}
     */
    public RiffFmtChunk(int nChannels, int nSamplesPerSec, BitDepth bitDepth, UUID codec) {
        super(null, Chunks.FORMAT, 0, CONTENT_SIZE);
        this.bitsPerSample = bitDepth.getBits();
        this.formatTag = WaveCodecs.WAVE_FORMAT_EXTENSIBLE.getValue();
        this.nChannels = nChannels;
        this.nSamplesPerSec = nSamplesPerSec;
        this.nBlockAlign = (nChannels * bitsPerSample) / 8;
        this.nAvgBytesPerSec = nSamplesPerSec * nBlockAlign;
        this.channelMask = WaveChannels.toMask(nChannels);
        this.codec = codec;
    }
}
