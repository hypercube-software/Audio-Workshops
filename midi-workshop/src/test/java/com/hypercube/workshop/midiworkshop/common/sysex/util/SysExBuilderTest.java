package com.hypercube.workshop.midiworkshop.common.sysex.util;

import com.hypercube.workshop.midiworkshop.common.errors.MidiError;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import javax.sound.midi.InvalidMidiDataException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SysExBuilderTest {

    @ParameterizedTest
    @CsvSource({
            "F0 43 20 7A 'LM  0065DR' 00 00 00 00 00 00 00 00 00 00 00 00 00 00 7F 00 F7,0xF043207A4C4D202030303635445200000000000000000000000000007F00F7",
            "F0 41 00 42 11 480000 001D10 CK6 F7,0xF041004211480000001D100BF7",
            "F0 41 00 42 11 480000 001D10 CK3 F7,0xF041004211480000001D1053F7",
            "F041004211480000001D10F7,0xF041004211480000001D10F7"
    })
    void parse(String input, String expected) throws InvalidMidiDataException {
        var actual = SysExBuilder.parse(input);
        assertEquals(expected, actual.get(0)
                .getHexValues());
    }

    @Test
    void parseRange() throws InvalidMidiDataException {
        var actual = SysExBuilder.parse("F0 43 20 7A 'LM  0066SY' 0000000000000000000000000000 00 [0-9] F7");
        assertEquals(10, actual.size());
        assertEquals("0xF043207A4C4D202030303636535900000000000000000000000000000000F7", actual.getFirst()
                .getHexValues());
        assertEquals("0xF043207A4C4D202030303636535900000000000000000000000000000009F7", actual.getLast()
                .getHexValues());
    }

    @ParameterizedTest
    @CsvSource({
            "F0 41 00 42 11 480000* 001D10 CK6 F7",
            "F0 41 00 42 11 480000 001D10 CK3F7",
            "F04100*4211480000001D10F7"
    })
    void parseWithError(String input) throws InvalidMidiDataException {
        assertThrows(MidiError.class, () -> SysExBuilder.parse(input));
    }
}