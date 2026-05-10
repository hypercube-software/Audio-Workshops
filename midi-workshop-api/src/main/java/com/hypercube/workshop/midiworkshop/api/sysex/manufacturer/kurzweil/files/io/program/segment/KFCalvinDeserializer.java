package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io.program.segment;

import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment.KFCalvinSegment;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment.KFProgramSegment;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamReader;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamWriter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KFCalvinDeserializer implements KFSegmentDeserializer {

    @Override
    public void serialize(KFProgramSegment segment, BitStreamWriter out) {
        KFCalvinSegment calvin = (KFCalvinSegment) segment;
        BitStreamWriter segmentContent = new BitStreamWriter();
        segmentContent.writeByte(calvin.getSubTag());
        segmentContent.writeByte(calvin.getTrans());
        segmentContent.writeByte(calvin.getDtune());
        segmentContent.writeByte(calvin.getTkScale());
        segmentContent.writeByte(calvin.getTvScale());
        segmentContent.writeByte(calvin.getTcontrol());
        segmentContent.writeByte(calvin.getTrange());
        segmentContent.writeShort(calvin.getSkeymap());
        segmentContent.writeByte(calvin.getSroot());
        segmentContent.writeByte(calvin.getSlegato());
        segmentContent.writeShort(calvin.getKeymap());
        segmentContent.writeByte(calvin.getRoot());
        segmentContent.writeByte(calvin.getLegato());
        segmentContent.writeByte(calvin.getTshift());
        segmentContent.writeByte(calvin.getRfu2());
        segmentContent.writeByte(calvin.getCpitch());
        segmentContent.writeByte(calvin.getFpitch());
        segmentContent.writeByte(calvin.getCkScale());
        segmentContent.writeByte(calvin.getCvScale());
        segmentContent.writeByte(calvin.getPcontrol());
        segmentContent.writeByte(calvin.getPrange());
        segmentContent.writeByte(calvin.getPdepth());
        segmentContent.writeByte(calvin.getPmin());
        segmentContent.writeByte(calvin.getPmax());
        segmentContent.writeByte(calvin.getPsource());
        segmentContent.writeShort(calvin.getCcr());
        segmentContent.writeByte(calvin.getAlg());
        segmentContent.writeByte(calvin.getFineHz());
        byte[] result = segmentContent.toByteArray();
        segment.updateContent(result, out.getBytePos());
        out.writeBytes(result);
    }

    @Override
    public void deserialize(KFProgramSegment segment) {
        KFCalvinSegment calvinSegment = (KFCalvinSegment) segment;
        BitStreamReader in = calvinSegment.getSegmentContent()
                .bitStreamReader();
        calvinSegment.setSubTag(in.readByte());
        calvinSegment.setTrans(in.readByte());
        calvinSegment.setDtune(in.readByte());
        calvinSegment.setTkScale(in.readByte());
        calvinSegment.setTvScale(in.readByte());
        calvinSegment.setTcontrol(in.readByte());
        calvinSegment.setTrange(in.readByte());
        calvinSegment.setSkeymap(in.readShort());
        calvinSegment.setSroot(in.readByte());
        calvinSegment.setSlegato(in.readByte());
        calvinSegment.setKeymap(in.readShort());
        calvinSegment.setRoot(in.readByte());
        calvinSegment.setLegato(in.readByte());
        calvinSegment.setTshift(in.readByte());
        calvinSegment.setRfu2(in.readByte());
        calvinSegment.setCpitch(in.readByte());
        calvinSegment.setFpitch(in.readByte());
        calvinSegment.setCkScale(in.readByte());
        calvinSegment.setCvScale(in.readByte());
        calvinSegment.setPcontrol(in.readByte());
        calvinSegment.setPrange(in.readByte());
        calvinSegment.setPdepth(in.readByte());
        calvinSegment.setPmin(in.readByte());
        calvinSegment.setPmax(in.readByte());
        calvinSegment.setPsource(in.readByte());
        calvinSegment.setCcr(in.readShort());
        calvinSegment.setAlg(in.readByte());
        calvinSegment.setFineHz(in.readByte());
        log.info("Using keymap {} and skeyMap {}", calvinSegment.getKeymap(), calvinSegment.getSkeymap());
    }
}
