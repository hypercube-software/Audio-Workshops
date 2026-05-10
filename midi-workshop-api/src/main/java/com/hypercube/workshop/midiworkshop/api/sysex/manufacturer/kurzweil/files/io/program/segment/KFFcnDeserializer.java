package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io.program.segment;

import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment.KFFcnSegment;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment.KFProgramSegment;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamReader;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamWriter;

public class KFFcnDeserializer implements KFSegmentDeserializer {

    @Override
    public void serialize(KFProgramSegment segment, BitStreamWriter out) {
        KFFcnSegment fcnSegment = (KFFcnSegment) segment;
        BitStreamWriter segmentContent = new BitStreamWriter();
        segmentContent.writeByte(fcnSegment.getOp());
        segmentContent.writeByte(fcnSegment.getArg1());
        segmentContent.writeByte(fcnSegment.getArg2());
        byte[] result = segmentContent.toByteArray();
        segment.updateContent(result, out.getBytePos());
        out.writeBytes(result);
    }

    @Override
    public void deserialize(KFProgramSegment segment) {
        KFFcnSegment fcnSegment = (KFFcnSegment) segment;
        BitStreamReader in = fcnSegment.getSegmentContent()
                .bitStreamReader();
        fcnSegment.setOp(in.readByte());
        fcnSegment.setArg1(in.readByte());
        fcnSegment.setArg2(in.readByte());
    }
}
