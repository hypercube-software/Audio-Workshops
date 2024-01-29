package com.hypercube.workshop.audioworkshop.common.vca;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SuppressWarnings({"FieldCanBeLocal", "java:S1068"})
public class AdsrVCA extends VCA {
    private enum State {
        OFF,
        ATTACK,
        SUSTAIN,
        RELEASE
    }

    private final double attack;
    private final double decay;
    private final double release;
    private double positionMs;

    private final double incrementMs;

    private Envelope currentEnvelope;
    private State state;

    private final Envelope silentEnv = new SustainEnvelope(0);
    private Envelope attackEnv;

    private Envelope sustainEnv;
    private Envelope releaseEnv;

    private final Object guardian = new Object();

    public AdsrVCA(double sampleRate, double attack, double decay, double release) {
        super(sampleRate);
        double sampleDurationInMs = 1.0d / (sampleRate / 1000.d);
        this.incrementMs = sampleDurationInMs;
        this.attack = attack;
        this.decay = decay;
        this.release = release;
        positionMs = 0;
        currentEnvelope = silentEnv;
        state = State.OFF;
    }

    @Override
    public double getCurrentGain() {
        synchronized (guardian) {
            if (currentEnvelope.done()) {
                log.info("done " + state);
                if (state == State.ATTACK) {
                    state = State.SUSTAIN;
                    currentEnvelope = sustainEnv;
                    positionMs = 0;
                } else if (state == State.RELEASE) {
                    state = State.OFF;
                    currentEnvelope = silentEnv;
                    positionMs = 0;
                }
            }
            double gain = currentEnvelope.value(positionMs);

            //log.info(String.format("ADSR: %d Index:%.8f ms gain: %.8f Increment: %.8f ms", currentEnvelope, positionMs, gain, incrementMs));
            positionMs += incrementMs;
            return gain;
        }
    }

    @Override
    public void onNoteOn(double velocity) {
        synchronized (guardian) {
            attackEnv = new LinearInterpolator(0, 0, attack, velocity);
            sustainEnv = new SustainEnvelope(velocity);
            releaseEnv = new LinearInterpolator(0, velocity, this.release, 0);

            positionMs = 0;
            currentEnvelope = attackEnv;
            state = State.ATTACK;
        }
    }

    @Override
    public void onNoteOff() {
        synchronized (guardian) {
            positionMs = 0;
            currentEnvelope = releaseEnv;
            state = State.RELEASE;
        }
    }
}
