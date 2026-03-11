package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil;

import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.Manufacturer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

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
}
