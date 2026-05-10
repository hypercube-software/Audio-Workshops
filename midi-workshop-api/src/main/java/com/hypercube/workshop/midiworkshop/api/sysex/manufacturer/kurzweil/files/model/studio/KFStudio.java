package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.studio;

import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.KObject;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.KFObject;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.RawData;
import lombok.Getter;

@Getter
public class KFStudio extends KFObject {

    public KFStudio(RawData data, String name, int objectId) {
        super(data, KObject.STUDIO, name, objectId);
    }
}
