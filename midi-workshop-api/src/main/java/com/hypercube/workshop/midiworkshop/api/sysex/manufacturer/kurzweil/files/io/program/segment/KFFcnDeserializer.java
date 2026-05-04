package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io.program.segment;

import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment.KFFcnSegment;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment.KFProgramSegment;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamReader;

public class KFFcnDeserializer implements KFSegmentDeserializer {

    @Override
    public void deserialize(KFProgramSegment segment) {
        KFFcnSegment fcnSegment = (KFFcnSegment) segment;
        BitStreamReader in = fcnSegment.getSegmentContent()
                .bitStreamReader();
        fcnSegment.setOp(in.readByte());
        fcnSegment.setArg1(in.readByte());
        fcnSegment.setArg2(in.readByte());
    }
}
