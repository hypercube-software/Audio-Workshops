package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io.program.segment;

import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment.KFProgramSegment;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment.KFZoneSegment;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamReader;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamWriter;

public class KFZoneDeserializer implements KFSegmentDeserializer {

    @Override
    public void serialize(KFProgramSegment segment, BitStreamWriter out) {
        KFZoneSegment zone = (KFZoneSegment) segment;
        BitStreamWriter segmentContent = new BitStreamWriter();
        segmentContent.writeByte(zone.getChan());
        segmentContent.writeShort(zone.getProg());
        segmentContent.writeByte(zone.getLokey());
        segmentContent.writeByte(zone.getHikey());
        segmentContent.writeByte(zone.getFlags());
        segmentContent.writeByte(zone.getTrans());
        for (int ctl : zone.getCtls()) {
            segmentContent.writeByte(ctl);
        }
        byte[] result = segmentContent.toByteArray();
        segment.updateContent(result, out.getBytePos());
        out.writeBytes(result);
    }

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
