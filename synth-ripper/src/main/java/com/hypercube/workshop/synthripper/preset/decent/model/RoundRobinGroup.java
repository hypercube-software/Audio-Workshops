package com.hypercube.workshop.synthripper.preset.decent.model;

import jakarta.xml.bind.annotation.*;
import lombok.Getter;
import lombok.Setter;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@XmlAccessorType(XmlAccessType.FIELD)
public class RoundRobinGroup {
    @XmlAttribute
    private String name;
    @XmlElement(name = "sample")
    private List<Sample> samples = new ArrayList<>();
    @XmlAttribute(name = "release")
    private Float releaseTimeInSec;
    @XmlAttribute
    private Float modVolume; // undocumented field used by dave hilowitz
    @XmlAttribute
    private String tags;
    @XmlAttribute
    private Integer position;
    @XmlAnyAttribute
    private Map<QName, String> sampleControlChanges = new HashMap<>();

    public void addSampleControlChange(SampleControlChange sampleControlChange) {
        QName lowQName = new QName("lowCC%d".formatted(sampleControlChange.cc()));
        if (sampleControlChanges.containsKey(lowQName)) {
            throw new IllegalArgumentException("Duplicate Control Change entry detected: " + lowQName);
        }
        sampleControlChanges.put(lowQName, "%d".formatted(sampleControlChange.range()
                .low()));

        QName hiQName = new QName("hiCC%d".formatted(sampleControlChange.cc()));
        if (sampleControlChanges.containsKey(hiQName)) {
            throw new IllegalArgumentException("Duplicate Control Change entry detected: " + hiQName);
        }
        sampleControlChanges.put(hiQName, "%d".formatted(sampleControlChange.range()
                .high()));
    }
}
