package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.soundblock;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.KObject;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.KFObject;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.RawData;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * <pre>
 * soundblock structure
 * typedef struct {
 *   word	base;   // base sfh ID
 *   word	nsfh;   // #of headers -1
 *   word  off;
 *   ubyte sflags;
 *   ubyte unused;
 *   word  copyID;
 *   word  rfu;
 *   SFH   headers[1];
 * } SBLK;
 * </pre>
 */
@Getter
@Setter
@JsonPropertyOrder({"type", "objectId", "name", "data", "base", "nsfh", "off", "sflags", "unused", "copyID", "rfu", "headers"})
public class KFSoundBlock extends KFObject {
    private int base;
    private int nsfh;
    private int off;
    private int sflags;
    private int unused;
    private int copyID;
    private int rfu;
    private List<KFSoundBlockHeader> headers;
    private List<KFSoundBlockEnvelope> envelopes; // Re-adding envelopes field if it was intended to be separate from SFH

    public KFSoundBlock(RawData data, String name, int objectId, List<KFSoundBlockHeader> headers, List<KFSoundBlockEnvelope> envelopes) {
        super(data, KObject.SOUND_BLOCK, name, objectId);
        this.headers = headers;
        this.envelopes = envelopes;
    }
}
