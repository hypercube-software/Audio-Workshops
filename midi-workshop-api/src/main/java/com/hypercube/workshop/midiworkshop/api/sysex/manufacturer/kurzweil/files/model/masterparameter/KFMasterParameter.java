package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.masterparameter;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.KObject;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.KFObject;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.RawData;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents a Kurzweil Master Parameter Block (mastb) object.
 * The structure is based on the `mastb` definition found in `mastb.h`.
 *
 * <pre><code>
 * typedef struct {
 * 	mdb	m;		// master params
 * 	zdb	z;	        // zone params
 * 	cdb	c[16];	// channel params (nCHANS is 16)
 * 	ubyte   sb[10][18];     // search buffers
 * } mastb;
 * </code></pre>
 */
@Getter
@Setter
@JsonPropertyOrder({"type", "objectId", "name", "data", "m", "z", "c", "sb"})
public class KFMasterParameter extends KFObject {
    /**
     * Master Params
     */
    private MDB m;
    /**
     * Zone params
     */
    private ZDB z;
    /**
     * channel params (nCHANS is 16)
     */
    private CDB[] c = new CDB[16];
    /**
     * search buffers (ubyte)
     */
    private int[][] sb = new int[10][18];

    public KFMasterParameter(RawData data, String name, int objectId) {
        super(data, KObject.MASTER_PARAMETER, name, objectId);
    }
}
