package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io.program.segment;

import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment.KFHammSegment3;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment.KFProgramSegment;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamReader;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamWriter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KFHamm3Deserializer implements KFSegmentDeserializer {

    @Override
    public void serialize(KFProgramSegment segment, BitStreamWriter out) {
        KFHammSegment3 h = (KFHammSegment3) segment;
        BitStreamWriter sw = new BitStreamWriter();
        for (int bar : h.getDrawbars()) {
            sw.writeByte(bar);
        }
        sw.writeByte(h.getRfu6());
        sw.writeByte(h.getRfu7());
        sw.writeByte(h.getKeyclick());
        sw.writeByte(h.getRelclick());
        sw.writeByte(h.getNoizran());
        sw.writeByte(h.getRfu4());
        sw.writeByte(h.getLoTune());
        sw.writeByte(h.getRcvMap());
        for (int rfu : h.getRfu5()) {
            sw.writeByte(rfu);
        }

        byte[] result = sw.toByteArray();
        segment.updateContent(result, out.getBytePos());
        out.writeBytes(result);
    }

    @Override
    public void deserialize(KFProgramSegment segment) {
        KFHammSegment3 h = (KFHammSegment3) segment;
        BitStreamReader in = h.getSegmentContent()
                .bitStreamReader();
        int[] drawbars = new int[9];
        for (int i = 0; i < 9; i++) drawbars[i] = in.readByte();
        h.setDrawbars(drawbars);
        h.setRfu6(in.readByte());
        h.setRfu7(in.readByte());
        h.setKeyclick(in.readByte());
        h.setRelclick(in.readByte());
        h.setNoizran(in.readByte());
        h.setRfu4(in.readByte());
        h.setLoTune(in.readByte());
        h.setRcvMap(in.readByte());
        int[] rfu5 = new int[14];
        for (int i = 0; i < 14; i++) rfu5[i] = in.readByte();
        h.setRfu5(rfu5);
    }
}
