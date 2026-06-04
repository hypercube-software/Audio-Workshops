package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io.velocitymap;

import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io.KFDeserializer;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.RawData;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.velocitymap.KFVelocityMap;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamReader;

public class KFVelocityMapDeserializer extends KFDeserializer {

    public KFVelocityMap deserialize(RawData rawData, int objectId, String name) {
        BitStreamReader in = rawData.bitStreamReader();
        int[] vmapVels = new int[KFVelocityMap.SIZE];
        for (int i = 0; i < KFVelocityMap.SIZE; i++) {
            // Each element is a ubyte (1 byte). Read as unsigned int.
            vmapVels[i] = in.readByte() & 0xFF; // readByte reads 1 byte, & 0xFF to treat as unsigned
        }
        return new KFVelocityMap(rawData, objectId, name, vmapVels);
    }
}
