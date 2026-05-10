package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io.program.segment;

import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment.KFEncSegment;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment.KFProgramSegment;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamReader;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamWriter;

public class KFEncDeserializer implements KFSegmentDeserializer {

    @Override
    public void serialize(KFProgramSegment segment, BitStreamWriter out) {
        KFEncSegment enc = (KFEncSegment) segment;
        BitStreamWriter segmentContent = new BitStreamWriter();
        segmentContent.writeByte(enc.getRfu1());
        segmentContent.writeByte(enc.getFlags());
        segmentContent.writeByte(enc.getAtTime());
        segmentContent.writeByte(enc.getAtKScale());
        segmentContent.writeByte(enc.getAtVScale());
        segmentContent.writeByte(enc.getAtCtl());
        segmentContent.writeByte(enc.getAtRange());
        segmentContent.writeByte(enc.getDtTime());
        segmentContent.writeByte(enc.getDtKScale());
        segmentContent.writeByte(enc.getDtCtl());
        segmentContent.writeByte(enc.getDtRange());
        segmentContent.writeByte(enc.getRtTime());
        segmentContent.writeByte(enc.getRtKScale());
        segmentContent.writeByte(enc.getRtCtl());
        segmentContent.writeByte(enc.getRtRange());
        byte[] result = segmentContent.toByteArray();
        segment.updateContent(result, out.getBytePos());
        out.writeBytes(result);
    }

    @Override
    public void deserialize(KFProgramSegment segment) {
        KFEncSegment encSegment = (KFEncSegment) segment;
        BitStreamReader in = encSegment.getSegmentContent()
                .bitStreamReader();
        encSegment.setRfu1(in.readByte());
        encSegment.setFlags(in.readByte());
        encSegment.setAtTime(in.readByte());
        encSegment.setAtKScale(in.readByte());
        encSegment.setAtVScale(in.readByte());
        encSegment.setAtCtl(in.readByte());
        encSegment.setAtRange(in.readByte());
        encSegment.setDtTime(in.readByte());
        encSegment.setDtKScale(in.readByte());
        encSegment.setDtCtl(in.readByte());
        encSegment.setDtRange(in.readByte());
        encSegment.setRtTime(in.readByte());
        encSegment.setRtKScale(in.readByte());
        encSegment.setRtCtl(in.readByte());
        encSegment.setRtRange(in.readByte());
    }
}
