package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io.program.segment;

import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment.KFArpSegment;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment.KFProgramSegment;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamReader;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamWriter;

public class KFArpDeserializer implements KFSegmentDeserializer {

    @Override
    public void serialize(KFProgramSegment segment, BitStreamWriter out) {
        KFArpSegment arpSegment = (KFArpSegment) segment;
        BitStreamWriter segmentContent = new BitStreamWriter();
        segmentContent.writeByte(arpSegment.getHiKey());
        segmentContent.writeByte(arpSegment.getInitialState());
        segmentContent.writeByte(arpSegment.getLatchMode());
        segmentContent.writeByte(arpSegment.getPlayOrder());
        segmentContent.writeByte(arpSegment.getGlissando());
        segmentContent.writeByte(arpSegment.getTempoSource());
        segmentContent.writeByte(arpSegment.getOnOffControl());
        segmentContent.writeShort(arpSegment.getClocksPerBeat());
        segmentContent.writeShort(arpSegment.getDurationPerBeat());
        segmentContent.writeShort(arpSegment.getInitialTempo());
        segmentContent.writeByte(arpSegment.getVelocityMode());
        segmentContent.writeByte(arpSegment.getVelocityFixed());
        segmentContent.writeByte(arpSegment.getVelocityCtrl());
        segmentContent.writeByte(arpSegment.getNoteShift());
        segmentContent.writeByte(arpSegment.getShiftLimit());
        segmentContent.writeByte(arpSegment.getLimitOption());
        segmentContent.writeByte(arpSegment.getArpSyncFlags());
        segmentContent.writeByte(arpSegment.getRfu1());
        segmentContent.writeShort(arpSegment.getRfu2());
        segmentContent.writeShort(arpSegment.getRfu3());
        byte[] result = segmentContent.toByteArray();
        segment.updateContent(result, out.getBytePos());
        out.writeBytes(result);
    }

    @Override
    public void deserialize(KFProgramSegment segment) {
        KFArpSegment arpSegment = (KFArpSegment) segment;
        BitStreamReader in = arpSegment.getSegmentContent()
                .bitStreamReader();
        // tag: First byte of arpb is loKey, but we set it as tag if we follow the pattern
        arpSegment.setHiKey(in.readByte());
        arpSegment.setInitialState(in.readByte());
        arpSegment.setLatchMode(in.readByte());
        arpSegment.setPlayOrder(in.readByte());
        arpSegment.setGlissando(in.readByte());
        arpSegment.setTempoSource(in.readByte());
        arpSegment.setOnOffControl(in.readByte());
        arpSegment.setClocksPerBeat(in.readShort());
        arpSegment.setDurationPerBeat(in.readShort());
        arpSegment.setInitialTempo(in.readShort());
        arpSegment.setVelocityMode(in.readByte());
        arpSegment.setVelocityFixed(in.readByte());
        arpSegment.setVelocityCtrl(in.readByte());
        arpSegment.setNoteShift(in.readByte());
        arpSegment.setShiftLimit(in.readByte());
        arpSegment.setLimitOption(in.readByte());
        arpSegment.setArpSyncFlags(in.readByte());
        arpSegment.setRfu1(in.readByte());
        arpSegment.setRfu2(in.readShort());
        arpSegment.setRfu3(in.readShort());
    }
}
