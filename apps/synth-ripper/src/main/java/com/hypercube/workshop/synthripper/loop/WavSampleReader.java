package com.hypercube.workshop.synthripper.loop;

import com.hypercube.workshop.audioworkshop.api.consumer.SampleBuffer;
import com.hypercube.workshop.audioworkshop.files.riff.RiffFileInfo;
import com.hypercube.workshop.audioworkshop.files.riff.RiffReader;
import com.hypercube.workshop.audioworkshop.files.riff.insights.RiffInspector;
import com.hypercube.workshop.synthripper.model.SynthRipperError;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads the samples of a WAV file as a mono {@code float[]} for loop analysis.
 */
public final class WavSampleReader {

    private WavSampleReader() {
    }

    /**
     * Read all samples of the given wav file and mix them to mono.
     *
     * @param wavFile the wav file
     * @return mono samples in the [-1, 1] range
     */
    public static float[] readMonoSamples(File wavFile) {
        List<Double> mono = new ArrayList<>();
        try (RiffReader riffReader = new RiffReader(wavFile, false)) {
            RiffFileInfo info = riffReader.parse();
            int nbChannels = info.getAudioInfo()
                    .getNbChannels();
            RiffInspector inspector = new RiffInspector(riffReader, info);
            inspector.inspect((SampleBuffer buffer) -> {
                for (int s = 0; s < buffer.nbSamples(); s++) {
                    double sum = 0;
                    for (int ch = 0; ch < nbChannels; ch++) {
                        sum += buffer.sample(ch, s);
                    }
                    mono.add(sum / nbChannels);
                }
            });
        } catch (IOException e) {
            throw new SynthRipperError("Cannot read wav file: " + wavFile.getAbsolutePath(), e);
        }
        float[] samples = new float[mono.size()];
        for (int i = 0; i < samples.length; i++) {
            samples[i] = mono.get(i)
                    .floatValue();
        }
        return samples;
    }
}
