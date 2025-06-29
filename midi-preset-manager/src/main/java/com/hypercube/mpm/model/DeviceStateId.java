package com.hypercube.mpm.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class DeviceStateId {
    private String name;
    private String mode;
    private Integer channel;

    @Override
    public String toString() {
        return "[%s:%s:%d]".formatted(name, mode, channel);
    }
}
