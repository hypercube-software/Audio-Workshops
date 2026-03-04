package com.hypercube.workshop.synthripper.preset.decent.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "DecentSampler")
@Getter
@Setter
@XmlAccessorType(XmlAccessType.FIELD)
public class DecentSampler {
    private List<Groups> groups = new ArrayList<>();
    private Midi midi = new Midi();
}
