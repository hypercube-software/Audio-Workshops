package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io.program.segment;

import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment.KFLfoSegment;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment.KFProgramSegment;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamReader;

public class KFLfoDeserializer implements KFSegmentDeserializer {

    @Override
    public void deserialize(KFProgramSegment segment) {
        KFLfoSegment lfoSegment = (KFLfoSegment) segment;
        BitStreamReader in = lfoSegment.getSegmentContent()
                .bitStreamReader();
        lfoSegment.setRfu1(in.readByte());
        lfoSegment.setRateCtl(in.readByte());
        lfoSegment.setMinRate(in.readByte());
        lfoSegment.setMaxRate(in.readByte());
        lfoSegment.setPhase(in.readByte());
        lfoSegment.setShape(in.readByte());
        lfoSegment.setRfu2(in.readByte());
    }
}
