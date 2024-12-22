package com.hypercube.workshop.synthripper.preset.decent.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@XmlAccessorType(XmlAccessType.FIELD)
@AllArgsConstructor
public class Binding {
    @XmlAttribute
    private String type;
    @XmlAttribute
    private String level;
    @XmlAttribute
    private Integer position;
    @XmlAttribute
    private String tags;
    @XmlAttribute
    private String parameter;
    @XmlAttribute
    private String translation;
    @XmlAttribute
    private String translationTable;
}
