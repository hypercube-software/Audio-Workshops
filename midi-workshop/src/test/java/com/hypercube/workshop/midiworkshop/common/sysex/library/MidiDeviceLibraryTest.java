package com.hypercube.workshop.midiworkshop.common.sysex.library;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MidiDeviceLibraryTest {
    public static final String DEVICE_NAME = "TG-500";
    public static final File APP_CONFIGFILE = new File("MyAppConfigFile");
    MidiDeviceLibrary midiDeviceLibrary;

    @BeforeEach
    void init() {
        midiDeviceLibrary = new MidiDeviceLibrary();
        midiDeviceLibrary.load(new File("./src/test/resources/devices-library"));
    }

    @Test
    void load() {
        // WHEN
        MidiDeviceDefinition midiDeviceDefinition = midiDeviceLibrary.getDevice(DEVICE_NAME)
                .get();

        // THEN
        assertEquals(DEVICE_NAME, midiDeviceDefinition.getDeviceName());
        assertEquals(14, midiDeviceDefinition.getMacros()
                .size());
    }

    @Test
    void forgeRequestsWithSequence() {
        // WHEN
        var requests = midiDeviceLibrary.forgeMidiRequestsSequence(APP_CONFIGFILE, DEVICE_NAME, "getAll() : --- : AllMulti();AllPerformances()");
        // THEN
        assertEquals("getAll", requests.getName());
        assertEquals(2, requests.getMidiRequests()
                .size());
    }

    @Test
    void forgeRequestsWithoutSequence() {
        // WHEN
        var requests = midiDeviceLibrary.forgeMidiRequestsSequence(APP_CONFIGFILE, DEVICE_NAME, "getAll() : --- : AllMulti()");
        // THEN
        assertEquals("getAll", requests.getName());
        assertEquals(1, requests.getMidiRequests()
                .size());
    }
}