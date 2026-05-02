package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io;

import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.RawData;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.sample.KFSoundBlock;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.sample.KFSoundBlockEnvelope;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.sample.KFSoundBlockHeader;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamReader;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class KFSoundBlockDeserializer extends KFDeserializer {
    public KFSoundBlock deserialize(RawData data, int objectId) {
        BitStreamReader in = data.bitStreamReader();
        String name = readName(in);
        // See official spec sblock.h, struct SBLK
        int base = in.readShort();   /* base sfh ID */
        int nsfh = in.readShort() + 1;   /* #of headers -1 */
        int off = in.readShort();
        int sflags = in.readByte();
        in.skipBytes(1);
        int copyID = in.readShort();
        int rfu = in.readShort();
        List<KFSoundBlockHeader> headers = new ArrayList<>();
        for (int i = 0; i < nsfh; i++) {
            log.info("Read SFH {}", i);
            headers.add(deserializeSoundBlockHeader(in));
        }
        int remain = data.size() - (in.getBitPos() / 8);
        int nbEnvelopes = remain / 12;
        List<KFSoundBlockEnvelope> envelopes = new ArrayList<>();
        for (int i = 0; i < nbEnvelopes; i++) {
            envelopes.add(deserializeSoundBlockEnv(in));
        }
        return new KFSoundBlock(data, objectId, name, headers, envelopes);
    }

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

    private KFSoundBlockHeader deserializeSoundBlockHeader(BitStreamReader in) {
        KFSoundBlockHeader header = new KFSoundBlockHeader();
        header.setRootk(in.readByte());     /* MIDI key # */
        header.setFlags(in.readByte());
        header.setAmp1(in.readByte());    /* normal attack amp adjust ) */
        header.setAmp2(in.readByte());    /* alt attack amp adjust */
        header.setPitch(in.readShort());    /* pitch at highest playback rate*/
        header.setNameOffset(in.readShort());     /* offset to name if any, 0 if none */
        header.setSos(in.readLong());     /* normal start of span */
        header.setAlt(in.readLong());     /* alt (legato)  start of span */
        header.setLos(in.readLong());      /* loop of span */
        header.setEos(in.readLong());      /* end of span */
        header.setEnv1(in.readShort());     /* normal expansion envelope */
        header.setEnv2(in.readShort());     /* alt (legato) expansion env */
        header.setSrate(in.readLong());    /* 1/sampling rate in nanosecs */
        return header;
    }
}
