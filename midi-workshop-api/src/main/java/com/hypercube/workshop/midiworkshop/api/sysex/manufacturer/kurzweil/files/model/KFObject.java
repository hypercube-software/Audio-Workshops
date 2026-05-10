package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.KObject;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.keymap.KFKeyMap;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.KFProgram;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.soundblock.KFSoundBlock;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,          // Use a logical name to identify the type
        include = JsonTypeInfo.As.PROPERTY,   // The type indicator is a property in the JSON
        property = "type",                    // The name of the property is "type"
        visible = true                        // Keeps the "type" property visible if you need to bind it to a Java field
)
@JsonSubTypes({
        // Map the "type" string value to the correct subclass
        @JsonSubTypes.Type(value = KFProgram.class, name = "PROGRAM"),
        @JsonSubTypes.Type(value = KFSoundBlock.class, name = "SOUND_BLOCK"),
        @JsonSubTypes.Type(value = KFKeyMap.class, name = "KEYMAP"),
})
@JsonPropertyOrder({"type", "objectId", "data"})
public abstract class KFObject {
    protected final RawData data;
    protected final KObject type;
    private final String name;
    protected int objectId;
}
