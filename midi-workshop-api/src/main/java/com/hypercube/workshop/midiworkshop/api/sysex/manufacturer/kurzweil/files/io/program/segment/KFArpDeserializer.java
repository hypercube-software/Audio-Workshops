package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io.program.segment;

import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment.KFArpSegment;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment.KFProgramSegment;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamReader;

public class KFArpDeserializer implements KFSegmentDeserializer {

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
