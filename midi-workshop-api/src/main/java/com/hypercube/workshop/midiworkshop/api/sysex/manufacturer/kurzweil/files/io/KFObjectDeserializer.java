package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io;

import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.KObject;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.*;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamReader;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class KFObjectDeserializer extends KFDeserializer {
    public static KFObject deserialize(RawData data) {
        BitStreamReader in = data.getBitStream();
        // Read type and id of the object
        // see official spec objtypes.h
        // some types use 10 bits ids whereas some other require only 8 bits ids
        final int objectTypeId;
        final int objectId;
        if (in.readBit() == 1) {
            // 10 bits object id
            objectTypeId = 0x80 | in.readBits(5);
            objectId = in.readBits(10);
        } else {
            // 8 bits object id
            objectTypeId = in.readBits(7);
            objectId = in.readBits(8);
        }
        KObject type = KObject.fromId(objectTypeId)
                .orElse(KObject.UNKNOWN);
        log.info("Read type {} id {}", type.name(), objectId);
        return switch (type) {
            case SOUND_BLOCK -> deserializeSoundBlock(data, in, objectId);
            case PROGRAM -> deserializeProgram(data, in, objectId);
            default -> new KFObject(data, type, objectId);
        };
    }

    private static KFProgram deserializeProgram(RawData data, BitStreamReader in, int objectId) {
        int size = in.readShort();
        int ofs = in.readShort();
        String name = readName(in, ofs);
        List<KFProgramSegment> segments = new ArrayList<>();
        for (; ; ) {
            int segmentTag = in.readByte();
            if (segmentTag == 0) {
                break;
            }
            ProgramSegmentType type = ProgramSegmentType.fromTag(segmentTag)
                    .orElse(null);
            int instanceId = segmentTag & (~type.getMask());
            segments.add(deserializeKFProgramSegment(in, type, instanceId));
        }

        return new KFProgram(data, objectId, segments);
    }

    private static KFProgramSegment deserializeKFProgramSegment(BitStreamReader in, ProgramSegmentType programSegmentType, int instanceId) {
        int segmentSize = programSegmentType.getSize() - 1; // tag already read
        log.info("{} (tag {}) size {} instanceId {}", programSegmentType, programSegmentType.getTag(), segmentSize, instanceId);
        byte[] content = new byte[segmentSize];
        RawData segmentContent = new RawData(content, in.getBitPos() / 8);
        for (int i = 0; i < segmentSize; i++) {
            content[i] = (byte) in.readByte();
        }
        return new KFProgramSegment(segmentContent, programSegmentType);
    }

    private static KFSoundBlock deserializeSoundBlock(RawData data, BitStreamReader in, int objectId) {
        int size = in.readShort();
        int ofs = in.readShort();
        String name = readName(in, ofs);
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
        return new KFSoundBlock(data, objectId, name, size, headers, envelopes);
    }


    private static String readName(BitStreamReader in, int ofs) {
        StringBuilder name = new StringBuilder();
        for (int i = 0; i < ofs - 2; i++) {
            int ch = in.readBits(8);
            if (ch != 0) {
                name.append((char) ch);
            }
        }
        return name.toString();
    }

    private static KFSoundBlockEnvelope deserializeSoundBlockEnv(BitStreamReader in) {
        int attackRate = in.readSignedShort();
        int attackLevel = in.readSignedShort();
        int decayRate = in.readSignedShort();
        int decayLevel = in.readSignedShort();
        int releaseRate = in.readSignedShort();
        int releaseLevel = in.readSignedShort();
        return new KFSoundBlockEnvelope();
    }

    private static KFSoundBlockHeader deserializeSoundBlockHeader(BitStreamReader in) {
        int rootk = in.readByte();     /* MIDI key # */
        int flags = in.readByte();
        int amp1 = in.readByte();    /* normal attack amp adjust ) */
        int amp2 = in.readByte();    /* alt attack amp adjust */
        int pitch = in.readShort();    /* pitch at highest playback rate*/
        int name = in.readShort();     /* offset to name if any, 0 if none */
        long sos = in.readLong();     /* normal start of span */
        long alt = in.readLong();     /* alt (legato)  start of span */
        long los = in.readLong();      /* loop of span */
        long eos = in.readLong();      /* end of span */
        int env1 = in.readShort();     /* normal expansion envelope */
        int env2 = in.readShort();     /* alt (legato) expansion env */
        long srate = in.readLong();    /* 1/sampling rate in nanosecs */
        return new KFSoundBlockHeader();
    }
}
