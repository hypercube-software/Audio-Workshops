package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.intonationtable;

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
@JsonPropertyOrder({"objectId", "name", "itblTbl"})
public class KFIntonationTable extends KFObject {

    public static final int SIZE = 12; // 12 words, each 2 bytes = 24 bytes

    private final int[] itblTbl;

    public KFIntonationTable(RawData rawData, int objectId, String name, int[] itblTbl) {
        super(rawData, KObject.INTONATION_TABLE, name, objectId);
        if (itblTbl.length != SIZE) {
            throw new IllegalArgumentException("itblTbl must have a size of " + SIZE);
        }
        this.itblTbl = Arrays.copyOf(itblTbl, SIZE);
    }

    public List<Integer> getDumpableMutableParameters() {
        // Intonation tables are typically fixed, not "mutable" in the sense of program parameters
        // For now, return empty list or consider if individual entries are modifiable directly.
        // Based on itbl.h, it's a fixed array of words.
        return List.of();
    }
}
