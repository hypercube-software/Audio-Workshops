package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io.program.segment;

import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment.KFEfxSegment;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment.KFProgramSegment;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamReader;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamWriter;

public class KFEfxDeserializer implements KFSegmentDeserializer {

    @Override
    public void serialize(KFProgramSegment segment, BitStreamWriter out) {
        KFEfxSegment efxSegment = (KFEfxSegment) segment;
        BitStreamWriter segmentContent = new BitStreamWriter();
        segmentContent.writeByte(efxSegment.getChan());
        segmentContent.writeByte(efxSegment.getProg());
        segmentContent.writeByte(efxSegment.getMix());
        segmentContent.writeByte(efxSegment.getCtl1());
        segmentContent.writeByte(efxSegment.getOut1());
        segmentContent.writeByte(efxSegment.getCtl2());
        segmentContent.writeByte(efxSegment.getOut2());
        byte[] result = segmentContent.toByteArray();
        segment.updateContent(result, out.getBytePos());
        out.writeBytes(result);
    }

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
