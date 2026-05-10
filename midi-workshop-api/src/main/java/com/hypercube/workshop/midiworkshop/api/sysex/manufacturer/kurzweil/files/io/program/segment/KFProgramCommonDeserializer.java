package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io.program.segment;

import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment.KFProgramCommon;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment.KFProgramSegment;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamReader;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamWriter;

public class KFProgramCommonDeserializer implements KFSegmentDeserializer {

    @Override
    public void serialize(KFProgramSegment segment, BitStreamWriter out) {
        KFProgramCommon pgm = (KFProgramCommon) segment;
        BitStreamWriter segmentContent = new BitStreamWriter();
        segmentContent.writeByte(pgm.getFmt());
        segmentContent.writeByte(pgm.getNumLayers());
        segmentContent.writeByte(pgm.getModeFlags());
        segmentContent.writeByte(pgm.getBendRange());
        segmentContent.writeByte(pgm.getPortSlope());
        segmentContent.writeByte(pgm.getMixControl());
        segmentContent.writeByte(pgm.getMixRange());
        segmentContent.writeByte(pgm.getCoarse1());
        segmentContent.writeByte(pgm.getControl1());
        segmentContent.writeByte(pgm.getRange1());
        segmentContent.writeByte(pgm.getDest1());
        segmentContent.writeByte(pgm.getCoarse2());
        segmentContent.writeByte(pgm.getControl2());
        segmentContent.writeByte(pgm.getRange2());
        segmentContent.writeByte(pgm.getDest2());
        byte[] result = segmentContent.toByteArray();
        segment.updateContent(result, out.getBytePos());
        out.writeBytes(result);
    }

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
