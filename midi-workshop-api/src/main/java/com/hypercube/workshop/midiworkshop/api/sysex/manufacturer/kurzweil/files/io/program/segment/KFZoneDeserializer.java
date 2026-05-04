package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io.program.segment;

import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment.KFProgramSegment;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment.KFZoneSegment;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamReader;

public class KFZoneDeserializer implements KFSegmentDeserializer {

    @Override
    public void deserialize(KFProgramSegment segment) {
        KFZoneSegment zoneSegment = (KFZoneSegment) segment;
        BitStreamReader in = zoneSegment.getSegmentContent()
                .bitStreamReader();
        zoneSegment.setChan(in.readByte());
        zoneSegment.setProg(in.readShort());
        zoneSegment.setLokey(in.readByte());
        zoneSegment.setHikey(in.readByte());
        zoneSegment.setFlags(in.readByte());
        zoneSegment.setTrans(in.readByte());
        int[] ctls = new int[8];
        for (int i = 0; i < 8; i++) ctls[i] = in.readByte();
        zoneSegment.setCtls(ctls);
    }
}
