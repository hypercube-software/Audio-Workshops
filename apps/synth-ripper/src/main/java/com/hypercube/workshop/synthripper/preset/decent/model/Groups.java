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
public class Groups {
    @XmlAttribute
    private String tags;
    @XmlElement(name = "group")
    List<RoundRobinGroup> roundRobinGroups = new ArrayList<>();
}
