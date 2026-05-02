package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io;

import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.RawData;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.keymap.KFKeyMap;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamReader;

import java.util.List;

public class KFKeyMapDeserializer extends KFDeserializer {

    public KFKeyMap deserialize(RawData data, int objectId) {
        BitStreamReader in = data.bitStreamReader();
        String name = readName(in);
        // TODO
        return new KFKeyMap(data, objectId, name, List.of());
    }
}
