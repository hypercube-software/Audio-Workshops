package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io.program.segment;

import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment.KFAsrSegment;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment.KFProgramSegment;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamReader;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamWriter;

public class KFAsrDeserializer implements KFSegmentDeserializer {

    @Override
    public void serialize(KFProgramSegment segment, BitStreamWriter out) {
        KFAsrSegment asrSegment = (KFAsrSegment) segment;
        BitStreamWriter segmentContent = new BitStreamWriter();
        segmentContent.writeByte(asrSegment.getRfu1());
        segmentContent.writeByte(asrSegment.getTrigger());
        segmentContent.writeByte(asrSegment.getFlags());
        segmentContent.writeByte(asrSegment.getDtime());
        segmentContent.writeByte(asrSegment.getAtime());
        segmentContent.writeByte(asrSegment.getRfu2());
        segmentContent.writeByte(asrSegment.getRtime());
        byte[] result = segmentContent.toByteArray();
        segment.updateContent(result, out.getBytePos());
        out.writeBytes(result);
    }

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
