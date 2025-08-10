package com.hypercube.workshop.audioworkshop.record;

import com.hypercube.workshop.audioworkshop.api.device.AudioInputDevice;
import com.hypercube.workshop.audioworkshop.api.errors.AudioError;
import com.hypercube.workshop.audioworkshop.api.format.PCMBufferFormat;
import com.hypercube.workshop.audioworkshop.api.line.AudioInputLine;

import javax.sound.sampled.LineUnavailableException;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class SimpleRecorder {
    public void recordWAV(AudioInputDevice inputDevice, PCMBufferFormat format, File output) {
        try (AudioInputLine line = new AudioInputLine(inputDevice, format)) {
            line.recordWAV(4, TimeUnit.SECONDS, output);
        } catch (LineUnavailableException | IOException e) {
            throw new AudioError(e);
        }
    }

}
