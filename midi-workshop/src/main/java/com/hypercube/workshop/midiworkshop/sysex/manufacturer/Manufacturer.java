package com.hypercube.workshop.midiworkshop.sysex.manufacturer;

import com.hypercube.workshop.midiworkshop.common.errors.MidiError;
import com.hypercube.workshop.midiworkshop.sysex.device.Device;
import com.hypercube.workshop.midiworkshop.sysex.device.Devices;
import com.hypercube.workshop.midiworkshop.sysex.manufacturer.behringer.BehringerDevice;
import com.hypercube.workshop.midiworkshop.sysex.manufacturer.behringer.BehringerSysExParser;
import com.hypercube.workshop.midiworkshop.sysex.manufacturer.roland.RolandDevice;
import com.hypercube.workshop.midiworkshop.sysex.manufacturer.roland.RolandSysExParser;
import com.hypercube.workshop.midiworkshop.sysex.parser.SysExParser;
import com.hypercube.workshop.midiworkshop.sysex.util.SysExReader;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.hypercube.workshop.midiworkshop.sysex.util.SysExConstants.ROLAND_SOUND_CANVAS_DEVICE_MULTI;
import static com.hypercube.workshop.midiworkshop.sysex.util.SysExConstants.ROLAND_SOUND_CANVAS_DEVICE_SINGLE;

/**
 * MIDI Manufacturer list with their Devices
 */
@Getter
public enum Manufacturer {
    SEQUENTIAL_CIRCUITS("Sequential Circuits", 1),
    BIG_BRIAR("Big Briar", 2),
    OCTAVE_PLATEAU("Octave / Plateau", 3),
    MOOG("Moog", 4),
    PASSPORT_DESIGNS("Passport Designs", 5),
    LEXICON("Lexicon", 6),
    KURZWEIL("Kurzweil", 7),
    FENDER("Fender", 8),
    GULBRANSEN("Gulbransen", 9),
    DELTA_LABS("Delta Labs", 0x0A),
    SOUND_COMP("Sound Comp.", 0x0B),
    GENERAL_ELECTRO("General Electro", 0x0C),
    TECHMAR("Techmar", 0x0D),
    MATTHEWS_RESEARCH("Matthews Research", 0x0E),
    OBERHEIM("Oberheim", 0x10),
    PAIA("PAIA", 0x11),
    SIMMONS("Simmons", 0x12),
    DIGIDESIGN("DigiDesign", 0x13),
    FAIRLIGHT("Fairlight", 0x14),
    PEAVEY("Peavey", 0x1B),
    JL_COOPER("JL Cooper", 0x15),
    LOWERY("Lowery", 0x16),
    LIN("Lin", 0x17),
    EMU("Emu", 0x18),
    BON_TEMPI("Bon Tempi", 0x20),
    SIEL("S.I.E.L.", 0x21),
    SYNTHEAXE("SyntheAxe", 0x23),
    HOHNER("Hohner", 0x24),
    CRUMAR("Crumar", 0x25),
    SOLTON("Solton", 0x26),
    JELLINGHAUS("Jellinghaus Ms", 0x27),
    CTX("CTS", 0x28),
    PPG("PPG", 0x29),
    ELKA("Elka", 0x2F),
    CHEETAH("Cheetah", 0x36),
    WALDORF("Waldorf", 0x3E),
    KAWAI("Kawai", 0x40),
    ROLAND("Roland", 0x41, new RolandSysExParser(), RolandDevice.class,
            List.of(
                    new DeviceDefinition("RSP-550", 0x38),
                    new DeviceDefinition("DS-330", ROLAND_SOUND_CANVAS_DEVICE_MULTI),
                    new DeviceDefinition("SC-33", ROLAND_SOUND_CANVAS_DEVICE_MULTI),
                    new DeviceDefinition("SC-55", ROLAND_SOUND_CANVAS_DEVICE_MULTI),
                    new DeviceDefinition("SC-88", ROLAND_SOUND_CANVAS_DEVICE_MULTI),
                    new DeviceDefinition("DS-330 (Single Mode)", ROLAND_SOUND_CANVAS_DEVICE_SINGLE),
                    new DeviceDefinition("SC-33 (Single Mode)", ROLAND_SOUND_CANVAS_DEVICE_SINGLE),
                    new DeviceDefinition("SC-55 (Single Mode)", ROLAND_SOUND_CANVAS_DEVICE_SINGLE),
                    new DeviceDefinition("SC-88 (Single Mode)", ROLAND_SOUND_CANVAS_DEVICE_SINGLE),
                    new DeviceDefinition("D-70", 0x39),
                    new DeviceDefinition("JV-1010", 0x6A))),
    KORG("Korg", 0x42),
    YAMAHA("Yamaha", 0x43),
    CASIO("Casio", 0x44),
    AKAI("Akai", 0x47),
    M_AUDIO("M-Audio", 0x2008),
    BEHRINGER("Behringer", 0x2032, new BehringerSysExParser(), BehringerDevice.class, List.of(
            new DeviceDefinition("Neutron", 0x28)));

    private record DeviceDefinition(String name, int code) {
    }

    ;

    private <T extends Device> Manufacturer(String title, int code, SysExParser parser, Class<T> deviceClass, List<DeviceDefinition> devices) {
        this.title = title;
        this.code = code;
        this.devices = new Devices<>(
                devices.stream()
                        .map(d -> {
                            try {
                                return deviceClass.getConstructor(Manufacturer.class, String.class, int.class)
                                        .newInstance(this, d.name, d.code);
                            } catch (Exception e) {
                                throw new MidiError(e);
                            }
                        })
                        .toList());
        this.parser = Optional.ofNullable(parser)
                .orElseGet(SysExParser::new);
    }

    private Manufacturer(String title, int code) {
        this.title = title;
        this.code = code;
        this.devices = null;
        this.parser = new SysExParser();
    }

    private final String title;
    private final int code;
    private final Devices<? extends Device> devices;
    private final SysExParser parser;

    private static final Map<Integer, Manufacturer> manufacturers = Arrays.stream(Manufacturer.values())
            .collect(Collectors.toMap(Manufacturer::getCode, Function.identity()));

    /**
     * Return a Manufacturer given its code (found in a SysEx)
     *
     * @param code Manufacturer code
     * @return the Manufacturer or throw an exception MidiError if not found
     */

    public static Manufacturer get(int code) {
        return Optional.ofNullable(manufacturers.get(code))
                .orElseThrow(() -> new MidiError("Unknown Manufacturer 0x%02X".formatted(code)));
    }

    /**
     * Return a Manufacturer given its code (found in a SysEx)
     *
     * @param code Manufacturer code
     * @return An empty Optional if not found
     */
    public static Optional<Manufacturer> search(int code) {
        return Optional.ofNullable(manufacturers.get(code));
    }

    /**
     * Parse a SysEx dedicated to a specific Manufacturer
     *
     * @param sysExReader Input SysEx
     * @return A device with its virtual memory properly set
     */
    public Device parse(SysExReader sysExReader) {
        return Optional.ofNullable(parser)
                .map(p -> p.parse(this, sysExReader))
                .orElse(null);
    }

    /**
     * Retreive a device given its name
     *
     * @param name Name of a manufacturer device
     * @param <T>  Expected device type
     * @return A Manufacturer Device
     */
    @SuppressWarnings("unchecked")
    public <T extends Device> T getDevice(String name) {
        return (T) Optional.ofNullable(devices)
                .map(d -> d.get(name))
                .orElse(null);
    }

    /**
     * Retreive a device given its code
     *
     * @param code Code of a manufacturer device
     * @param <T>  Expected device type
     * @return A Manufacturer Device
     */
    @SuppressWarnings("unchecked")
    public <T extends Device> T getDevice(int code) {
        return (T) Optional.ofNullable(devices)
                .map(d -> d.get(code))
                .orElse(null);
    }


}
