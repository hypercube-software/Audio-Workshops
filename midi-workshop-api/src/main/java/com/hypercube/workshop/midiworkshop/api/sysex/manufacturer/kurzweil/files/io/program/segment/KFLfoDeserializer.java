package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io.program.segment;

import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment.KFLfoSegment;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment.KFProgramSegment;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamReader;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamWriter;

public class KFLfoDeserializer implements KFSegmentDeserializer {

    @Override
    public void serialize(KFProgramSegment segment, BitStreamWriter out) {
        KFLfoSegment lfoSegment = (KFLfoSegment) segment;
        BitStreamWriter segmentContent = new BitStreamWriter();
        segmentContent.writeByte(lfoSegment.getRfu1());
        segmentContent.writeByte(lfoSegment.getRateCtl());
        segmentContent.writeByte(lfoSegment.getMinRate());
        segmentContent.writeByte(lfoSegment.getMaxRate());
        segmentContent.writeByte(lfoSegment.getPhase());
        segmentContent.writeByte(lfoSegment.getShape());
        segmentContent.writeByte(lfoSegment.getRfu2());
        byte[] result = segmentContent.toByteArray();
        segment.updateContent(result, out.getBytePos());
        out.writeBytes(result);
    }

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
