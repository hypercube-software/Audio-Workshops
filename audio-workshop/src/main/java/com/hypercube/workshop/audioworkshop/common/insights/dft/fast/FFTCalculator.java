package com.hypercube.workshop.audioworkshop.common.insights.dft.fast;

import com.hypercube.workshop.audioworkshop.common.format.PCMBufferFormat;
import com.hypercube.workshop.audioworkshop.common.insights.dft.DFTCalculator;
import com.hypercube.workshop.audioworkshop.common.insights.dft.DFTResult;
import com.hypercube.workshop.audioworkshop.common.insights.dft.windows.DFTWindowGenerator;
import lombok.Getter;
import org.bytedeco.fftw.global.fftw3;
import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.Loader;

import java.util.ArrayList;
import java.util.List;

import static org.bytedeco.fftw.global.fftw3.*;

/**
 * FFTW3 buffers are interleaved, this mean we have [r,i,r,i,r,i,r,i,r,i,r,i]
 * where r = real part of a sample and i = imaginary part of a sample
 * DoublePointer contains the input samples in this format
 */
public class FFTCalculator implements DFTCalculator, AutoCloseable {

    public static final int COMPLEX_SAMPLE_SIZE = 2;
    private final double[] window;

    {
        Loader.load(org.bytedeco.fftw.global.fftw3.class);
    }

    @Getter
    private final PCMBufferFormat format;

    private fftw3.fftw_plan[] plan;
    private DoublePointer[] signal;
    private DoublePointer[] fft;
    private double[][] doubleInput;
    private double[][] doubleResult;

    private static final int REAL = 0;
    private static final int IMAG = 1;
    private static int windowSize;
    @Getter
    private List<DFTResult>[] magnitudes;

    public FFTCalculator(PCMBufferFormat format, DFTWindowGenerator dftWindowGenerator) {
        this.format = format;
        this.windowSize = format.getSampleBufferSize();
        this.window = dftWindowGenerator.generate(windowSize);
        reset();
    }

    @Override
    public void reset() {
        close();
        int nbChannels = format.getNbChannels();
        signal = new DoublePointer[nbChannels];
        fft = new DoublePointer[nbChannels];
        plan = new fftw_plan[nbChannels];
        doubleResult = new double[nbChannels][];
        doubleInput = new double[nbChannels][];
        magnitudes = new List[nbChannels];
        for (int i = 0; i < nbChannels; i++) {
            magnitudes[i] = new ArrayList<>();
        }
        for (int ch = 0; ch < nbChannels; ch++) {

            signal[ch] = new DoublePointer(COMPLEX_SAMPLE_SIZE * windowSize);
            fft[ch] = new DoublePointer(COMPLEX_SAMPLE_SIZE * windowSize);
            doubleResult[ch] = new double[(int) signal[ch].capacity()];
            doubleInput[ch] = new double[(int) signal[ch].capacity()];
            plan[ch] = fftw_plan_dft_r2c_1d(windowSize, signal[ch], fft[ch], FFTW_ESTIMATE);
        }
    }

    @Override
    public List<DFTResult>[] getMagnitudes() {
        return magnitudes;
    }

    @Override
    public void onBuffer(double[][] samples, int nbSamples, int nbChannels) {
        for (int ch = 0; ch < nbChannels; ch++) {
            for (int s = 0; s < nbSamples; s++) {
                double sample = samples[ch][s] * window[s];
                doubleInput[ch][s] = sample;
            }
            signal[ch].put(doubleInput[ch]);
            fftw_execute(plan[ch]);
            fft[ch].get(doubleResult[ch]);

            DFTResult r = new DFTResult(nbSamples / 2);
            double scale = 1.0;
            for (int freqBin = 0; freqBin < nbSamples / 2; freqBin++) {
                double realPart = doubleResult[ch][COMPLEX_SAMPLE_SIZE * freqBin + REAL] * scale;
                double imgPart = doubleResult[ch][COMPLEX_SAMPLE_SIZE * freqBin + IMAG] * scale;
                double magnitude = Math.sqrt(realPart * realPart + imgPart * imgPart);
                // toDB
                magnitude = ((double) 20) * Math.log10(magnitude + Double.MIN_VALUE);
                double freq = freqBin * (double) format.getSampleRate() / nbSamples;
                //if (freq >= 420 && freq <= 450) {
                //    System.out.println("Bin %d %.03fHz Magnitude: %.03f dB %f, ".formatted(freqBin, freq, magnitude, magnitude));
                // }
                r.setMagnitude(freqBin, magnitude);
            }
            magnitudes[ch].add(r);
        }
    }

    @Override
    public void close() {
        if (plan != null) {
            for (int ch = 0; ch < format.getNbChannels(); ch++) {
                if (plan[ch] != null) {
                    fftw_destroy_plan(plan[ch]);
                }
            }
        }
    }
}
