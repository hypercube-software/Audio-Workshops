package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io.program.segment;

import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment.KFFXPartSegment;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment.KFProgramSegment;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamReader;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamWriter;

public class KFFXPartDeserializer implements KFSegmentDeserializer {

    @Override
    public void serialize(KFProgramSegment segment, BitStreamWriter out) {
        KFFXPartSegment rootTableSegment = (KFFXPartSegment) segment;
        BitStreamWriter segmentContent = new BitStreamWriter();
        segmentContent.writeByte(rootTableSegment.getNum());
        segmentContent.writeByte(rootTableSegment.getStrip());
        segmentContent.writeByte(rootTableSegment.getParam());
        segmentContent.writeByte(rootTableSegment.getAdjust());
        segmentContent.writeByte(rootTableSegment.getSource());
        segmentContent.writeByte(rootTableSegment.getDepth());
        segmentContent.writeByte(rootTableSegment.getRfu());
        byte[] result = segmentContent.toByteArray();
        segment.updateContent(result, out.getBytePos());
        out.writeBytes(result);
    }

    @Override
    public void deserialize(KFProgramSegment segment) {
        KFFXPartSegment rootTableSegment = (KFFXPartSegment) segment;
        BitStreamReader in = rootTableSegment.getSegmentContent()
                .bitStreamReader();
        rootTableSegment.setNum(in.readByte());
        rootTableSegment.setStrip(in.readByte());
        rootTableSegment.setParam(in.readByte());
        rootTableSegment.setAdjust(in.readByte());
        rootTableSegment.setSource(in.readByte());
        rootTableSegment.setDepth(in.readByte());
        rootTableSegment.setRfu(in.readByte());
    }
}
