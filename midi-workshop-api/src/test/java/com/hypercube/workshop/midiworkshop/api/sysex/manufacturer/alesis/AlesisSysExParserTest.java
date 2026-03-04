package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.alesis;

import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceDecodingKey;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceDefinition;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamReader;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AlesisSysExParserTest {
    private static Stream<Arguments> unpack() {
        return Stream.of(
                Arguments.of("QS6.1/Alesis Edit Buffer1.syx", "          "),
                Arguments.of("QS6.1/Alesis Edit Buffer2.syx", "!         "),
                Arguments.of("QS6.1/Alesis Edit Buffer3.syx", "!!        "),
                Arguments.of("QS6.1/Alesis Edit Buffer4.syx", "!!!       "),
                Arguments.of("QS6.1/Alesis Edit Buffer5.syx", "!!!!      "),
                Arguments.of("QS6.1/Alesis Edit Buffer6.syx", "!!!!!     "),
                Arguments.of("QS6.1/Alesis Edit Buffer7.syx", "!!!!!!    "),
                Arguments.of("QS6.1/Alesis Edit Buffer8.syx", "?!!!!!    "),
                Arguments.of("QS6.1/Alesis Edit Buffer9.syx", " ?        "),
                Arguments.of("QS6.1/Alesis Edit Buffer10.syx", "  ?       "),
                Arguments.of("QS6.1/Alesis Edit Buffer11.syx", "   ?      "),
                Arguments.of("QS6.1/Alesis Edit Buffer12.syx", "    ?     "),
                Arguments.of("QS6.1/Alesis Edit Buffer13.syx", "     ?    "),
                Arguments.of("QS6.1/Alesis Edit Buffer14.syx", "← ← ← ← ← "),
                Arguments.of("QS6.1/Alesis Edit Buffer15.syx", "→ → → → → "),
                Arguments.of("QS6.1/Alesis Edit Buffer16.syx", "_ _ _ _ _ "),
                Arguments.of("QS6.1/Alesis Edit Buffer17.syx", " _ _ _ _ _"),
                Arguments.of("QS6.1/Alesis Edit Buffer18.syx", "PureStereo"),
                Arguments.of("QS6.1/Alesis Edit Buffer19.syx", "Rave Knave"),
                Arguments.of("QS6.1/Alesis Edit Buffer20.syx", "AntiquePno"),
                Arguments.of("QS6.1/Alesis Edit Buffer21.syx", "4Draw Rock"),
                Arguments.of("QS4/mbquadra.syx", "MB Strings") //  QuadraSynth Patches created by Michael Bernard ( one of the original QS sounddevelopers )
        );
    }

    @ParameterizedTest
    @MethodSource
    void unpack(String file, String title) throws Exception {
        //
        // GIVEN
        //
        MidiDeviceDefinition device = new MidiDeviceDefinition();
        MidiDeviceDecodingKey key = new MidiDeviceDecodingKey(7, 1, """
                    0 A1 A2 A3 A4 A5 A6 A7
                    0 B2 B3 B4 B5 B6 B7 A0
                    0 C3 C4 C5 C6 C7 B0 B1
                    0 D4 D5 D6 D7 C0 C1 C2
                    0 E5 E6 E7 D0 D1 D2 D3
                    0 F6 F7 E0 E1 E2 E3 E4
                    0 G7 F0 F1 F2 F3 F4 F5
                    0 G0 G1 G2 G3 G4 G5 G6                
                """);
        device.setDecodingKey(key);
        AlesisSysExParser alesisSysExParser = new AlesisSysExParser();
        byte[] payload = AlesisSysExParser.class.getResourceAsStream("/SysEx/Alesis/" + file)
                .readAllBytes();
        // the SYSEX looks like this:
        // USER PROGRAM: F0 00 00 0E 0E 00 <edit#> <data> F7
        // PROGRAM     : F0 00 00 0E 0E 02 <edit#> <data> F7
        // MIX         : F0 00 00 0E 0E 0E <edit#> <data> F7
        // we extract "data" and decode it
        int payloadType = payload[5];
        if (payloadType != 0x02 && payloadType != 00) {
            throw new IllegalStateException("Unexpected payload payloadType for this test: " + payloadType);
        }
        String header1 = "AAAAAAAA";
        String header2 = "76543210";
        String expected = "00000000";
        int bit = 8;
        for (int i = 0; i < title.length(); i++) {
            int idx = alesisSysExParser.getCharCode(title.charAt(i));
            String code = BitStreamReader.getBinary7Inverted(idx);
            expected += " " + code;
            header1 += " ";
            header2 += " ";
            for (int j = 0; j < 7; j++) {
                char byteCode = (char) ('A' + ((bit % 56) / 8));
                header1 += byteCode;
                header2 += 7 - (bit % 8);
                bit++;
            }
        }
        //
        // WHEN
        //
        byte[] unpacked = alesisSysExParser.unpackMidiBuffer(device, payload);

        // THEN
        //assertEquals(350, unpacked.length);
        BitStreamReader bsr = new BitStreamReader(unpacked);
        String actual = getActualDecodedBits(bsr);
        String actualTitle = payloadType == 0x0E ? getActualTitle2(bsr, alesisSysExParser) : getActualTitle(bsr, alesisSysExParser);

        String errors = getActualBitsErrors(actual, expected);
        System.out.println("Title          : '" + title + "'");
        System.out.println("Actual Title   : '" + actualTitle + "'");
        System.out.println("          " + header1);
        System.out.println("          " + header2);
        System.out.println("ACTUAL  : " + actual);
        System.out.println("EXPECTED: " + expected);
        System.out.println("ERRORS  : " + errors);
        System.out.println("Rom identifier : " + bsr.readBits(2)); // not LSB first apparently
        System.out.println("Mode           : " + bsr.readInvertedBits(1));
        System.out.println("Sample Group   : " + bsr.readInvertedBits(6));
        System.out.println("Sample Number  : " + bsr.readInvertedBits(7));
        System.out.println("Sound Volume   : " + bsr.readInvertedBits(7));
        System.out.println("Sound Pan      : " + bsr.readInvertedBits(3));
        System.out.println("Sound Output   : " + bsr.readInvertedBits(2));
        System.out.println("Sound FX Level : " + bsr.readInvertedBits(7));
        System.out.println("Sound FX bus   : " + bsr.readInvertedBits(2));
        assertEquals(expected, actual);
    }


    private static String getActualBitsErrors(String actual, String expected) {
        String errors = "";
        for (int i = 0; i < actual.length(); i++) {
            if (actual.charAt(i) != expected.charAt(i)) {
                errors += "^";
            } else {
                errors += " ";
            }
        }
        return errors;
    }

    private String getActualTitle(BitStreamReader bsr, AlesisSysExParser alesisSysExParser) {
        bsr.reset();
        String actualTitle = "";
        bsr.readBits(8);
        for (int i = 0; i < 10; i++) {
            int c = bsr.readInvertedBits(7);
            actualTitle += alesisSysExParser.getChar(c);
        }
        return actualTitle;
    }

    private String getActualTitle2(BitStreamReader bsr, AlesisSysExParser alesisSysExParser) {
        bsr.reset();
        String actualTitle = "";
        bsr.readBits(5);
        for (int i = 0; i < 10; i++) {
            int c = bsr.readInvertedBits(7);
            actualTitle += alesisSysExParser.getChar(c);
        }
        return actualTitle;
    }

    private String getActualDecodedBits(BitStreamReader bsr) {
        bsr.reset();
        String actual = "";
        for (int i = 0; i < 8 + 7 * 10; i++) {
            if (i == 8 || (i > 8 && (i - 8) % 7 == 0)) {
                actual += " ";
            }
            actual += bsr.readBit();
        }
        return actual;
    }

    private static void dumpMIDIPayload(byte[] payload) {
        BitStreamReader pack = new BitStreamReader(payload);
        List<Integer> bits = new ArrayList<>();
        for (int i = 0; i < 8 * 8; i++) {
            int bit = pack.readBit();
            bits.addLast(bit);
        }
        var o = System.out;
        for (int i = 0; i < bits.size(); i++) {
            int bit = bits.get(i);
            if (i % 8 == 0) {
                o.print("\n[%d]".formatted(bit));
            } else {
                o.print(" %d".formatted(bit));
            }
        }
        o.println();
    }
}