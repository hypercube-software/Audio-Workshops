package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.velocitymap;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.KFObject;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.RawData;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.KObject;
import lombok.Getter;
import lombok.ToString;

import java.util.Arrays;
import java.util.List;

@Getter
@ToString(callSuper = true)
@JsonPropertyOrder({"objectId", "name", "vmapVels"})
public class KFVelocityMap extends KFObject {

    public static final int SIZE = 8; // NDYNAM from vmap.h

    private final int[] vmapVels;

    public KFVelocityMap(RawData rawData, int objectId, String name, int[] vmapVels) {
        super(rawData, KObject.VELOCITY_MAP, name, objectId);
        if (vmapVels.length != SIZE) {
            throw new IllegalArgumentException("vmapVels must have a size of " + SIZE);
        }
        this.vmapVels = Arrays.copyOf(vmapVels, SIZE);
    }

    public List<Integer> getDumpableMutableParameters() {
        // Velocity maps are typically fixed arrays of values.
        return List.of();
    }
}
