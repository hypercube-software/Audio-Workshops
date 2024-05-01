package com.hypercube.workshop.audioworkshop.record;

import com.hypercube.workshop.audioworkshop.common.device.AudioDeviceManager;
import com.hypercube.workshop.audioworkshop.common.errors.AudioError;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import javax.sound.sampled.AudioFormat;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
@ShellComponent
@AllArgsConstructor
public class RecordCLI {
    @ShellMethod(value = "Record audio input")
    public void recordWAV(@ShellOption(value = "-i") String inputDevice, @ShellOption(value = "-f") File wavFile) {
        AudioDeviceManager m = new AudioDeviceManager();
        m.collectDevices();
        m.getInputs()
                .forEach(d -> d.logFormats());
        AudioFormat format = new AudioFormat(44100, 16, 1, true, false);
        var in = m.getInput(inputDevice)
                .orElseThrow(() -> new AudioError("Device not found:" + inputDevice));
        log.info("Record 4 seconds of audio");
        SimpleRecorder simpleRecorder = new SimpleRecorder();
        simpleRecorder.recordWAV(in, format, wavFile);
    }

    @ShellMethod(value = "Record audio input and monitor at the same time")
    public void listen(@ShellOption(value = "-i") String inputDevice, @ShellOption(value = "-o") String outputDevice, @ShellOption(value = "-f") File wavFile) {
        AudioDeviceManager m = new AudioDeviceManager();
        m.collectDevices();
        m.getInputs()
                .forEach(d -> d.logFormats());
        m.getOutputs()
                .forEach(d -> log.info("OUTPUT: " + d.getName()));
        AudioFormat format = new AudioFormat(44100, 16, 2, true, false);
        var in = m.getInput(inputDevice)
                .orElseThrow(() -> new AudioError("Device not found:" + inputDevice));
        var out = m.getOutput(outputDevice)
                .orElseThrow(() -> new AudioError("Device not found:" + outputDevice));
        try (MonitorRecorder monitorRecorder = new MonitorRecorder(wavFile, format, 10, TimeUnit.SECONDS)) {
            monitorRecorder.recordWithMonitoring(in, out, format);
        } catch (IOException e) {
            throw new AudioError(e);
        }
    }
}
