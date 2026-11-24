package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer;

import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ManufacturerTest {

    @Test
    void searchDeviceAcrossManufacturers() {
        assertEquals("MPK-261", Manufacturer.searchDevice("MPK-261")
                .getName());
        assertEquals("DS-330", Manufacturer.searchDevice("DS-330")
                .getName());
        assertEquals("SE-02", Manufacturer.searchDevice("SE-02")
                .getName());
        assertEquals("Neutron", Manufacturer.searchDevice("Neutron")
                .getName());
    }

    @Test
    void searchUnknownDeviceFails() {
        assertThrows(MidiError.class, () -> Manufacturer.searchDevice("Unknown Device"));
    }
}
