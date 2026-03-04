package com.hypercube.workshop.synthripper.preset.decent.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@XmlAccessorType(XmlAccessType.FIELD)
public class Midi {
    @XmlElement(name = "cc")
    private List<MidiControlChange> midiControlChangeList = new ArrayList<>();

    public boolean hasControlChange(Integer cc) {
        return midiControlChangeList.stream()
                .anyMatch(midiControlChangeList -> midiControlChangeList.getNumber() == cc);
    }
}
