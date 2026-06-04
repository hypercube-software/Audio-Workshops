package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io.studio;

import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io.KFDeserializer;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.RawData;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.studio.FXBus;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.studio.FXInput;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.studio.KFStudio;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamReader;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamWriter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class KFStudioDeserializer extends KFDeserializer {

    public KFStudio deserialize(RawData data, int objectId, String name) {
        BitStreamReader in = data.bitStreamReader();
        if (name == null) {
            name = readName(in);
        }

        KFStudio studio = new KFStudio(data, name, objectId);

        // Read FXStudio fields (36 bytes initially from header)
        studio.setVersion(in.readByte() & 0xFF);
        studio.setMixSend(in.readByte() & 0xFF);
        studio.setAuxFXID(in.readShort() & 0xFFFF);
        studio.setAuxMixLevel(in.readByte() & 0xFF);
        studio.setAuxMixBal(in.readByte() & 0xFF);
        studio.setMixLevel(in.readByte() & 0xFF);
        studio.setMixBal(in.readByte() & 0xFF);
        studio.setAuxWetDry(in.readByte() & 0xFF);
        studio.setAuxWetDry2(in.readByte() & 0xFF);
        studio.setAuxWetDrySel(in.readByte() & 0xFF);
        studio.setAuxWetDrySel2(in.readByte() & 0xFF);

        int[] mixCoeffs = new int[20];
        for (int i = 0; i < 20; i++) {
            mixCoeffs[i] = in.readByte() & 0xFF;
        }
        studio.setMixCoeffs(mixCoeffs);

        List<Integer> theOutputs = new ArrayList<>(4);
        for (int i = 0; i < 4; i++) {
            theOutputs.add(in.readByte() & 0xFF);
        }
        studio.setTheOutputs(theOutputs);

        // Read FXInput array (8 inputs)
        List<FXInput> theInputs = new ArrayList<>(8);
        for (int i = 0; i < 8; i++) {
            theInputs.add(deserializeFXInput(in));
        }
        studio.setTheInputs(theInputs);

        // Read FXBus array (4 busses)
        List<FXBus> theBusses = new ArrayList<>(4);
        for (int i = 0; i < 4; i++) {
            theBusses.add(deserializeFXBus(in));
        }
        studio.setTheBusses(theBusses);

        // Consume the remaining 76 bytes identified as FXBus[4]
        // This is not stored in KFStudio currently, but needs to be consumed.
        in.readBytes(76);

        try {
            // This is just for debugging, can be removed later
            Files.write(Path.of("./target/studio.dat"), data.getContent());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return studio;
    }

    public byte[] serialize(KFStudio studio) {
        BitStreamWriter out = new BitStreamWriter();

        // Write name (assuming name length and name itself are handled by the caller or a helper)
        writeName(studio.getName(), out);

        // Write FXStudio fields (36 bytes initially from header)
        out.writeByte((byte) studio.getVersion());
        out.writeByte((byte) studio.getMixSend());
        out.writeShort((short) studio.getAuxFXID());
        out.writeByte((byte) studio.getAuxMixLevel());
        out.writeByte((byte) studio.getAuxMixBal());
        out.writeByte((byte) studio.getMixLevel());
        out.writeByte((byte) studio.getMixBal());
        out.writeByte((byte) studio.getAuxWetDry());
        out.writeByte((byte) studio.getAuxWetDry2());
        out.writeByte((byte) studio.getAuxWetDrySel());
        out.writeByte((byte) studio.getAuxWetDrySel2());

        for (int coeff : studio.getMixCoeffs()) {
            out.writeByte((byte) coeff);
        }

        for (int output : studio.getTheOutputs()) {
            out.writeByte((byte) output);
        }

        for (FXInput fxInput : studio.getTheInputs()) {
            serializeFXInput(out, fxInput);
        }

        for (FXBus fxBus : studio.getTheBusses()) {
            serializeFXBus(out, fxBus);
        }

        // The trailing 76 bytes (FXBus[4]) are not part of the KFStudio model,
        // so we don't serialize them here. If they need to be preserved on round-trip,
        // KFStudio would need a field to store them.

        return out.toByteArray();
    }

    private FXInput deserializeFXInput(BitStreamReader in) {
        FXInput fxInput = new FXInput();
        fxInput.setVersion(in.readByte() & 0xFF);
        fxInput.setBusID(in.readByte() & 0xFF);
        fxInput.setMode(in.readByte() & 0xFF);
        fxInput.setUseStereo(in.readByte() & 0xFF);
        fxInput.setEqEnable(in.readByte() & 0xFF);
        fxInput.setEq1Type(in.readByte() & 0xFF);
        fxInput.setEq1Level(in.readByte() & 0xFF);
        fxInput.setEq1Freq(in.readByte() & 0xFF);
        fxInput.setEq1Q(in.readByte() & 0xFF);
        fxInput.setRfu1(in.readByte() & 0xFF);
        fxInput.setEq2Type(in.readByte() & 0xFF);
        fxInput.setEq2Level(in.readByte() & 0xFF);
        fxInput.setEq2Freq(in.readByte() & 0xFF);
        fxInput.setEq2Q(in.readByte() & 0xFF);
        fxInput.setRfu2(in.readByte() & 0xFF);
        fxInput.setSend1Type(in.readByte() & 0xFF);
        fxInput.setSend1Assign(in.readByte() & 0xFF);
        fxInput.setSend1Level(in.readByte() & 0xFF);
        fxInput.setSend1Pan(in.readByte() & 0xFF);
        fxInput.setSend1Width(in.readByte() & 0xFF);
        fxInput.setRfu3(in.readByte() & 0xFF);
        fxInput.setSend2Type(in.readByte() & 0xFF);
        fxInput.setSend2Assign(in.readByte() & 0xFF);
        fxInput.setSend2Level(in.readByte() & 0xFF);
        fxInput.setSend2Pan(in.readByte() & 0xFF);
        fxInput.setSend2Width(in.readByte() & 0xFF);
        fxInput.setRfu4(in.readByte() & 0xFF);
        fxInput.setRfu5(in.readByte() & 0xFF);
        fxInput.setRfu6(in.readSignedInt32());
        return fxInput;
    }

    private void serializeFXInput(BitStreamWriter out, FXInput fxInput) {
        out.writeByte((byte) fxInput.getVersion());
        out.writeByte((byte) fxInput.getBusID());
        out.writeByte((byte) fxInput.getMode());
        out.writeByte((byte) fxInput.getUseStereo());
        out.writeByte((byte) fxInput.getEqEnable());
        out.writeByte((byte) fxInput.getEq1Type());
        out.writeByte((byte) fxInput.getEq1Level());
        out.writeByte((byte) fxInput.getEq1Freq());
        out.writeByte((byte) fxInput.getEq1Q());
        out.writeByte((byte) fxInput.getRfu1());
        out.writeByte((byte) fxInput.getEq2Type());
        out.writeByte((byte) fxInput.getEq2Level());
        out.writeByte((byte) fxInput.getEq2Freq());
        out.writeByte((byte) fxInput.getEq2Q());
        out.writeByte((byte) fxInput.getRfu2());
        out.writeByte((byte) fxInput.getSend1Type());
        out.writeByte((byte) fxInput.getSend1Assign());
        out.writeByte((byte) fxInput.getSend1Level());
        out.writeByte((byte) fxInput.getSend1Pan());
        out.writeByte((byte) fxInput.getSend1Width());
        out.writeByte((byte) fxInput.getRfu3());
        out.writeByte((byte) fxInput.getSend2Type());
        out.writeByte((byte) fxInput.getSend2Assign());
        out.writeByte((byte) fxInput.getSend2Level());
        out.writeByte((byte) fxInput.getSend2Pan());
        out.writeByte((byte) fxInput.getSend2Width());
        out.writeByte((byte) fxInput.getRfu4());
        out.writeByte((byte) fxInput.getRfu5());
        out.writeInt32(fxInput.getRfu6());
    }

    private FXBus deserializeFXBus(BitStreamReader in) {
        FXBus fxBus = new FXBus();
        fxBus.setVersion(in.readByte() & 0xFF);
        fxBus.setBusID(in.readByte() & 0xFF);
        fxBus.setMultiEffect(in.readShort() & 0xFFFF);
        fxBus.setAlloc(in.readByte() & 0xFF);
        fxBus.setAuxLevel(in.readByte() & 0xFF);
        fxBus.setAuxBal(in.readByte() & 0xFF);
        fxBus.setMixLevel(in.readByte() & 0xFF);
        fxBus.setMixBal(in.readByte() & 0xFF);
        fxBus.setWetDry(in.readByte() & 0xFF);
        fxBus.setWetDry2(in.readByte() & 0xFF);
        fxBus.setAuxSend(in.readByte() & 0xFF);
        fxBus.setWetDrySel(in.readByte() & 0xFF);
        fxBus.setWetDrySel2(in.readByte() & 0xFF);
        fxBus.setMixSend(in.readByte() & 0xFF);
        fxBus.setRfu(in.readByte() & 0xFF);
        int[] rfu2 = new int[4];
        for (int i = 0; i < 4; i++) {
            rfu2[i] = in.readByte() & 0xFF;
        }
        fxBus.setRfu2(rfu2);
        return fxBus;
    }

    private void serializeFXBus(BitStreamWriter out, FXBus fxBus) {
        out.writeByte((byte) fxBus.getVersion());
        out.writeByte((byte) fxBus.getBusID());
        out.writeShort((short) fxBus.getMultiEffect());
        out.writeByte((byte) fxBus.getAlloc());
        out.writeByte((byte) fxBus.getAuxLevel());
        out.writeByte((byte) fxBus.getAuxBal());
        out.writeByte((byte) fxBus.getMixLevel());
        out.writeByte((byte) fxBus.getMixBal());
        out.writeByte((byte) fxBus.getWetDry());
        out.writeByte((byte) fxBus.getWetDry2());
        out.writeByte((byte) fxBus.getAuxSend());
        out.writeByte((byte) fxBus.getWetDrySel());
        out.writeByte((byte) fxBus.getWetDrySel2());
        out.writeByte((byte) fxBus.getMixSend());
        out.writeByte((byte) fxBus.getRfu());
        for (int rfuByte : fxBus.getRfu2()) {
            out.writeByte((byte) rfuByte);
        }
    }
}
