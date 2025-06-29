package com.hypercube.mpm.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class Patch {
    private String device;
    private String mode;
    private String bank;
    private String name;
    private String category;
    private String command;
    private String filename;
    @EqualsAndHashCode.Exclude
    private Integer channel;
    @EqualsAndHashCode.Exclude
    private int score;

    @Override
    public String toString() {
        return name;
    }

    @JsonIgnore
    public DeviceStateId getDeviceStateId() {
        return new DeviceStateId(device, mode, channel);
    }
}
