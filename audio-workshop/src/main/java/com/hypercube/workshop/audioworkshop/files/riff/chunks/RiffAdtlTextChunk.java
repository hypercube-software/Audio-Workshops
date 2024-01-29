package com.hypercube.workshop.audioworkshop.files.riff.chunks;

import lombok.Getter;

@Getter
public class RiffAdtlTextChunk extends RiffAdtlLabelChunk {
    private final int sampleLength;
    private final String purposeId;
    private final int countryId;
    private final int language;
    private final int dialect;
    private final int codePage;

    public RiffAdtlTextChunk(RiffChunk parent, String id, int contentStart, int contentSize, int cuePointId, String value, int sampleLength, String purposeId, int countryId, int language, int dialect, int codePage) {
        super(parent, id, contentStart, contentSize, cuePointId, value);
        this.sampleLength = sampleLength;
        this.purposeId = purposeId;
        this.countryId = countryId;
        this.language = language;
        this.dialect = dialect;
        this.codePage = codePage;
    }

}
