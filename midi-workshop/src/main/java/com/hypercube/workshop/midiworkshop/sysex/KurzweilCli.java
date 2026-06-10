package com.hypercube.workshop.midiworkshop.sysex;

import com.hypercube.workshop.midiworkshop.presets.kurzweil.KurzweilExplorer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.nio.file.Path;

@ShellComponent()
@ShellCommandGroup("Kurzweil CLI")
@Slf4j
@RequiredArgsConstructor
public class KurzweilCli {
    private final KurzweilExplorer kurzweilExplorer;

    @ShellMethod("Extract Kurzweil Programs")
    public void extractKurzweilPrograms(@ShellOption(value = "-d", help = "Device Name") String deviceName) {
        kurzweilExplorer.listProgramBanks(deviceName);
    }

    @ShellMethod("Extract Kurzweil sample")
    public void getKurzweilSample(@ShellOption(value = "-d", help = "Device Name") String deviceName, @ShellOption(value = "-s", help = "Sample Id") int sampleId) {
        kurzweilExplorer.getSample(deviceName, sampleId);
    }

    @ShellMethod("Extract Kurzweil object")
    public void getKurzweilObject(@ShellOption(value = "-d", help = "Device Name") String deviceName,
                                  @ShellOption(value = "-t", help = "Object type in decimal") int type,
                                  @ShellOption(value = "-i", help = "Object id in decimal") int id) {
        kurzweilExplorer.loadObject(deviceName, type, id);
    }

    @ShellMethod("Dir Kurzweil Bank")
    public void dirKurzweilObjects(@ShellOption(value = "-d", help = "Device Name") String deviceName,
                                   @ShellOption(value = "-t", help = "Object type in decimal") int type,
                                   @ShellOption(value = "-b", help = "Bank number in decimal") int bank) {
        kurzweilExplorer.dirBank(deviceName, type, bank);
    }

    @ShellMethod("Send Kurzweil Patch")
    public void sendKurzweilPatch(@ShellOption(value = "-d", help = "Device Name") String deviceName,
                                  @ShellOption(value = "-f", help = "SysEx file") String filename
    ) {
        kurzweilExplorer.sendPatch(deviceName, Path.of(filename));
    }
}
