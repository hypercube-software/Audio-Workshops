package com.hypercube.workshop.midiworkshop.common.sysex.library.device;

import com.hypercube.workshop.midiworkshop.common.errors.MidiConfigError;
import com.hypercube.workshop.midiworkshop.common.sysex.macro.CommandCall;
import com.hypercube.workshop.midiworkshop.common.sysex.util.SysExTemplate;
import lombok.Getter;
import lombok.Setter;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.zip.CRC32;

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
     * Controller ID in hexadecimal or SYSEX command call (@link {@link CommandCall}
     */
    private final String identity;
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

    /**
     * Template used to generate the SYSEX message quickly
     */
    @Setter
    private SysExTemplate sysExTemplate;

    public MidiDeviceController(ControllerValueType type, String name, String identity, int minValue, int maxValue) {
        this.idBitDepth = identity.length() == 4 ? 14 : 7;
        this.type = type == ControllerValueType.CC && idBitDepth == 14 ? ControllerValueType.CC_MSB_LSB : type;
        this.name = name;
        this.identity = identity;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.sysExTemplate = null; // set later after deserialization
        if (type == ControllerValueType.SYSEX) {
            CRC32 crc = new CRC32();
            crc.update(identity.getBytes(StandardCharsets.UTF_8));
            this.id = (int) crc.getValue();
        } else {
            this.id = Integer.parseInt(identity, 16);
        }
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

    public int getMSB() {
        return (id >> 8) & 0x7F;
    }

    public int getLSB() {
        return (id >> 0) & 0x7F;
    }

    public boolean isSigned() {
        return minValue < 0;
    }

    public boolean isNRPN() {
        return switch (type) {
            case CC, CC_MSB_LSB, SYSEX -> false;
            case NRPN_MSB, NRPN_LSB, NRPN_MSB_LSB -> true;
        };
    }

    @Override
    public String toString() {
        if (type == ControllerValueType.SYSEX) {
            return "%s (0x%s %s %s)".formatted(name, sysExTemplate.midiRequest()
                    .getValue(), identity, type.name());
        } else {
            return "%s (0x%s %s)".formatted(name, identity, type.name());
        }
    }
}
