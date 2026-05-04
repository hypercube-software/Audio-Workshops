package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io.program.segment;

import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment.KFProgramCommon;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment.KFProgramSegment;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamReader;

public class KFProgramCommonDeserializer implements KFSegmentDeserializer {

    @Override
    public void deserialize(KFProgramSegment segment) {
        KFProgramCommon common = (KFProgramCommon) segment;
        BitStreamReader in = common.getSegmentContent()
                .bitStreamReader();
        common.setFmt(in.readByte());
        common.setNumLayers(in.readByte());
        common.setModeFlags(in.readByte());
        common.setBendRange(in.readByte());
        common.setPortSlope(in.readByte());
        common.setMixControl(in.readByte());
        common.setMixRange(in.readByte());
        common.setCoarse1(in.readByte());
        common.setControl1(in.readByte());
        common.setRange1(in.readByte());
        common.setDest1(in.readByte());
        common.setCoarse2(in.readByte());
        common.setControl2(in.readByte());
        common.setRange2(in.readByte());
        common.setDest2(in.readByte());
    }
}
