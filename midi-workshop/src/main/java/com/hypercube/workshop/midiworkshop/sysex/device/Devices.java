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
    private final Map<String, T> devices;

    public Devices(List<T> devices) {
        this.devices = devices.stream()
                .collect(Collectors.toMap(T::getName, Function.identity()));
    }

    public T get(int code) {
        // Roland Sound Canvas share the same code, so system property FORCE_DEVICE must be used
        String forceDeviceName = System.getProperty("FORCE_DEVICE");
        List<T> matches = devices.values()
                .stream()
                .filter(d -> (forceDeviceName != null) ? d.getName()
                        .equals(forceDeviceName) : (d.getCode() == code))
                .toList();
        if (matches.size() == 1) {
            return matches.getFirst();
        } else if (matches.isEmpty()) {
            throw new MidiError("Unknown device 0x%02X".formatted(code));
        } else {
            throw new MidiError("Multiple devices share the same code 0x%02X".formatted(code));
        }
    }

    public T get(String name) {
        return Optional.ofNullable(devices.get(name))
                .orElseThrow(() -> new MidiError("Unknown device " + name));
    }
}
