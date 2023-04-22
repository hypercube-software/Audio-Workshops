package com.hypercube.workshop.audioworkshop.common;

import lombok.extern.slf4j.Slf4j;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

@Slf4j
public class AudioOutputDevice extends AbstractAudioDevice {

    public AudioOutputDevice(Mixer.Info mixerInfo) {
        super(mixerInfo);
    }

    public void play(AudioFormat af, byte[] data, int durationMs) throws LineUnavailableException, InterruptedException {
        final var clip = AudioSystem.getClip(mixerInfo);
        clip.open(af, data, 0, data.length);
        clip.setLoopPoints(0, -1);

        Object waiter = new Object();

        addAudioListener(clip, waiter);
        clip.loop(Clip.LOOP_CONTINUOUSLY);
        Thread.sleep(durationMs);
        clip.stop();
        clip.drain();
        clip.close();
    }

    public void play(File file, int loopCount) throws UnsupportedAudioFileException, IOException, LineUnavailableException, InterruptedException {
        AudioInputStream sound = AudioSystem.getAudioInputStream(file);
        log.info("Channels   : " + sound.getFormat().getChannels());
        log.info("Encoding   : " + sound.getFormat().getEncoding().toString());
        log.info("SampleRate : " + sound.getFormat().getSampleRate());
        log.info("BitDepth   : " + sound.getFormat().getSampleSizeInBits());
        final var clip = AudioSystem.getClip(mixerInfo);
        clip.open(sound);
        clip.setLoopPoints(0, -1);

        Object waiter = new Object();
        addAudioListener(clip, waiter);

        clip.loop(loopCount);

        waitStopEvent(waiter);
        clip.drain();
        clip.close();
    }

    private static void waitStopEvent(Object waiter) throws InterruptedException {
        synchronized (waiter) {
            waiter.wait();
        }
    }

    private static void addAudioListener(Clip clip, Object waiter) {
        clip.addLineListener(lineEvent -> {
            if (lineEvent.getType() == LineEvent.Type.STOP) {
                synchronized (waiter) {
                    waiter.notifyAll();
                }
            }
        });
    }
}
