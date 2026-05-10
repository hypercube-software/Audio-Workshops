package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io.program.segment;

import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment.KFChannelSegment;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment.KFProgramSegment;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamReader;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamWriter;

public class KFChannelDeserializer implements KFSegmentDeserializer {

    @Override
    public void serialize(KFProgramSegment segment, BitStreamWriter out) {
        KFChannelSegment chan = (KFChannelSegment) segment;
        BitStreamWriter segmentContent = new BitStreamWriter();
        segmentContent.writeByte(chan.getChan());
        segmentContent.writeByte(chan.getNlyrs());
        segmentContent.writeByte(chan.getFlags());
        segmentContent.writeShort(chan.getProg());
        segmentContent.writeByte(chan.getVolume());
        segmentContent.writeByte(chan.getPan());
        segmentContent.writeByte(chan.getTrans());
        segmentContent.writeByte(chan.getDtune());
        segmentContent.writeByte(chan.getBrange());
        segmentContent.writeByte(chan.getPlayflags());
        segmentContent.writeByte(chan.getPortRate());
        segmentContent.writeByte(chan.getOutflags());
        segmentContent.writeByte(chan.getRfu1());
        segmentContent.writeByte(chan.getRfu2());
        byte[] result = segmentContent.toByteArray();
        segment.updateContent(result, out.getBytePos());
        out.writeBytes(result);
    }

    @Override
    public void deserialize(KFProgramSegment segment) {
        KFChannelSegment channelSegment = (KFChannelSegment) segment;
        BitStreamReader in = channelSegment.getSegmentContent()
                .bitStreamReader();
        channelSegment.setChan(in.readByte());
        channelSegment.setNlyrs(in.readByte());
        channelSegment.setFlags(in.readByte());
        channelSegment.setProg(in.readShort());
        channelSegment.setVolume(in.readByte());
        channelSegment.setPan(in.readByte());
        channelSegment.setTrans(in.readByte());
        channelSegment.setDtune(in.readByte());
        channelSegment.setBrange(in.readByte());
        channelSegment.setPlayflags(in.readByte());
        channelSegment.setPortRate(in.readByte());
        channelSegment.setOutflags(in.readByte());
        channelSegment.setRfu1(in.readByte());
        channelSegment.setRfu2(in.readByte());
    }
}
