package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io.pressuremap;

import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io.KFDeserializer;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.RawData;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.pressuremap.KFPressureMap;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamReader;

public class KFPressureMapDeserializer extends KFDeserializer {

    public KFPressureMap deserialize(RawData rawData, int objectId, String name) {
        BitStreamReader in = rawData.bitStreamReader();
        int[] pmapPrss = new int[KFPressureMap.SIZE];
        for (int i = 0; i < KFPressureMap.SIZE; i++) {
            // Each element is a ubyte (1 byte). Read as unsigned int.
            pmapPrss[i] = in.readByte() & 0xFF; // readByte reads 1 byte, & 0xFF to treat as unsigned
        }
        return new KFPressureMap(rawData, objectId, name, pmapPrss);
    }
}
