package com.hypercube.workshop.audioworkshop.files;

import com.hypercube.workshop.audioworkshop.files.riff.RiffFileInfo;
import com.hypercube.workshop.audioworkshop.files.riff.RiffReader;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.io.File;
import java.io.IOException;

@Slf4j
@ShellComponent
@AllArgsConstructor
public class RiffCLI {
    @ShellMethod(value = "Load a file")
    public void parseNpr(@ShellOption(value = "-i") File file) throws IOException {
        try (RiffReader riffReader = new RiffReader(file, false)) {
            RiffFileInfo riffFileInfo = riffReader.parse();
        }
    }
}
