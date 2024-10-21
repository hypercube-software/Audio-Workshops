package com.hypercube.workshop.audioworkshop.synth.vco;

import com.hypercube.workshop.audioworkshop.synth.vca.VCA;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;


/**
 * This naive VCO try to generate multiple periods of sine in one samples
 * The goal is to reduce the number of calls but on the other hand we
 * reallocate a samples on each call
 * But there is one mistake about periodDurationInSamples calculation...
 */
public class NaiveVCO extends VCO {

    public NaiveVCO(int bufferSizeRequestedInMs, double sampleRate, int bitDepth, ByteOrder byteOrder, VCA vca) {
        super(bufferSizeRequestedInMs, sampleRate, bitDepth, byteOrder, vca);
    }

    @Override
    public byte[] generateSignal(double freq) {
        // Rule N°1: NEVER ever use sample position width decimal part
        // The FIX: double periodDurationInSamples = Math.floor(sampleRate /freq);
        // The following will generate incorrect value, which can be ear in high notes
        double periodDurationInSamples = sampleRate / freq;

        double samplePeriodInMs = 1000 / sampleRate;
        double periodDurationInMs = periodDurationInSamples * samplePeriodInMs;
        double angleIncrementInRadians = (SIN_PERIOD_2PI / periodDurationInSamples);
        int nbPeriodsToGenerate = (int) Math.max(1, bufferSizeRequestedInMs / periodDurationInMs);

        // allocate the samples
        int samples = (int) (periodDurationInSamples * nbPeriodsToGenerate);
        var output = ByteBuffer.allocate(samples * getBytesPerSamples());
        output.order(byteOrder);

        for (int i = 0; i < samples; i++) {
            double anglePos = i * angleIncrementInRadians;

            // convert the sample to Signed 16 bits
            short sample = (short) (vca.getCurrentGain() * Math.sin(anglePos) * 0x7FFF);

            // write the sample into the samples
            output.putShort(sample);
        }

        // this does not copy the array
        return output.array();
    }


}
