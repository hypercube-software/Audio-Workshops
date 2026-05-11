package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io.sample;

import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io.KFDeserializer;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.soundblock.KFSoundBlockHeader;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamReader;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamWriter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KFSoundBlockHeaderDeserializer extends KFDeserializer {

    public KFSoundBlockHeader deserialize(BitStreamReader in) {
        KFSoundBlockHeader header = new KFSoundBlockHeader();
        header.setRootk(in.readByte());
        header.setFlags(in.readByte());
        header.setAmp1(in.readByte());
        header.setAmp2(in.readByte());
        header.setPitch(in.readShort());
        header.setNameOffset(in.readShort());
        header.setSos(in.readUnsignedInt32());
        header.setAlt(in.readUnsignedInt32());
        header.setLos(in.readUnsignedInt32());
        header.setEos(in.readUnsignedInt32());
        header.setEnv1(in.readShort());
        header.setEnv2(in.readShort());
        header.setSrate(in.readUnsignedInt32());
        return header;
    }

    public void serialize(KFSoundBlockHeader header, BitStreamWriter out) {
        out.writeByte(header.getRootk());
        out.writeByte(header.getFlags());
        out.writeByte(header.getAmp1());
        out.writeByte(header.getAmp2());
        out.writeShort(header.getPitch());
        out.writeShort(header.getNameOffset());
        out.writeInt32(header.getSos());
        out.writeInt32(header.getAlt());
        out.writeInt32(header.getLos());
        out.writeInt32(header.getEos());
        out.writeShort(header.getEnv1());
        out.writeShort(header.getEnv2());
        out.writeInt32(header.getSrate());
    }
}
