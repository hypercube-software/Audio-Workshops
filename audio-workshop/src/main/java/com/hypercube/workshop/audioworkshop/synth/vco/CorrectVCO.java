package com.hypercube.workshop.audioworkshop.synth.vco;

import com.hypercube.workshop.audioworkshop.synth.vca.VCA;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;


/**
 * This VCO reuse its samples instead of creating one on every calls
 * The anglePos does not restart from 0 on each call
 * The anglePos stay inside the period to avoid big values of IEEE754
 */
public class CorrectVCO extends VCO {
    final double lastFreq = 0;
    private ByteBuffer output;
    private int samples;
    private double angleIncrementInRadians;
    private double anglePos;

    public CorrectVCO(int bufferSizeRequestedInMs, double sampleRate, int bitDepth, ByteOrder byteOrder, VCA vca) {
        super(bufferSizeRequestedInMs, sampleRate, bitDepth, byteOrder, vca);
    }

    @Override
    public byte[] generateSignal(double freq) {
        // Compute the samples each time a new frequency is asked
        if (freq != lastFreq) {
            double periodDurationInSamples = sampleRate / freq;
            angleIncrementInRadians = (SIN_PERIOD_2PI / periodDurationInSamples);
            samples = (int) (periodDurationInSamples);
            output = ByteBuffer.allocate(samples * getBytesPerSamples());
            output.order(byteOrder);
            anglePos = 0;
        }
        output.rewind();
        for (int i = 0; i < samples; i++) {
            // convert the sample to Signed 16 bits
            short sample = (short) (vca.getCurrentGain() * Math.sin(anglePos) * 0x7FFF);

            // write the sample into the samples
            output.putShort(sample);

            // Rule N°2: Avoid big numbers width IEEE754
            // FIX: we stay inside the period width this modulo
            anglePos += angleIncrementInRadians;
            if (anglePos >= SIN_PERIOD_2PI) {
                anglePos -= SIN_PERIOD_2PI;
            }
        }

        // this does not copy the array
        return output.array();
    }


}
