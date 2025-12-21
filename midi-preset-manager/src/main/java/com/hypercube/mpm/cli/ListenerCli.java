package com.hypercube.mpm.cli;

import com.hypercube.workshop.midiworkshop.api.config.ConfigHelper;
import com.hypercube.workshop.midiworkshop.api.devices.AbstractMidiDevice;
import com.hypercube.workshop.midiworkshop.api.devices.remote.server.NetworkServer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

@ShellComponent
@Slf4j
@RequiredArgsConstructor
public class ListenerCli {
    private final NetworkServer networkServer;

    @ShellMethod(key = "listen", value = "listen UDP MIDI")
    public void listen() {
        networkServer.getConfig()
                .midiPortsManager()
                .collectDevices();
        networkServer.getConfig()
                .midiDeviceLibrary()
                .load(ConfigHelper.getApplicationFolder(MidiPresetManagerCliApplication.class));
        listDevices();
        networkServer.start(8000);
    }

    private void listDevices() {
        var m = networkServer.getConfig()
                .midiPortsManager();
        m.getInputs()
                .forEach(d -> log.info(String.format("MIDI INPUT  Port \"%s\"%s", d.getName(), getDeviceAlias(d))));
        m.getOutputs()
                .forEach(d -> log.info(String.format("MIDI OUTPUT Port \"%s\"%s", d.getName(), getDeviceAlias(d))));
    }

    private String getDeviceAlias(AbstractMidiDevice midiDevice) {
        return networkServer.getConfig()
                .midiDeviceLibrary()
                .getDeviceFromMidiPort(midiDevice
                        .getName())
                .map(def -> " => bound to library device \"%s\"".formatted(def.getDeviceName()))
                .orElse("");
    }
}
