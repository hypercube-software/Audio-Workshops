package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io.program.segment;

import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment.KFProgramSegment;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamWriter;

public interface KFSegmentDeserializer {
    void serialize(KFProgramSegment segment, BitStreamWriter out);

    void deserialize(KFProgramSegment segment);
}
