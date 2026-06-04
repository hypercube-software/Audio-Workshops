package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.pressuremap;

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
@JsonPropertyOrder({"objectId", "name", "pmapPrss"})
public class KFPressureMap extends KFObject {

    public static final int SIZE = 8; // NDYNAM from pmap.h

    private final int[] pmapPrss;

    public KFPressureMap(RawData rawData, int objectId, String name, int[] pmapPrss) {
        super(rawData, KObject.PRESSURE_MAP, name, objectId);
        if (pmapPrss.length != SIZE) {
            throw new IllegalArgumentException("pmapPrss must have a size of " + SIZE);
        }
        this.pmapPrss = Arrays.copyOf(pmapPrss, SIZE);
    }

    public List<Integer> getDumpableMutableParameters() {
        // Pressure maps are typically fixed arrays of values.
        return List.of();
    }
}
