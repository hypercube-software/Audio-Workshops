package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io.program.segment;

import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment.KFLayerSegment;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment.KFProgramSegment;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamReader;

public class KFLayerDeserializer implements KFSegmentDeserializer {

    @Override
    public void deserialize(KFProgramSegment segment) {
        KFLayerSegment layerSegment = (KFLayerSegment) segment;
        BitStreamReader in = layerSegment.getSegmentContent()
                .bitStreamReader();
        layerSegment.setLoEnable(in.readByte());
        layerSegment.setTrans(in.readByte());
        layerSegment.setTune(in.readByte());
        layerSegment.setLoKey(in.readByte());
        layerSegment.setHiKey(in.readByte());
        layerSegment.setVRange(in.readByte());
        layerSegment.setESwitch(in.readByte());
        layerSegment.setFlags(in.readByte());
        layerSegment.setMoreFlags(in.readByte());
        layerSegment.setVTrig(in.readByte());
        layerSegment.setHiEnable(in.readByte());
        layerSegment.setDlyCtl(in.readByte());
        layerSegment.setDlyMin(in.readByte());
        layerSegment.setDlyMax(in.readByte());
        layerSegment.setXfade(in.readByte());
    }
}
