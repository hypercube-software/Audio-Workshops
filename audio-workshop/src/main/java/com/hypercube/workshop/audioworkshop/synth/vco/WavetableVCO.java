package com.hypercube.workshop.audioworkshop.synth.vco;

import com.hypercube.workshop.audioworkshop.api.errors.AudioError;
import com.hypercube.workshop.audioworkshop.synth.vca.VCA;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

@SuppressWarnings("FieldCanBeLocal")
@Slf4j
public class WavetableVCO extends VCO {
    private static final int WAVETABLE_LENGTH = 64;

    private final double[] wavetable;

    private ByteBuffer output;

    double lastFreq = 0;
    private double index;

    private double indexIncrement;

    public WavetableVCO(WavetableType wavetableType, int bufferSizeRequestedInMs, double sampleRate, int bitDepth, ByteOrder byteOrder, VCA vca) {
        super(bufferSizeRequestedInMs, sampleRate, bitDepth, byteOrder, vca);
        index = 0;
        wavetable = new double[WAVETABLE_LENGTH];
        switch (wavetableType) {
            case SINE -> generateSine(wavetable);
            default -> throw new AudioError("wave table type currently not Implemented:" + wavetableType);
        }
    }

    private void generateSine(double[] wavetable) {
        for (int i = 0; i < wavetable.length; i++) {
            wavetable[i] = Math.sin(SIN_PERIOD_2PI * i / wavetable.length);
        }
    }

    @Override
    public byte[] generateSignal(double freq) {

        // Compute the samples each time a new frequency is asked
        if (freq != lastFreq) {
            output = ByteBuffer.allocate(wavetable.length * getBytesPerSamples());
            output.order(byteOrder);
            indexIncrement = freq * wavetable.length / sampleRate;
            index = 0;
            lastFreq = freq;
        }

        output.rewind();
        for (int i = 0; i < wavetable.length; i++) {
            // convert the sample to Signed 16 bits
            short sample = (short) (vca.getCurrentGain() * nextSample() * 0x7FFF);

            // write the sample into the samples
            output.putShort(sample);
        }

        // this does not copy the array
        return output.array();
    }

    private double nextSample() {
        double truncatedIndex = Math.floor(index);
        double nextIndex = Math.ceil(index);
        if (nextIndex >= wavetable.length) {
            nextIndex -= wavetable.length;
        }
        double nextIndexWeight = index - truncatedIndex;
        double sample = wavetable[(int) nextIndex] * nextIndexWeight + (1d - nextIndexWeight) * wavetable[(int) truncatedIndex];
        //log.info(String.format("Index: %.8f Next Index: %.8f truncatedIndex: %.8f nextIndexWeight: %.8f Sample: %.8f",index,nextIndex,truncatedIndex,nextIndexWeight,sample));
        index += indexIncrement;
        if (index >= wavetable.length) {
            index -= wavetable.length;
        }
        return sample;
    }
}
