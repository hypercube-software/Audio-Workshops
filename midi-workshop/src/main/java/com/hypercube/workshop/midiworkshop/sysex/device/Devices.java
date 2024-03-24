package com.hypercube.workshop.midiworkshop.sysex.device;

import com.hypercube.workshop.midiworkshop.common.errors.MidiError;
import com.hypercube.workshop.midiworkshop.sysex.manufacturer.Manufacturer;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Class used as a container of {@link Manufacturer} devices
 *
 * @param <T> class implementing the device
 */
public class Devices<T extends Device> {
    private final Map<Integer, T> devices;

    public Devices(List<T> devices) {
        this.devices = devices.stream()
                .collect(Collectors.toMap(T::getCode, Function.identity()));
    }

    public T get(int code) {
        return Optional.ofNullable(devices.get(code))
                .orElseThrow(() -> new MidiError("Unknown device 0x%02X".formatted(code)));
    }

    public T get(String name) {
        return devices.values()
                .stream()
                .filter(m -> m.getName()
                        .equals(name))
                .findFirst()
                .orElseThrow(() -> new MidiError("Unknown device " + name));
    }
}
