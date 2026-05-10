package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io.program.segment;

import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment.KFEnvSegment;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment.KFProgramSegment;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamReader;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamWriter;

public class KFEnvDeserializer implements KFSegmentDeserializer {

    @Override
    public void serialize(KFProgramSegment segment, BitStreamWriter out) {
        KFEnvSegment seg = (KFEnvSegment) segment;
        BitStreamWriter segmentContent = new BitStreamWriter();
        segmentContent.writeByte(seg.getFlags());
        for (int i = 0; i < 7; i++) {
            segmentContent.writeByte(seg.getSegs()[i][0]);
            segmentContent.writeByte(seg.getSegs()[i][1]);
        }
        byte[] result = segmentContent.toByteArray();
        segment.updateContent(result, out.getBytePos());
        out.writeBytes(result);
    }

    @Override
    public void deserialize(KFProgramSegment segment) {
        KFEnvSegment seg = (KFEnvSegment) segment;
        BitStreamReader in = seg.getSegmentContent()
                .bitStreamReader();
        seg.setFlags(in.readByte());
        int[][] segs = new int[7][2];
        for (int i = 0; i < 7; i++) {
            segs[i][0] = in.readByte();
            segs[i][1] = in.readByte();
        }
        seg.setSegs(segs);
    }
}
