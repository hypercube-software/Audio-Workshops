package com.hypercube.workshop.audioworkshop.files.riff;

import com.hypercube.workshop.audioworkshop.files.meta.AudioMetadata;
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
    private final AudioMetadata metadata = new AudioMetadata();

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
        String subCodecString = "";
        if (getFileInfo().getSubCodec() != null) {
            if (getFileInfo().getSubCodec()
                    .equals(WaveGUIDCodecs.WMMEDIASUBTYPE_PCM_BE)) {
                subCodecString = "PCM_BE";
            } else if (getFileInfo().getSubCodec()
                    .equals(WaveGUIDCodecs.WMMEDIASUBTYPE_PCM_LE)) {
                subCodecString = "PCM_LE";
            } else {
                subCodecString = getFileInfo().getSubCodec()
                        .toString();
            }
        }
        return getFileInfo().getCodec() + (getFileInfo().getCodec() == WaveCodecs.WAVE_FORMAT_EXTENSIBLE ? " " + subCodecString : "");
    }

    public void addChunk(RiffChunk chunk) {
        chunks.add(chunk);
    }

    public List<RiffChunk> getChunks() {
        return Collections.unmodifiableList(chunks);
    }

    public RiffChunk getDataChunk() {
        RiffChunk data = getChunk("data"); // WAV files
        if (data == null) {
            return getChunk("SSND"); // AIFF files
        } else {
            return data;
        }
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
