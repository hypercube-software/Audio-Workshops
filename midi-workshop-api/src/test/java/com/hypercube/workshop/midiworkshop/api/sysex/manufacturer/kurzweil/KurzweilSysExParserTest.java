package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil;

import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.Manufacturer;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class KurzweilSysExParserTest {
    private static Stream<Arguments> unpack() {
        return Stream.of(
                Arguments.of("K2600R/Dump Program 1 Stream.syx"),
                Arguments.of("K2600R/Dump Program 1 Nibble.syx"),
                Arguments.of("K2600R/All LCD Text.syx"),
                Arguments.of("K2600R/All LCD Pixels.syx")
        );
    }

    @ParameterizedTest
    @MethodSource
    void unpack(String file) throws Exception {
        //
        // GIVEN
        //
        KurzweilSysExParser kurzweilSysExParser = new KurzweilSysExParser();
        byte[] payload = KurzweilSysExParser.class.getResourceAsStream("/SysEx/Kurzweil/" + file)
                .readAllBytes();

        //
        // WHEN
        //
        kurzweilSysExParser.parse(Manufacturer.KURZWEIL, payload);
    }

    @Test
    @Disabled
    void parseProgram() throws Exception {
        //
        // GIVEN
        //
        KurzweilSysExParser kurzweilSysExParser = new KurzweilSysExParser();
        //String file = "/SysEx/Kurzweil/K2600R/READ_NIBBLE_200.syx";
        //String file = "D:\\github-checkout\\Audio-Workshops\\Manuals\\Kurzweil\\202 - Orch Pad 4 original NIBBLE.syx";
        String file = "src/test/resources/SysEx/Kurzweil/K2600R/199 - Default Program NIBBLE.syx";
        byte[] payload = Files.readAllBytes(Path.of(file));

        //
        // WHEN
        //
        kurzweilSysExParser.parse(Manufacturer.KURZWEIL, payload);
    }
}
