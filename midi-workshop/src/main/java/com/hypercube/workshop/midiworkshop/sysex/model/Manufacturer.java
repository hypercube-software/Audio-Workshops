package com.hypercube.workshop.midiworkshop.sysex.model;

import com.hypercube.workshop.midiworkshop.common.errors.MidiError;
import com.hypercube.workshop.midiworkshop.sysex.parser.SysExParser;
import com.hypercube.workshop.midiworkshop.sysex.parser.roland.RolandSysExParser;
import com.hypercube.workshop.midiworkshop.sysex.util.CustomByteBuffer;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public enum Manufacturer {
    SEQUENTIAL_CIRCUITS("Sequential Circuits", 1, null),
    BIG_BRIAR("Big Briar", 2, null),
    OCTAVE_PLATEAU("Octave / Plateau", 3, null),
    MOOG("Moog", 4, null),
    PASSPORT_DESIGNS("Passport Designs", 5, null),
    LEXICON("Lexicon", 6, null),
    KURZWEIL("Kurzweil", 7, null),
    FENDER("Fender", 8, null),
    GULBRANSEN("Gulbransen", 9, null),
    DELTA_LABS("Delta Labs", 0x0A, null),
    SOUND_COMP("Sound Comp.", 0x0B, null),
    GENERAL_ELECTRO("General Electro", 0x0C, null),
    TECHMAR("Techmar", 0x0D, null),
    MATTHEWS_RESEARCH("Matthews Research", 0x0E, null),
    OBERHEIM("Oberheim", 0x10, null),
    PAIA("PAIA", 0x11, null),
    SIMMONS("Simmons", 0x12, null),
    DIGIDESIGN("DigiDesign", 0x13, null),
    FAIRLIGHT("Fairlight", 0x14, null),
    PEAVEY("Peavey", 0x1B, null),
    JL_COOPER("JL Cooper", 0x15, null),
    LOWERY("Lowery", 0x16, null),
    LIN("Lin", 0x17, null),
    EMU("Emu", 0x18, null),
    BON_TEMPI("Bon Tempi", 0x20, null),
    SIEL("S.I.E.L.", 0x21, null),
    SYNTHEAXE("SyntheAxe", 0x23, null),
    HOHNER("Hohner", 0x24, null),
    CRUMAR("Crumar", 0x25, null),
    SOLTON("Solton", 0x26, null),
    JELLINGHAUS("Jellinghaus Ms", 0x27, null),
    CTX("CTS", 0x28, null),
    PPG("PPG", 0x29, null),
    ELKA("Elka", 0x2F, null),
    CHEETAH("Cheetah", 0x36, null),
    WALDORF("Waldorf", 0x3E, null),
    KAWAI("Kawai", 0x40, null),
    ROLAND("Roland", 0x41, new RolandSysExParser()),
    KORG("Korg", 0x42, null),
    YAMAHA("Yamaha", 0x43, null),
    CASIO("Casio", 0x44, null),
    AKAI("Akai", 0x45, null);

    private final String name;
    private final int code;
    private final SysExParser parser;

    private static final Map<Integer, Manufacturer> manufacturers = Arrays.stream(Manufacturer.values())
            .collect(Collectors.toMap(Manufacturer::getCode, Function.identity()));

    public static Manufacturer get(int code) {
        return Optional.ofNullable(manufacturers.get(code))
                .orElseThrow(() -> new MidiError("Unknown Manufacturer 0x%02X".formatted(code)));
    }

    public DeviceModel parse(CustomByteBuffer bb) {
        return parser.parse(this, bb);
    }
}
