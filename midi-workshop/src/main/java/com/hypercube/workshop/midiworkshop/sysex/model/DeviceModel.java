package com.hypercube.workshop.midiworkshop.sysex.model;

import com.hypercube.workshop.midiworkshop.sysex.model.memory.DeviceMemory;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * {@link DeviceModel} cannot be enum like {@link Manufacturer} because it is mutable via {@link DeviceModel#memory}
 */

@RequiredArgsConstructor
@Getter
@EqualsAndHashCode
public abstract class DeviceModel {
    @EqualsAndHashCode.Include
    protected final Manufacturer manufacturer;
    @EqualsAndHashCode.Exclude
    protected final String name;
    @EqualsAndHashCode.Include
    protected final int code;

    @Setter
    @EqualsAndHashCode.Exclude
    protected DeviceMemory memory;

    @Override
    public String toString() {
        return "%s [0x%02X %s]".formatted(getClass().getSimpleName(), code, name);
    }
}
