package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.keymap;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.KObject;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.KFObject;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.RawData;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * <pre>
 * typedef	struct	{
 * 	word	block;		// sound block ID
 * 	word	method;		// method
 * 	word	pitch;		// base pitch
 * 	word	cents;		// cents/entry
 * 	word	nelem;		// # entries
 * 	word	esize;		// entry size
 * 	word	level[8];	// offset to timbre level arrays
 * } pitchMap;
 * </pre>
 */
@Getter
@Setter
@JsonPropertyOrder({"type", "objectId", "name", "data", "block", "method", "mask", "pitch", "cents", "nelem", "esize", "level", "entries"})
public class KFKeyMap extends KFObject {
    /**
     * sound block ID
     */
    private int block;
    /**
     * Method bits:
     * - 0x10: 2-byte tuning
     * - 0x08: 1-byte tuning
     * - 0x04: volume adjust
     * - 0x02: sample ID
     * - 0x01: subsample number
     */
    private int method;
    private Set<KeyMapMask> mask = new HashSet<>();
    private int pitch;
    private int cents;
    /**
     * Number of entries per level
     */
    private int nelem;
    private int esize;
    private int[] level = new int[8];
    private List<KeyMapEntry> entries = new ArrayList<>();

    public KFKeyMap(RawData data, String name, int objectId) {
        super(data, KObject.KEYMAP, name, objectId);
    }

    public void setMethod(int method) {
        this.method = method;
        updateMask();
    }

    public void updateMask() {
        mask.clear();
        for (KeyMapMask m : KeyMapMask.values()) {
            if ((method & m.getMask()) != 0) {
                mask.add(m);
            }
        }
    }
}
