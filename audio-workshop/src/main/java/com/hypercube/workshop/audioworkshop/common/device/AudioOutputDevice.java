package com.hypercube.workshop.audioworkshop.common.device;

import lombok.extern.slf4j.Slf4j;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class AudioOutputDevice extends AbstractAudioDevice {
    private List<DataLine.Info> outputLines;

    public AudioOutputDevice(Mixer.Info mixerInfo, List<DataLine.Info> outputLines) {
        super(mixerInfo);
        this.outputLines = outputLines;
    }

    public void logFormats() {
        log.info("AUDIO OUTPUT: " + getName());
        outputLines.forEach(dataline -> {
            log.info("\tDATA LINE: " + dataline.toString());
            Arrays.stream(dataline.getFormats())
                    .forEach(format -> log.info("\t\t" + format.toString()));
        });
    }

    private static final Object stopEvent = new Object();

    public void play(AudioFormat af, byte[] data, int durationMs) throws LineUnavailableException, InterruptedException {
        final var clip = AudioSystem.getClip(mixerInfo);
        clip.open(af, data, 0, data.length);
        clip.setLoopPoints(0, -1);

        addAudioListener(clip);
        clip.loop(Clip.LOOP_CONTINUOUSLY);
        Thread.sleep(durationMs);
        clip.stop();
        clip.drain();
        clip.close();
    }

    public void play(File file, int loopCount) throws UnsupportedAudioFileException, IOException, LineUnavailableException, InterruptedException {
        AudioInputStream sound = AudioSystem.getAudioInputStream(file);
        log.info("Channels   : " + sound.getFormat()
                .getChannels());
        log.info("Encoding   : " + sound.getFormat()
                .getEncoding()
                .toString());
        log.info("SampleRate : " + sound.getFormat()
                .getSampleRate());
        log.info("BitDepth   : " + sound.getFormat()
                .getSampleSizeInBits());
        final var clip = AudioSystem.getClip(mixerInfo);
        clip.open(sound);
        clip.setLoopPoints(0, -1);


        addAudioListener(clip);

        clip.loop(loopCount);

        waitStopEvent();
        clip.drain();
        clip.close();
    }

    private static void waitStopEvent() throws InterruptedException {
        synchronized (stopEvent) {
            stopEvent.wait();
        }
    }

    private static void addAudioListener(Clip clip) {
        clip.addLineListener(lineEvent -> {
            if (lineEvent.getType() == LineEvent.Type.STOP) {
                synchronized (stopEvent) {
                    stopEvent.notifyAll();
                }
            }
        });
    }
}
