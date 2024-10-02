package com.hypercube.workshop.audioworkshop.record;

import com.hypercube.workshop.audioworkshop.common.device.AudioDeviceManager;
import com.hypercube.workshop.audioworkshop.common.errors.AudioError;
import com.hypercube.workshop.audioworkshop.common.format.PCMBufferFormat;
import com.hypercube.workshop.audioworkshop.common.pcm.BitDepth;
import com.hypercube.workshop.audioworkshop.common.pcm.PCMEncoding;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.io.File;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.concurrent.TimeUnit;

@Slf4j
@ShellComponent
@AllArgsConstructor
public class RecordCLI {
    public static final int BUFFER_DURATION_MS = 80;

    @ShellMethod(value = "Record audio input")
    public void recordWAV(@ShellOption(value = "-i") String inputDevice, @ShellOption(value = "-f") File wavFile) {
        AudioDeviceManager m = new AudioDeviceManager();
        m.collectDevices();
        m.getInputs()
                .forEach(d -> d.logFormats());
        PCMBufferFormat format = new PCMBufferFormat(BUFFER_DURATION_MS, 44100, BitDepth.BIT_DEPTH_16, 1, PCMEncoding.SIGNED, ByteOrder.LITTLE_ENDIAN);
        var in = m.getInput(inputDevice)
                .orElseThrow(() -> new AudioError("Device not found:" + inputDevice));
        log.info("Record 4 seconds of audio into {}", wavFile);
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
        PCMBufferFormat format = new PCMBufferFormat(BUFFER_DURATION_MS, 44100, BitDepth.BIT_DEPTH_16, 2, PCMEncoding.SIGNED, ByteOrder.LITTLE_ENDIAN);
        var in = m.getInput(inputDevice)
                .orElseThrow(() -> new AudioError("Device not found:" + inputDevice));
        var out = m.getOutput(outputDevice)
                .orElseThrow(() -> new AudioError("Device not found:" + outputDevice));
        int durationInSec = 10;
        log.info("Record {} seconds of audio into {}", durationInSec, wavFile);
        try (MonitorRecorder monitorRecorder = new MonitorRecorder(wavFile, format, durationInSec, TimeUnit.SECONDS)) {
            monitorRecorder.recordWithMonitoring(in, out, format);
        } catch (IOException e) {
            throw new AudioError(e);
        }
    }
}
