package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io.program.segment;

import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment.KFHammSegment1;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment.KFProgramSegment;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamReader;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamWriter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KFHamm1Deserializer implements KFSegmentDeserializer {

    @Override
    public void serialize(KFProgramSegment segment, BitStreamWriter out) {
        KFHammSegment1 h = (KFHammSegment1) segment;
        BitStreamWriter segmentContent = new BitStreamWriter();
        segmentContent.writeByte(h.getVersion());
        segmentContent.writeByte(h.getBasekey());
        segmentContent.writeByte(h.getNwheels());
        segmentContent.writeByte(h.getBitfields1());
        segmentContent.writeByte(h.getToneleak());
        segmentContent.writeByte(h.getResmap());
        for (int bar : h.getBartones()) segmentContent.writeByte(bar);
        segmentContent.writeByte(h.getVolmap());
        segmentContent.writeByte(h.getVolume());
        segmentContent.writeByte(h.getBalance());
        segmentContent.writeByte(h.getEmph_g());
        segmentContent.writeByte(h.getEmph_f());
        segmentContent.writeByte(h.getBass_g());
        segmentContent.writeByte(h.getBass_f());
        segmentContent.writeByte(h.getPar1_g());
        segmentContent.writeByte(h.getPar1_f());
        segmentContent.writeByte(h.getPar1_q());
        segmentContent.writeByte(h.getPar2_g());
        segmentContent.writeByte(h.getPar2_f());
        segmentContent.writeByte(h.getPar2_q());
        segmentContent.writeByte(h.getTreb_g());
        segmentContent.writeByte(h.getTreb_f());
        segmentContent.writeByte(h.getNoizvol());

        byte[] result = segmentContent.toByteArray();
        segment.updateContent(result, out.getBytePos());
        out.writeBytes(result);
    }

    @Override
    public void deserialize(KFProgramSegment segment) {
        KFHammSegment1 h = (KFHammSegment1) segment;
        BitStreamReader in = h.getSegmentContent()
                .bitStreamReader();
        h.setVersion(in.readByte());
        h.setBasekey(in.readByte());
        h.setNwheels(in.readByte());
        h.setBitfields1(in.readByte());
        h.setToneleak(in.readByte());
        h.setResmap(in.readByte());
        int[] bartones = new int[9];
        for (int i = 0; i < 9; i++) bartones[i] = in.readByte();
        h.setBartones(bartones);
        h.setVolmap(in.readByte());
        h.setVolume(in.readByte());
        h.setBalance(in.readByte());
        h.setEmph_g(in.readByte());
        h.setEmph_f(in.readByte());
        h.setBass_g(in.readByte());
        h.setBass_f(in.readByte());
        h.setPar1_g(in.readByte());
        h.setPar1_f(in.readByte());
        h.setPar1_q(in.readByte());
        h.setPar2_g(in.readByte());
        h.setPar2_f(in.readByte());
        h.setPar2_q(in.readByte());
        h.setTreb_g(in.readByte());
        h.setTreb_f(in.readByte());
        h.setNoizvol(in.readByte());
    }
}
