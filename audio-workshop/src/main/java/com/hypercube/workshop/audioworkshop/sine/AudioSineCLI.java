package com.hypercube.workshop.audioworkshop.sine;

import com.hypercube.workshop.audioworkshop.common.AudioDeviceManager;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.io.File;

@Slf4j
@ShellComponent
@AllArgsConstructor
public class AudioSineCLI {
    private final AudioSine audioSine;

    @ShellMethod(value = "Play a file")
    public void file(@ShellOption(value = "-o") String outputDevice, @ShellOption(value = "-f") File file, @ShellOption(value = "-l") int loops) {
        AudioDeviceManager m = new AudioDeviceManager();
        m.collectDevices();
        m.getOutputs().stream().filter(d -> d.getName().equals(outputDevice)).findFirst().ifPresentOrElse(d -> audioSine.playFile(d, file, loops), () -> log.error("Device not found:" + outputDevice));
    }

    @ShellMethod(value = "Play a sine  in a precalculated buffer")
    public void sine(@ShellOption(value = "-o") String outputDevice) {
        AudioDeviceManager m = new AudioDeviceManager();
        m.collectDevices();
        m.getOutputs().stream().filter(d -> d.getName().equals(outputDevice)).findFirst().ifPresentOrElse(audioSine::playSine, () -> log.error("Device not found:" + outputDevice));
    }

    @ShellMethod(value = "Play a sine  in real time")
    public void vco(@ShellOption(value = "-o") String outputDevice) {
        AudioDeviceManager m = new AudioDeviceManager();
        m.collectDevices();
        m.getOutputs().stream().filter(d -> d.getName().equals(outputDevice)).findFirst().ifPresentOrElse(audioSine::vco, () -> log.error("Device not found:" + outputDevice));
    }

    @ShellMethod(value = "Generate a signal in a file")
    public void gen(@ShellOption(value = "-n") int midiNote, @ShellOption(value = "-f") File file) {
        audioSine.generateFile(midiNote, file);
    }
}
