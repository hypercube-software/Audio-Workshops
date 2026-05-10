package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io.program.segment;

import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment.KFFXRootTableSegment;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment.KFProgramSegment;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamReader;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamWriter;

public class KFFXRootTableDeserializer implements KFSegmentDeserializer {

    @Override
    public void serialize(KFProgramSegment segment, BitStreamWriter out) {
        KFFXRootTableSegment rootTableSegment = (KFFXRootTableSegment) segment;
        BitStreamWriter segmentContent = new BitStreamWriter();
        segmentContent.writeByte(rootTableSegment.getVers());
        segmentContent.writeShort(rootTableSegment.getStudio());
        for (var rfu : rootTableSegment.getRfu()) {
            segmentContent.writeByte(rfu);
        }
        byte[] result = segmentContent.toByteArray();
        segment.updateContent(result, out.getBytePos());
        out.writeBytes(result);
    }

    @Override
    public void deserialize(KFProgramSegment segment) {
        KFFXRootTableSegment rootTableSegment = (KFFXRootTableSegment) segment;
        BitStreamReader in = rootTableSegment.getSegmentContent()
                .bitStreamReader();
        rootTableSegment.setVers(in.readByte());
        rootTableSegment.setStudio(in.readShort());
        int[] rfu = new int[4];
        for (int i = 0; i < rfu.length; i++) {
            rfu[i] = in.readByte();
        }
        rootTableSegment.setRfu(rfu);
    }
}
