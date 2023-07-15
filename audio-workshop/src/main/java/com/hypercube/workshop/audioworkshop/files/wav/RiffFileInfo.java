package com.hypercube.workshop.audioworkshop.files.wav;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
public class RiffFileInfo {
    private String filename;
    private byte[] prolog;
    private byte[] epilog;

    private final List<RiffChunk> chunks = new ArrayList<>();
    private final RiffAudioInfo fileInfo = new RiffAudioInfo();
    private final RiffMetadata metadata = new RiffMetadata();

    public void setFilename(String filename) {
        this.filename = filename;
    }
    
    public void setProlog(byte[] prolog) {
        this.prolog = prolog;
    }


    public void setEpilog(byte[] epilog) {
        this.epilog = epilog;
    }

    public boolean isPCM() {
        return getFileInfo().getCodec() == WaveCodecs.PCM ||
                (getFileInfo().getCodec() == WaveCodecs.WAVE_FORMAT_EXTENSIBLE
                        && (getFileInfo().getSubCodec()
                        .equals(WaveGUIDCodecs.WMMEDIASUBTYPE_PCM_BE)
                        || getFileInfo().getSubCodec()
                        .equals(WaveGUIDCodecs.WMMEDIASUBTYPE_PCM_LE)
                )
                );
    }

    public boolean isIEEE754FLOAT() {
        return getFileInfo().getCodec() == WaveCodecs.IEEE754_FLOAT ||
                (getFileInfo().getCodec() == WaveCodecs.WAVE_FORMAT_EXTENSIBLE
                        && getFileInfo().getSubCodec()
                        .equals(WaveGUIDCodecs.WMMEDIASUBTYPE_IEEE754_FLOAT)
                );
    }

    public String getCodecString() {
        return getFileInfo().getCodec() + (getFileInfo().getCodec() == WaveCodecs.WAVE_FORMAT_EXTENSIBLE ? " " + getFileInfo().getSubCodec()
                .toString() : "");
    }

    public void addChunk(RiffChunk chunk) {
        chunks.add(chunk);
    }

    public List<RiffChunk> getChunks() {
        return Collections.unmodifiableList(chunks);
    }

    public RiffChunk getDataChunk() {
        return getChunk("data");
    }

    public List<RiffChunk> getListChunks() {
        return getChunks("LIST");
    }

    public RiffChunk getChunk(String id) {
        return chunks.stream()
                .filter(c -> c.getId()
                        .equals(id))
                .findFirst()
                .orElse(null);
    }

    public List<RiffChunk> getChunks(String id) {
        return chunks.stream()
                .filter(c -> c.getId()
                        .equals(id))
                .toList();
    }

}
