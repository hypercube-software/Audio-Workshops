package com.hypercube.mpm.model;

import lombok.*;

import java.util.Comparator;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class DeviceStateId implements Comparable<DeviceStateId> {
    private String name; // device name
    private String mode; // mode name
    private Integer channel; // device channel

    @Override
    public String toString() {
        return "[%s:%s:%d]".formatted(name, mode, channel);
    }

    @Override
    public int compareTo(DeviceStateId o) {
        return Comparator.comparing(DeviceStateId::getName)
                .thenComparing(DeviceStateId::getMode)
                .thenComparing(DeviceStateId::getChannel)
                .compare(this, o);
    }
}
