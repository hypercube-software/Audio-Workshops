package com.hypercube.workshop.midiworkshop.common.sysex.util;

import com.hypercube.workshop.midiworkshop.common.sysex.library.device.MidiControllerValue;
import com.hypercube.workshop.midiworkshop.common.sysex.library.request.MidiRequest;
import org.junit.jupiter.api.Test;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SysExTemplateTest {

    @Test
    void forgeTemplateWithChecksum() {
        // GIVEN
        SysExTemplate template = SysExTemplate.of(new MidiRequest("Filter Cutoff", "F041104611 0008204A 000000 lsb CK8 F7", null, null));
        // WHEN
        var actual = template.forgePayload(new MidiControllerValue(0, 13));

        // THEN
        var hexa = IntStream.range(0, actual.length)
                .mapToObj(i -> String.format(" %02X", actual[i]))
                .collect(Collectors.joining())
                .trim();

        assertEquals("F0 41 10 46 11 00 08 20 4A 00 00 00 0D 01 F7", hexa);
    }
}
