package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io.program.segment;

import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment.KFHobbesSegment;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment.KFProgramSegment;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamReader;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamWriter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KFHobbesDeserializer implements KFSegmentDeserializer {

    @Override
    public void serialize(KFProgramSegment segment, BitStreamWriter out) {
        KFHobbesSegment hobbes = (KFHobbesSegment) segment;
        BitStreamWriter segmentContent = new BitStreamWriter();
        segmentContent.writeByte(hobbes.getSubTag());
        segmentContent.writeByte(hobbes.getCoarse());
        segmentContent.writeByte(hobbes.getFine());
        segmentContent.writeByte(hobbes.getKScale());
        segmentContent.writeByte(hobbes.getVScale());
        segmentContent.writeByte(hobbes.getControl());
        segmentContent.writeByte(hobbes.getRange());
        segmentContent.writeByte(hobbes.getDepth());
        segmentContent.writeByte(hobbes.getMinDepth());
        segmentContent.writeByte(hobbes.getMaxDepth());
        segmentContent.writeByte(hobbes.getSource());
        segmentContent.writeByte(hobbes.getBitfields1());
        segmentContent.writeByte(hobbes.getMoreTscr());
        segmentContent.writeByte(hobbes.getBitfields2());
        segmentContent.writeByte(hobbes.getBitfields3());
        byte[] result = segmentContent.toByteArray();
        segment.updateContent(result, out.getBytePos());
        out.writeBytes(result);
    }

    @Override
    public void deserialize(KFProgramSegment segment) {
        KFHobbesSegment hobbesSegment = (KFHobbesSegment) segment;
        BitStreamReader in = hobbesSegment.getSegmentContent()
                .bitStreamReader();
        hobbesSegment.setSubTag(in.readByte());
        hobbesSegment.setCoarse(in.readByte());
        hobbesSegment.setFine(in.readByte());
        hobbesSegment.setKScale(in.readByte());
        hobbesSegment.setVScale(in.readByte());
        hobbesSegment.setControl(in.readByte());
        hobbesSegment.setRange(in.readByte());
        hobbesSegment.setDepth(in.readByte());
        hobbesSegment.setMinDepth(in.readByte());
        hobbesSegment.setMaxDepth(in.readByte());
        hobbesSegment.setSource(in.readByte());
        hobbesSegment.setBitfields1(in.readByte());
        hobbesSegment.setMoreTscr(in.readByte());
        hobbesSegment.setBitfields2(in.readByte());
        hobbesSegment.setBitfields3(in.readByte());
    }
}
