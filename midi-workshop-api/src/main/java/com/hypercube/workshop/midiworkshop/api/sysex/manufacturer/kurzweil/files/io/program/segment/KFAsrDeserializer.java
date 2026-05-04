package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io.program.segment;

import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment.KFAsrSegment;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment.KFProgramSegment;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamReader;

public class KFAsrDeserializer implements KFSegmentDeserializer {

    @Override
    public void deserialize(KFProgramSegment segment) {
        KFAsrSegment asrSegment = (KFAsrSegment) segment;
        BitStreamReader in = asrSegment.getSegmentContent()
                .bitStreamReader();
        asrSegment.setRfu1(in.readByte());
        asrSegment.setTrigger(in.readByte());
        asrSegment.setFlags(in.readByte());
        asrSegment.setDtime(in.readByte());
        asrSegment.setAtime(in.readByte());
        asrSegment.setRfu2(in.readByte());
        asrSegment.setRtime(in.readByte());
    }
}
