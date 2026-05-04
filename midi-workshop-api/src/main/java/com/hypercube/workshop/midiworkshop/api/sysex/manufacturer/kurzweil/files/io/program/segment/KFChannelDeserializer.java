package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io.program.segment;

import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment.KFChannelSegment;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment.KFProgramSegment;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamReader;

public class KFChannelDeserializer implements KFSegmentDeserializer {

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
