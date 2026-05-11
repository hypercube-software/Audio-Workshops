package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io.sample;

import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io.KFDeserializer;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.RawData;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.soundblock.KFSoundBlock;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.soundblock.KFSoundBlockEnvelope;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.soundblock.KFSoundBlockHeader;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamReader;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamWriter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class KFSoundBlockDeserializer extends KFDeserializer {

    public KFSoundBlock deserialize(RawData data, int objectId) {
        BitStreamReader in = data.bitStreamReader();
        String name = readName(in);

        KFSoundBlock soundBlock = new KFSoundBlock(data, name, objectId, new ArrayList<>(), new ArrayList<>());

        soundBlock.setBase(in.readShort());
        int nsfh = in.readShort(); // nsfh is #of headers -1, so loop nsfh + 1 times
        soundBlock.setNsfh(nsfh);
        soundBlock.setOff(in.readShort());
        soundBlock.setSflags(in.readByte());
        soundBlock.setUnused(in.readByte()); // skip unused byte
        soundBlock.setCopyID(in.readShort());
        soundBlock.setRfu(in.readShort());

        KFSoundBlockHeaderDeserializer headerDeserializer = new KFSoundBlockHeaderDeserializer();
        List<KFSoundBlockHeader> headers = new ArrayList<>();
        for (int i = 0; i <= nsfh; i++) {
            log.info("Read KFSoundBlockHeader {}", i);
            headers.add(headerDeserializer.deserialize(in));
        }
        soundBlock.setHeaders(headers);

        // Deserialize envelopes - assuming they follow the headers directly
        int remain = data.size() - (in.getBitPos() / 8);
        int nbEnvelopes = remain / 12; // Assuming 12 bytes per envelope as per previous deserializeSoundBlockEnv
        List<KFSoundBlockEnvelope> envelopes = new ArrayList<>();
        // No dedicated deserializer for KFSoundBlockEnvelope yet, so I'll reuse the logic here.
        for (int i = 0; i < nbEnvelopes; i++) {
            envelopes.add(deserializeSoundBlockEnv(in));
        }
        soundBlock.setEnvelopes(envelopes);


        return soundBlock;
    }

    public void serialize(KFSoundBlock soundBlock, BitStreamWriter out) {
        writeName(soundBlock.getName(), out);

        out.writeShort(soundBlock.getBase());
        out.writeShort(soundBlock.getNsfh());
        out.writeShort(soundBlock.getOff());
        out.writeByte(soundBlock.getSflags());
        out.writeByte(soundBlock.getUnused()); // unused byte
        out.writeShort(soundBlock.getCopyID());
        out.writeShort(soundBlock.getRfu());

        KFSoundBlockHeaderDeserializer headerDeserializer = new KFSoundBlockHeaderDeserializer();
        for (KFSoundBlockHeader header : soundBlock.getHeaders()) {
            headerDeserializer.serialize(header, out);
        }

        // Serialize envelopes
        for (KFSoundBlockEnvelope env : soundBlock.getEnvelopes()) {
            serializeSoundBlockEnv(env, out);
        }
    }

    // Re-adding envelope deserialization logic
    private KFSoundBlockEnvelope deserializeSoundBlockEnv(BitStreamReader in) {
        KFSoundBlockEnvelope env = new KFSoundBlockEnvelope();
        env.setAttackRate(in.readSignedShort());
        env.setAttackLevel(in.readSignedShort());
        env.setDecayRate(in.readSignedShort());
        env.setDecayLevel(in.readSignedShort());
        env.setReleaseRate(in.readSignedShort());
        env.setReleaseLevel(in.readSignedShort());
        return env;
    }

    // Re-adding envelope serialization logic
    private void serializeSoundBlockEnv(KFSoundBlockEnvelope env, BitStreamWriter out) {
        out.writeShort(env.getAttackRate());
        out.writeShort(env.getAttackLevel());
        out.writeShort(env.getDecayRate());
        out.writeShort(env.getDecayLevel());
        out.writeShort(env.getReleaseRate());
        out.writeShort(env.getReleaseLevel());
    }
}
