package com.hypercube.workshop.midiworkshop.sysex.yamaha;

import com.hypercube.workshop.midiworkshop.sysex.yamaha.cs1x.CS1XPresetGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

import java.io.IOException;

@ShellComponent()
@ShellCommandGroup("Yamaha CLI")
@Slf4j
@RequiredArgsConstructor
public class YamahaCLI {
    private final CS1XPresetGenerator cs1XPresetGenerator;

    @ShellMethod("Generate CS1X voices SysEx")
    public void dumpCS1XVoices() throws InterruptedException, IOException {
        cs1XPresetGenerator.dumpCS1XVoices("CS1x");
    }
}
