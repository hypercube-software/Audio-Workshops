package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io.program.segment;

import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment.KFHobbesSegment;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment.KFProgramSegment;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamReader;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KFHobbesDeserializer implements KFSegmentDeserializer {

    @Override
    public void deserialize(KFProgramSegment segment) {
        KFHobbesSegment hobbesSegment = (KFHobbesSegment) segment;
        BitStreamReader in = hobbesSegment.getSegmentContent()
                .bitStreamReader();
        hobbesSegment.setSubTag(in.readByte());
        hobbesSegment.setCoarse(in.readByte());
        hobbesSegment.setFine(in.readByte());
        hobbesSegment.setKScale(in.readByte());
        hobbesSegment.setVScale(in.readByte());
        hobbesSegment.setControl(in.readByte());
        hobbesSegment.setRange(in.readByte());
        hobbesSegment.setDepth(in.readByte());
        hobbesSegment.setMinDepth(in.readByte());
        hobbesSegment.setMaxDepth(in.readByte());
        hobbesSegment.setSource(in.readByte());
        hobbesSegment.setBitfields1(in.readByte());
        hobbesSegment.setMoreTscr(in.readByte());
        hobbesSegment.setBitfields2(in.readByte());
        hobbesSegment.setBitfields3(in.readByte());
    }
}
