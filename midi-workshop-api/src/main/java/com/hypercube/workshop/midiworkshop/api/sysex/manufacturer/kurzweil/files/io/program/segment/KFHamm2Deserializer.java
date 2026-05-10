package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io.program.segment;

import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment.KFHammSegment2;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment.KFProgramSegment;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamReader;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamWriter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KFHamm2Deserializer implements KFSegmentDeserializer {

    @Override
    public void serialize(KFProgramSegment segment, BitStreamWriter out) {
        KFHammSegment2 h = (KFHammSegment2) segment;
        BitStreamWriter sw = new BitStreamWriter();
        sw.writeByte(h.getNoizpitch());
        sw.writeByte(h.getNoizlevel());
        sw.writeByte(h.getNoizdecay());
        sw.writeByte(h.getPerc1());
        sw.writeByte(h.getPerc2());
        sw.writeByte(h.getNoizvel());
        sw.writeByte(h.getPercvel());
        for (int v : h.getPerclevel()) {
            sw.writeByte(v);
        }
        for (int v : h.getPercdecay()) {
            sw.writeByte(v);
        }
        for (int v : h.getPerclcomp()) {
            sw.writeByte(v);
        }
        for (int v : h.getRfu2()) {
            sw.writeByte(v);
        }
        sw.writeByte(h.getBass_q());
        sw.writeByte(h.getTreb_q());
        sw.writeByte(h.getNoizrtrig());
        sw.writeByte(h.getLeakmode());
        sw.writeByte(h.getLeslieCtl());
        sw.writeByte(h.getChorusCtl());
        sw.writeByte(h.getChorusSelect());
        sw.writeByte(h.getRfu3());

        byte[] result = sw.toByteArray();
        segment.updateContent(result, out.getBytePos());
        out.writeBytes(result);
    }

    @Override
    public void deserialize(KFProgramSegment segment) {
        KFHammSegment2 h = (KFHammSegment2) segment;
        BitStreamReader in = h.getSegmentContent()
                .bitStreamReader();
        h.setNoizpitch(in.readByte());
        h.setNoizlevel(in.readByte());
        h.setNoizdecay(in.readByte());
        h.setPerc1(in.readByte());
        h.setPerc2(in.readByte());
        h.setNoizvel(in.readByte());
        h.setPercvel(in.readByte());
        int[] perclevel = new int[4];
        for (int i = 0; i < 4; i++) perclevel[i] = in.readByte();
        h.setPerclevel(perclevel);
        int[] percdecay = new int[4];
        for (int i = 0; i < 4; i++) percdecay[i] = in.readByte();
        h.setPercdecay(percdecay);
        int[] perclcomp = new int[4];
        for (int i = 0; i < 4; i++) perclcomp[i] = in.readByte();
        h.setPerclcomp(perclcomp);
        int[] rfu2 = new int[4];
        for (int i = 0; i < 4; i++) rfu2[i] = in.readByte();
        h.setRfu2(rfu2);
        h.setBass_q(in.readByte());
        h.setTreb_q(in.readByte());
        h.setNoizrtrig(in.readByte());
        h.setLeakmode(in.readByte());
        h.setLeslieCtl(in.readByte());
        h.setChorusCtl(in.readByte());
        h.setChorusSelect(in.readByte());
        h.setRfu3(in.readByte());
    }
}
