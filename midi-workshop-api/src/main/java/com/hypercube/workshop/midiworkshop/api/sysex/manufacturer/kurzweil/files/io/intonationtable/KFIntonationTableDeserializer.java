package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io.intonationtable;

import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io.KFDeserializer;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.RawData;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.intonationtable.KFIntonationTable;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamReader;

public class KFIntonationTableDeserializer extends KFDeserializer {


    public KFIntonationTable deserialize(RawData rawData, int objectId, String name) {
        BitStreamReader in = rawData.bitStreamReader();
        int[] itblTbl = new int[KFIntonationTable.SIZE];
        for (int i = 0; i < KFIntonationTable.SIZE; i++) {
            // Each word is 2 bytes. Assume MSB first, then LSB.
            itblTbl[i] = in.readShort() & 0xFFFF; // readShort reads 2 bytes and combines them, & 0xFFFF to treat as unsigned
        }
        return new KFIntonationTable(rawData, objectId, name, itblTbl); // Need to adjust KFIntonationTable constructor
    }
}
