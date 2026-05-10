package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io.studio;

import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io.KFDeserializer;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.RawData;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.studio.KFStudio;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamReader;

public class KFStudioDeserializer extends KFDeserializer {

    public KFStudio deserialize(RawData data, int objectId) {
        BitStreamReader in = data.bitStreamReader();
        String name = readName(in);
        // TODO
        return new KFStudio(data, name, objectId);
    }
}
