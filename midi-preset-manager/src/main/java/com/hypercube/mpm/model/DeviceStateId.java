package com.hypercube.mpm.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class DeviceStateId {
    private String name; // device name
    private String mode; // mode name
    private Integer channel; // device channel

    @Override
    public String toString() {
        return "[%s:%s:%d]".formatted(name, mode, channel);
    }
}
