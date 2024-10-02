package com.hypercube.workshop.audioworkshop.common.insights.dft.slow;

import com.hypercube.workshop.audioworkshop.common.format.PCMBufferFormat;
import com.hypercube.workshop.audioworkshop.common.insights.dft.DFTCalculator;
import com.hypercube.workshop.audioworkshop.common.insights.dft.DFTResult;
import com.hypercube.workshop.audioworkshop.common.insights.dft.windows.DFTWindowGenerator;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class SimpleDFTCalculator implements DFTCalculator {
    private final PCMBufferFormat format;
    private final int windowSizeInSamples;
    private final DFTWindowGenerator dftWindowGenerator;
    @Getter
    private final double[] window;
    private long totalSamples;
    private double[][] inDftImg;
    private double[][] inDftReal;
    @Getter
    private double[][] dftImg;
    @Getter
    private double[][] dftReal;
    @Getter
    private List<DFTResult>[] magnitudes;

    public SimpleDFTCalculator(PCMBufferFormat format, DFTWindowGenerator dftWindowGenerator) {
        this.format = format;
        this.windowSizeInSamples = format.getSampleBufferSize();
        this.dftWindowGenerator = dftWindowGenerator;
        this.window = dftWindowGenerator.generate(windowSizeInSamples);
        reset();
    }


    @Override
    public void reset() {
        int nbChannels = format.getNbChannels();
        inDftImg = new double[nbChannels][windowSizeInSamples];
        inDftReal = new double[nbChannels][windowSizeInSamples];
        dftImg = new double[nbChannels][windowSizeInSamples];
        dftReal = new double[nbChannels][windowSizeInSamples];
        magnitudes = new List[nbChannels];
        for (int i = 0; i < nbChannels; i++) {
            magnitudes[i] = new ArrayList<>();
        }
        totalSamples = 0;
    }

    @Override
    public void onBuffer(double[][] samples, int nbSamples, int nbChannels) {
        totalSamples += nbSamples;
        for (int c = 0; c < nbChannels; c++) {
            for (int s = 0; s < nbSamples; s++) {
                double sample = samples[c][s] * window[s];
                inDftImg[c][s] = 0;
                inDftReal[c][s] = sample;
            }
            computeDft(inDftReal[c], inDftImg[c], dftReal[c], dftImg[c]);
            DFTResult r = new DFTResult(nbSamples / 2);
            for (int s = 0; s < nbSamples / 2; s++) {
                double magnitude = Math.sqrt(dftReal[c][s] * dftReal[c][s] + dftImg[c][s] * dftImg[c][s]);
                // toDB
                magnitude = ((double) 20) * Math.log10(magnitude + Double.MIN_VALUE);

                double freqBin = s * (double) format.getSampleRate() / nbSamples;
                //System.out.println("Bin %d %.03fHz Magnitude: %.03f dB , ".formatted(s, freqBin, magnitude));
                r.setMagnitude(s, magnitude);
            }
            magnitudes[c].add(r);
        }
    }

    void computeDft(double[] inreal, double[] inimag, double[] outreal, double[] outimag) {
        int n = inreal.length;
        for (int k = 0; k < n; k++) {  // For each output element
            double sumreal = 0;
            double sumimag = 0;
            for (int t = 0; t < n; t++) {  // For each input element
                double angle = 2 * Math.PI * t * k / n;
                sumreal += inreal[t] * Math.cos(angle) + inimag[t] * Math.sin(angle);
                sumimag += -inreal[t] * Math.sin(angle) + inimag[t] * Math.cos(angle);
            }
            outreal[k] = sumreal;
            outimag[k] = sumimag;
        }
    }
}
