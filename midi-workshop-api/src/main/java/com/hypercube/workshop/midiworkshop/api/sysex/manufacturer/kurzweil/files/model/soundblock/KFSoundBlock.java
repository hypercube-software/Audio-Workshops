package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.soundblock;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.KObject;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.KFObject;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.RawData;
import lombok.Getter;

import java.util.List;

@Getter
@JsonPropertyOrder({"type", "objectId", "name", "data", "headers", "envelopes"})
public class KFSoundBlock extends KFObject {
    private final List<KFSoundBlockHeader> headers;
    private final List<KFSoundBlockEnvelope> envelopes;

    public KFSoundBlock(RawData data, String name, int objectId, List<KFSoundBlockHeader> headers, List<KFSoundBlockEnvelope> envelopes) {
        super(data, KObject.SOUND_BLOCK, name, objectId);
        this.headers = headers;
        this.envelopes = envelopes;
    }
}
