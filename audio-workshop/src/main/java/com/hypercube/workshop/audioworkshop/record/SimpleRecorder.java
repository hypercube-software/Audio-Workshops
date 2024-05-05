package com.hypercube.workshop.audioworkshop.record;

import com.hypercube.workshop.audioworkshop.common.device.AudioInputDevice;
import com.hypercube.workshop.audioworkshop.common.errors.AudioError;
import com.hypercube.workshop.audioworkshop.common.line.AudioInputLine;
import com.hypercube.workshop.audioworkshop.common.line.AudioLineFormat;

import javax.sound.sampled.LineUnavailableException;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class SimpleRecorder {
    public void recordWAV(AudioInputDevice inputDevice, AudioLineFormat format, File output) {
        try (AudioInputLine line = new AudioInputLine(inputDevice, format)) {
            line.recordWAV(4, TimeUnit.SECONDS, output);
        } catch (LineUnavailableException | IOException e) {
            throw new AudioError(e);
        }
    }

}
