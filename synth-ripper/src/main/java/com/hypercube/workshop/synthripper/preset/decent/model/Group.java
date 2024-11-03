package com.hypercube.workshop.synthripper.preset.decent.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@XmlAccessorType(XmlAccessType.FIELD)
public class Group {
    @XmlAttribute
    private int loVel;
    @XmlAttribute
    private int hiVel;
    @XmlAttribute(name = "release")
    private float releaseTimeInSec;
    @XmlElement(name = "sample")
    private List<Sample> samples = new ArrayList<>();
}
