package com.hypercube.workshop.midiworkshop.sysex.parser.roland;

import com.hypercube.workshop.midiworkshop.common.errors.MidiError;
import com.hypercube.workshop.midiworkshop.sysex.model.DeviceModel;
import com.hypercube.workshop.midiworkshop.sysex.model.Manufacturer;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("java:S2160")
public final class RolandDeviceModel extends DeviceModel {
    private static final Map<Integer, RolandDeviceModel> commands = Stream.of(
                    new RolandDeviceModel("RSP-550", 0x38),
                    new RolandDeviceModel("D-70", 0x39)
            )
            .collect(Collectors.toMap(RolandDeviceModel::getCode, Function.identity()));

    public RolandDeviceModel(String name, int code) {
        super(Manufacturer.ROLAND, name, code);
    }

    public static RolandDeviceModel get(int code) {
        return Optional.ofNullable(commands.get(code))
                .orElseThrow(() -> new MidiError("Unknown device 0x%02X".formatted(code)));
    }

    public static Optional<RolandDeviceModel> get(String name) {
        return commands.values()
                .stream()
                .filter(m -> m.getName()
                        .equals(name))
                .findFirst();
    }
}
