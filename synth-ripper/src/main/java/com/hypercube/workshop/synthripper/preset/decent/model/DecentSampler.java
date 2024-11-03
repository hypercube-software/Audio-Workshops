package com.hypercube.workshop.synthripper.preset.decent.model;

import jakarta.xml.bind.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "DecentSampler")
@Getter
@Setter
@XmlAccessorType(XmlAccessType.FIELD)
public class DecentSampler {
    @XmlElementWrapper(name = "groups")
    @XmlElement(name = "group")
    private List<Group> groups = new ArrayList<>();
}
