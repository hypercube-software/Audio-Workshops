package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io.program.segment;

import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment.KFEfxSegment;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment.KFProgramSegment;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamReader;

public class KFEfxDeserializer implements KFSegmentDeserializer {

    @Override
    public void deserialize(KFProgramSegment segment) {
        KFEfxSegment efxSegment = (KFEfxSegment) segment;
        BitStreamReader in = efxSegment.getSegmentContent()
                .bitStreamReader();
        efxSegment.setChan(in.readByte());
        efxSegment.setProg(in.readByte());
        efxSegment.setMix(in.readByte());
        efxSegment.setCtl1(in.readByte());
        efxSegment.setOut1(in.readByte());
        efxSegment.setCtl2(in.readByte());
        efxSegment.setOut2(in.readByte());
    }
}
