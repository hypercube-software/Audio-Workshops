package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io;

import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.KObject;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.KFObject;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.RawData;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamReader;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class KFObjectDeserializer extends KFDeserializer {
    private KFProgramDeserializer kfProgramDeserializer = new KFProgramDeserializer();
    private KFSoundBlockDeserializer kfSoundBlockDeserializer = new KFSoundBlockDeserializer();
    private KFKeyMapDeserializer kfKeyMapDeserializer = new KFKeyMapDeserializer();

    public List<KFObject> deserializeObjects(RawData data) {
        List<KFObject> objects = new ArrayList<>();
        BitStreamReader in = data.bitStreamReader();
        for (; ; ) {
            final long position = data.position() + in.getBytePos();
            final int objectTypeId;
            final int objectId;
            int inputId = in.readBits(16);
            if (inputId == 0) {
                break;
            }
            int HighestBit = inputId & 0x8000;
            if (HighestBit != 0) {
                // 10 bits object id
                objectTypeId = (HighestBit >>> 8) | ((inputId & 0b0111110000000000) >>> 10);
                objectId = inputId & 0b0000001111111111;
            } else {
                // 8 bits object id
                objectTypeId = inputId >>> 8;
                objectId = inputId & 0xFF;
            }
            KObject type = KObject.fromId(objectTypeId)
                    .orElse(KObject.UNKNOWN);
            int size = in.readShort() - 4; // size include what we already read
            log.info("Read type {} of size {} id {} encoded as {} at position {}",
                    type.name(),
                    Integer.toHexString(size),
                    objectId,
                    Integer.toHexString(inputId),
                    Long.toHexString(position)
            );
            RawData objectContent = data.readChildBlock(size);
            objects.add(switch (type) {
                case PROGRAM -> kfProgramDeserializer.deserialize(objectContent, objectId);
                case SOUND_BLOCK -> kfSoundBlockDeserializer.deserialize(objectContent, objectId);
                case KEYMAP -> kfKeyMapDeserializer.deserialize(objectContent, objectId);
                default -> new KFObject(objectContent, type, objectId);
            });
        }
        return objects;
    }


}
