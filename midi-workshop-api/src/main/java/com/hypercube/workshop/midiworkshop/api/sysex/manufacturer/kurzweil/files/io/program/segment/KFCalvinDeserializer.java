package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io.program.segment;

import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment.KFCalvinSegment;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment.KFProgramSegment;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamReader;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KFCalvinDeserializer implements KFSegmentDeserializer {

    @Override
    public void deserialize(KFProgramSegment segment) {
        KFCalvinSegment calvinSegment = (KFCalvinSegment) segment;
        BitStreamReader in = calvinSegment.getSegmentContent()
                .bitStreamReader();
        calvinSegment.setSubTag(in.readByte());
        calvinSegment.setTrans(in.readByte());
        calvinSegment.setDtune(in.readByte());
        calvinSegment.setTkScale(in.readByte());
        calvinSegment.setTvScale(in.readByte());
        calvinSegment.setTcontrol(in.readByte());
        calvinSegment.setTrange(in.readByte());
        calvinSegment.setSkeymap(in.readShort());
        calvinSegment.setSroot(in.readByte());
        calvinSegment.setSlegato(in.readByte());
        calvinSegment.setKeymap(in.readShort());
        calvinSegment.setRoot(in.readByte());
        calvinSegment.setLegato(in.readByte());
        calvinSegment.setTshift(in.readByte());
        calvinSegment.setRfu2(in.readByte());
        calvinSegment.setCpitch(in.readByte());
        calvinSegment.setFpitch(in.readByte());
        calvinSegment.setCkScale(in.readByte());
        calvinSegment.setCvScale(in.readByte());
        calvinSegment.setPcontrol(in.readByte());
        calvinSegment.setPrange(in.readByte());
        calvinSegment.setPdepth(in.readByte());
        calvinSegment.setPmin(in.readByte());
        calvinSegment.setPmax(in.readByte());
        calvinSegment.setPsource(in.readByte());
        calvinSegment.setCcr(in.readShort());
        calvinSegment.setBitfields(in.readByte());
        calvinSegment.setAlg(in.readByte());
        calvinSegment.setFineHz(in.readByte());
        log.info("Using keymap {} and skeyMap {}", calvinSegment.getKeymap(), calvinSegment.getSkeymap());
    }
}
