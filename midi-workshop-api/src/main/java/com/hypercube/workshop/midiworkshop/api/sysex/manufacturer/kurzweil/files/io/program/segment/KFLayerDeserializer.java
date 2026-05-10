package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io.program.segment;

import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment.KFLayerSegment;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment.KFProgramSegment;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamReader;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamWriter;

public class KFLayerDeserializer implements KFSegmentDeserializer {

    @Override
    public void serialize(KFProgramSegment segment, BitStreamWriter out) {
        KFLayerSegment layer = (KFLayerSegment) segment;
        BitStreamWriter segmentContent = new BitStreamWriter();
        segmentContent.writeByte(layer.getLoEnable());
        segmentContent.writeByte(layer.getTrans());
        segmentContent.writeByte(layer.getTune());
        segmentContent.writeByte(layer.getLoKey());
        segmentContent.writeByte(layer.getHiKey());
        segmentContent.writeByte(layer.getVRange());
        segmentContent.writeByte(layer.getESwitch());
        segmentContent.writeByte(layer.getFlags());
        segmentContent.writeByte(layer.getMoreFlags());
        segmentContent.writeByte(layer.getVTrig());
        segmentContent.writeByte(layer.getHiEnable());
        segmentContent.writeByte(layer.getDlyCtl());
        segmentContent.writeByte(layer.getDlyMin());
        segmentContent.writeByte(layer.getDlyMax());
        segmentContent.writeByte(layer.getXfade());
        byte[] result = segmentContent.toByteArray();
        segment.updateContent(result, out.getBytePos());
        out.writeBytes(result);
    }

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
