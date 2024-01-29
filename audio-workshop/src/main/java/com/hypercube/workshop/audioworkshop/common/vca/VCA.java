package com.hypercube.workshop.audioworkshop.common.vca;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public abstract class VCA {
    protected final double sampleRate;

    public abstract double getCurrentGain();

    public abstract void onNoteOn(double velocity);

    public abstract void onNoteOff();

    /**
     * 20dB is where human hearing start for most people
     * it correspond to an air pressure of 20 micro-pascal
     *
     * @param decibel in the range [0,20]
     * @return in the range [0,1]
     */
    public static double dbToAmplitude(double decibel) {
        return Math.pow(10.0d, (decibel / 20.0d));
    }

    /**
     * 20dB is where human hearing start for most people
     * it correspond to an air pressure of 20 micro-pascal
     *
     * @param amplitude in the range [0,1]
     * @return decibel in the range [0,20]
     */
    public static double amplitudeTodB(double amplitude) {
        return 20.d * Math.log10(amplitude);
    }
}
