package com.hypercube.mpm.cli;

import com.hypercube.mpm.udp.MidiUDPProxy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

@ShellComponent
@Slf4j
@RequiredArgsConstructor
public class ListenerCli {
    private final MidiUDPProxy midiUDPProxy;

    @ShellMethod(key = "listen", value = "listen UDP MIDI")
    public void listen() {
        midiUDPProxy.start(10092);
    }
}
