package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model;

import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.KObject;
import lombok.Getter;

import java.util.List;

@Getter
public class KFSoundBlock extends KFObject {
    private final String name;
    private final int size;
    private final List<KFSoundBlockHeader> headers;
    private final List<KFSoundBlockEnvelope> envelopes;

    public KFSoundBlock(RawData data, int objectId, String name, int size, List<KFSoundBlockHeader> headers, List<KFSoundBlockEnvelope> envelopes) {
        super(data, KObject.SOUND_BLOCK, objectId);
        this.name = name;
        this.size = size;
        this.headers = headers;
        this.envelopes = envelopes;
    }
}
