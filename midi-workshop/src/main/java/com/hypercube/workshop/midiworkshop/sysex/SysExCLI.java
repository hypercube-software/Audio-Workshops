package com.hypercube.workshop.midiworkshop.sysex;

import com.hypercube.workshop.midiworkshop.sysex.parser.SysExFileParser;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.io.File;

@ShellComponent()
@ShellCommandGroup("SysEx CLI")
@Slf4j
@AllArgsConstructor
public class SysExCLI {
    private final SysExFileParser sysExFileParser;

    @ShellMethod(value = "parse SysEx file and dump the device memory to disk")
    public void parse(@ShellOption(value = "-i") File input, @ShellOption(value = "-o") File output) {
        sysExFileParser.parse(input, output);
    }
}
