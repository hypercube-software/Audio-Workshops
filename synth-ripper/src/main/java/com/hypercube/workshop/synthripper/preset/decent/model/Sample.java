package com.hypercube.workshop.synthripper.preset.decent.model;

import com.hypercube.workshop.synthripper.preset.decent.jaxb.TriggerModeAdapter;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@XmlAccessorType(XmlAccessType.FIELD)
public class Sample {
    @XmlAttribute
    @XmlJavaTypeAdapter(TriggerModeAdapter.class)
    private TriggerMode trigger;
    @XmlAttribute
    private Long loopStart;
    @XmlAttribute
    private Long loopEnd;
    @XmlAttribute
    private Integer start;
    @XmlAttribute
    private Long end;
    @XmlAttribute
    private Boolean loopEnabled;
    @XmlAttribute
    private Long loopCrossfade;
    @XmlAttribute
    private String path;
    @XmlAttribute
    private Integer rootNote;
    @XmlAttribute
    private Integer hiNote;
    @XmlAttribute(name = "loNote")
    private Integer lowNote;
    @XmlAttribute
    private Integer loVel;
    @XmlAttribute
    private Integer hiVel;
    @XmlAttribute
    private String tags;
}
