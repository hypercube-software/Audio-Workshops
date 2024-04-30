package com.hypercube.workshop.audioworkshop.record;

import com.hypercube.workshop.audioworkshop.common.AudioInputDevice;
import com.hypercube.workshop.audioworkshop.common.errors.AudioError;
import com.hypercube.workshop.audioworkshop.common.line.AudioInputLine;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineUnavailableException;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class SimpleRecorder {
    public void recordWAV(AudioInputDevice inputDevice, AudioFormat format, File output) {
        try (AudioInputLine line = new AudioInputLine(inputDevice, format, 1000)) {
            line.recordWAV(4, TimeUnit.SECONDS, output);
        } catch (LineUnavailableException | IOException e) {
            throw new AudioError(e);
        }
    }

}
