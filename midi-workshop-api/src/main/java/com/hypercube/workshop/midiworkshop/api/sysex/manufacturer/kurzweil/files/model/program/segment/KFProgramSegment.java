package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment;

import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.RawData;
import lombok.Getter;

@Getter
public class KFProgramSegment {
    private final RawData segmentContent;
    private final ProgramSegmentIdentifier id;

    public KFProgramSegment(RawData segmentContent, ProgramSegmentIdentifier id) {
        this.segmentContent = segmentContent;
        this.id = id;
    }

    public int getRawTag() {
        return id.rawValue();
    }

    public ProgramSegmentType getType() {
        return id.type();
    }

    public int getInstanceId() {
        return id.instanceId();
    }

    public long getPosition() {
        return segmentContent.position();
    }
}
