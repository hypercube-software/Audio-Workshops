package com.hypercube.workshop.midiworkshop.common.clock;

import com.hypercube.workshop.midiworkshop.common.MidiOutDevice;
import com.hypercube.workshop.midiworkshop.common.errors.MidiError;
import lombok.extern.slf4j.Slf4j;

import javax.sound.midi.MidiUnavailableException;

@Slf4j
public class TimerBasedMidiClock implements MidiClock {
    private static final int MIDI_CLOCK_PPQ = 24;


    private final MidiOutDevice clock;

    private final Object signal = new Object();

    private final Thread thread;
    private volatile long clockPeriod;

    private volatile boolean mute = false;

    private volatile boolean exit = false;
    private long avgSendDurationInNanoSec;

    public TimerBasedMidiClock(MidiOutDevice clock) {
        this.clock = clock;

        try {
            clock.open();
            calibrate();
            thread = new Thread(this::threadLoop);
            thread.start();
        } catch (MidiUnavailableException e) {
            throw new MidiError(e);
        }
    }

    private void calibrate() {
        int nb = 1000;
        long start = System.nanoTime();
        for (int i = 0; i < nb; i++) {
            clock.sendActiveSensing();
        }
        long end = System.nanoTime();
        avgSendDurationInNanoSec = (end - start) / nb;
    }

    private void threadLoop() {
        log.info("Clock started");
        try {
            Thread.currentThread()
                    .setPriority(Thread.MAX_PRIORITY);
            long tickStart = -1;
            while (!exit) {
                while (tickStart != -1 && System.nanoTime() - tickStart < clockPeriod) {
                    // active loop
                }
                tickStart = System.nanoTime();
                if (!mute) {
                    clock.sendClock();
                    synchronized (signal) {
                        signal.notifyAll();
                    }
                }
            }
        } catch (Exception e) {
            log.error("Unexpected error", e);
        }
        log.info("Clock terminated");
    }

    @Override
    public void close() {
        stop();
        clock.close();
        if (thread != null) {
            exit = true;
            thread.interrupt();
            try {
                thread.join();
            } catch (InterruptedException e) {
                log.warn("Interrupted", e);
                Thread.currentThread()
                        .interrupt();
            }
        }
    }

    @Override
    public void start() {
        mute = false;
    }

    @Override
    public void stop() {
        mute = true;
    }

    @Override
    public void updateTempo(int tempo) {
        // https://stackoverflow.com/a/2038364
        long beatDurationInNanoSec = (60L * 1000000000L) / tempo;
        clockPeriod = (beatDurationInNanoSec / MIDI_CLOCK_PPQ) - avgSendDurationInNanoSec;
        log.info(String.format("ClockPeriod %d nano seconds, avgSendDurationInNanoSec: %d", clockPeriod, avgSendDurationInNanoSec));
    }

    @Override
    public void waitNextMidiTick() {
        synchronized (signal) {
            try {
                signal.wait();
            } catch (InterruptedException e) {
                log.warn("Interrupted", e);
                Thread.currentThread()
                        .interrupt();
            }
        }
    }

    @Override
    public int getResolutionPPQ() {
        return MIDI_CLOCK_PPQ;
    }
}
