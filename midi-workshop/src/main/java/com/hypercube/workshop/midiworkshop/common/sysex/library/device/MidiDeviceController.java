package com.hypercube.workshop.midiworkshop.common.sysex.library.device;

import com.hypercube.workshop.midiworkshop.common.errors.MidiConfigError;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
public final class MidiDeviceController {
    /**
     * Controller type (how the value is expressed)
     */
    private final ControllerValueType type;
    /**
     * Controller name
     */
    private final String name;
    /**
     * Controller ID in hexadecimal
     */
    private final String hexaId;
    /**
     * Controller ID
     */
    private final int id;
    /**
     * Size of the controller ID (7 or 14 bits)
     */
    private final int idBitDepth;
    /**
     * Minimum value of the controller
     */
    private final int minValue;
    /**
     * Maximum value of the controller
     */
    private final int maxValue;

    public MidiDeviceController(ControllerValueType type, String name, String hexaId, int minValue, int maxValue) {
        this.idBitDepth = hexaId.length() == 4 ? 14 : 7;
        this.type = type == ControllerValueType.CC && idBitDepth == 14 ? ControllerValueType.CC_MSB_LSB : type;
        this.name = name;
        this.hexaId = hexaId;
        this.id = Integer.parseInt(hexaId, 16);
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    public int getMSB() {
        return (id >> 8) & 0x7F;
    }

    public int getLSB() {
        return (id >> 0) & 0x7F;
    }

    public boolean isNRPN() {
        return switch (type) {
            case CC, CC_MSB_LSB -> false;
            case NRPN_MSB, NRPN_LSB, NRPN_MSB_LSB -> true;
        };
    }

    public static MidiDeviceController of(String definition) {
        List<String> parts = Arrays.stream(definition.split("\\|"))
                .toList();
        if (parts.size() != 5) {
            throw new MidiConfigError("Controller definition must be \"id | name | type | minValue | maxValue\":" + definition);
        }
        String hexaId = parts.get(0)
                .trim();
        String name = parts.get(1)
                .trim();
        ControllerValueType controllerValueType = ControllerValueType.valueOf(parts.get(2)
                .trim());
        int minValue = Integer.parseInt(parts.get(3)
                .trim());
        int maxValue = Integer.parseInt(parts.get(4)
                .trim());
        return new MidiDeviceController(controllerValueType, name, hexaId, minValue, maxValue);
    }

    @Override
    public String toString() {
        return "%s (0x%s %s)".formatted(name, hexaId, type.name());
    }
}
