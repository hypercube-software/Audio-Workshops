package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.keymap;

import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.KObject;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.KFObject;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.RawData;
import lombok.Getter;

import java.util.List;

@Getter
public class KFKeyMap extends KFObject {
    private final String name;
    private final List<KeyMapEntry> entries;

    public KFKeyMap(RawData data, int objectId, String name, List<KeyMapEntry> entries) {
        super(data, KObject.KEYMAP, objectId);
        this.name = name;
        this.entries = entries;
    }
}
