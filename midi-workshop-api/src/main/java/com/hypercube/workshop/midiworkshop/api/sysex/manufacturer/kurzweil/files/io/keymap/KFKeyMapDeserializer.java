package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io.keymap;

import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io.KFDeserializer;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.RawData;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.keymap.KFKeyMap;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.keymap.KeyMapEntry;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.keymap.KeyMapMask;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamReader;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamWriter;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class KFKeyMapDeserializer extends KFDeserializer {

    public KFKeyMap deserialize(RawData data, int objectId) {
        BitStreamReader in = data.bitStreamReader();
        String name = readName(in);

        KFKeyMap keyMap = new KFKeyMap(data, name, objectId);

        keyMap.setBlock(in.readShort());
        keyMap.setMethod(in.readShort());
        keyMap.setPitch(in.readShort());
        keyMap.setCents(in.readShort());
        keyMap.setNelem(in.readShort());
        keyMap.setEsize(in.readShort());

        int[] levels = new int[8];
        for (int i = 0; i < 8; i++) {
            levels[i] = in.readShort();
        }
        keyMap.setLevel(levels);
        int nbEntries = 1 + keyMap.getNelem();
        Set<KeyMapMask> mask = keyMap.getMask();
        // entries are compressed as much as possible, they can contain various optional fields
        log.info("Entries fields {}", mask.stream()
                .map(KeyMapMask::name)
                .collect(Collectors.joining(",")));
        for (int i = 0; i < nbEntries; i++) {
            KeyMapEntry entry = new KeyMapEntry();
            if (mask.contains(KeyMapMask.TUNING_SHORT)) {
                entry.setTuning(in.readShort());
            } else if (mask.contains(KeyMapMask.TUNING_BYTE)) {
                entry.setTuning(in.readByte());
            }
            if (mask.contains(KeyMapMask.VOLUME_ATTEN)) {
                entry.setAtten(in.readByte());
            }
            if (mask.contains(KeyMapMask.SAMPLE_ID)) {
                entry.setSblk(in.readShort());
            }
            if (mask.contains(KeyMapMask.SAMPLE_ROOT)) {
                entry.setRoot(in.readByte());
            }
            keyMap.getEntries()
                    .add(entry);
            log.info("entry {} tuning:{} sampleId: {} root: {}, atten: {}, end at pos {}", i,
                    entry.getTuning(),
                    entry.getSblk(),
                    entry.getRoot(),
                    entry.getAtten(),
                    "%X".formatted(in.getBytePos()));
        }
        return keyMap;
    }

    public void serialize(KFKeyMap keyMap, BitStreamWriter out) {
        writeName(keyMap.getName(), out);
        out.writeShort(keyMap.getBlock());
        out.writeShort(keyMap.getMethod());
        out.writeShort(keyMap.getPitch());
        out.writeShort(keyMap.getCents());
        out.writeShort(keyMap.getNelem());
        out.writeShort(keyMap.getEsize());

        for (int l : keyMap.getLevel()) {
            out.writeShort(l);
        }
    }
}
