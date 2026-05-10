package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.RawData;
import lombok.Getter;

import java.util.HexFormat;

@Getter
@JsonPropertyOrder({"id", "segmentContent"})
public class KFProgramSegment {
    private final RawData segmentContent;
    private final ProgramSegmentIdentifier id;

    public KFProgramSegment(RawData segmentContent,
                            ProgramSegmentIdentifier id) {
        this.segmentContent = segmentContent;
        this.id = id;
    }

    public void updateContent(byte[] data, int position) {
        ProgramSegmentType type = getId()
                .type();
        int expectedSize = type.getSize() - 1; // content does not include the first tag byte
        if (data.length != expectedSize) {
            throw new IllegalStateException("Unexpected serialized size for %s. Expected %s Actual %s".formatted(type, expectedSize, data.length));
        }
        segmentContent.setContent(data);
        segmentContent.setPosition(position);
    }

    @JsonIgnore
    public int getTag() {
        return id.type()
                .getTag();
    }

    @JsonIgnore
    public int getRawTag() {
        return id.rawValue();
    }

    @JsonIgnore
    public ProgramSegmentType getType() {
        return id.type();
    }

    @JsonIgnore
    public int getInstanceId() {
        return id.instanceId();
    }

    @JsonIgnore
    public long getPosition() {
        return segmentContent.getPosition();
    }

    @JsonIgnore
    public String getContent() {
        return HexFormat.of()
                .withUpperCase()
                .withDelimiter(" ")
                .formatHex(segmentContent.getContent());
    }
}
