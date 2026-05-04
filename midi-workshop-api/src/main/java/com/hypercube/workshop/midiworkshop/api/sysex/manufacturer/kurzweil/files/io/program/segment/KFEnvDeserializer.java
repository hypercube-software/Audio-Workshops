package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io.program.segment;

import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment.KFEnvSegment;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment.KFProgramSegment;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamReader;

public class KFEnvDeserializer implements KFSegmentDeserializer {

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
