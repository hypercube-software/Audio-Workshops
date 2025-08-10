package com.hypercube.workshop.audioworkshop.files.riff.insights;

import com.hypercube.workshop.audioworkshop.api.consumer.SampleBuffer;
import com.hypercube.workshop.audioworkshop.api.consumer.SampleBufferConsumer;
import com.hypercube.workshop.audioworkshop.api.errors.AudioError;
import com.hypercube.workshop.audioworkshop.api.format.PCMBufferFormat;
import com.hypercube.workshop.audioworkshop.api.format.PCMFormat;
import com.hypercube.workshop.audioworkshop.api.pcm.PCMConverter;
import com.hypercube.workshop.audioworkshop.files.riff.RiffFileInfo;
import com.hypercube.workshop.audioworkshop.files.riff.RiffReader;
import com.hypercube.workshop.audioworkshop.files.riff.WaveChannels;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;

/**
 * Pass all samples from a Riff/WAV File to a {@link SampleBufferConsumer}
 * <pre>As an example, convert samples to PNG or CSV
 */
@Slf4j
@RequiredArgsConstructor
public class RiffInspector {
    private final RiffReader riffReader;
    private final RiffFileInfo info;

    /**
     * Decode all PCM samples from WAV file and pass them to a {@link SampleBufferConsumer}
     *
     * @param sampleConsumer will receive buffers of samples for each channels
     */
    public void inspect(SampleBufferConsumer sampleConsumer) {
        PCMBufferFormat format = new PCMBufferFormat(50, info.getAudioInfo()
                .toPCMFormat());
        int nbChannels = format.getNbChannels();
        var pcmConverter = PCMConverter.getPCMtoSampleFunction(format);
        int bufferSizeInBytes = format.getByteBufferSize();
        int bufferSizeInSamples = format.getSampleBufferSize();
        double[][] samples = new double[nbChannels][bufferSizeInSamples];
        sampleConsumer.reset();
        try {
            riffReader.streamChunk(info.getDataChunk(), bufferSizeInBytes, (pcmBuffer, size) -> {
                int nbSamples = size / format.getFrameSizeInBytes();
                checkBufferSize(size, format);
                pcmConverter.convert(pcmBuffer, samples, nbSamples, nbChannels);
                sampleConsumer.onBuffer(new SampleBuffer(samples, 0, nbSamples, nbChannels));
            });
        } catch (IOException e) {
            throw new AudioError(e);
        }
    }

    /**
     * Example using {@link  #inspect(SampleBufferConsumer)} to dump all samples into a CSV file
     *
     * @param csvFile the output CSV File to create
     */
    public void saveCsv(File csvFile) {
        log.info("Generate {}", csvFile.getAbsolutePath());
        csvFile.getParentFile()
                .mkdirs();
        try (PrintWriter out = new PrintWriter(new FileOutputStream(csvFile))) {
            out.println("Sep=,");
            for (int c = 0; c < info.getAudioInfo()
                    .getNbChannels(); c++) {
                if (c > 0) {
                    out.print(",");
                }
                out.print(WaveChannels.valueOfOrdinal(c)
                        .getShortTitle());
            }
            out.println();
            inspect((buffer) -> {
                for (int s = 0; s < buffer.nbSamples(); s++) {
                    for (int c = 0; c < buffer.nbChannels(); c++) {
                        if (c > 0) {
                            out.print(",");
                        }
                        out.print(String.format(Locale.ENGLISH, "%f", buffer.sample(c, s)));
                    }
                    out.println();
                }
            });
        } catch (IOException e) {
            throw new AudioError(e);
        }
    }

    /**
     * Make sure we consume proper samples size given the frame size
     *
     * @param size   Size of the incoming samples in bytes
     * @param format Expected format providing the right frame size
     */
    private void checkBufferSize(int size, PCMFormat format) {
        if (size % format.getFrameSizeInBytes() != 0)
            throw new IllegalArgumentException("samples size %d is not a multiple of frame size %d in bytes".formatted(size, format.getFrameSizeInBytes()));
    }

    /**
     * Example using {@link  #inspect(SampleBufferConsumer)} to display the overall waveform in PNG
     *
     * @param pngFile     The PNG file to create
     * @param imageWidth  Width in pixels
     * @param imageHeight Height in pixels
     */
    public void savePng(File pngFile, int imageWidth, int imageHeight) {
        log.info("Generate {}", pngFile.getAbsolutePath());
        pngFile.getParentFile()
                .mkdirs();
        PCMFormat format = info.getAudioInfo()
                .toPCMFormat();
        BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
        int samplesPerPixel = Math.max(1, info.getAudioInfo()
                .getNbSamples() / imageWidth);
        RiffInspectorState state = new RiffInspectorState(image, samplesPerPixel, format);

        try {
            inspect(state);
            ImageIO.write(state.getImage(), "png", pngFile);
        } catch (IOException e) {
            throw new AudioError(e);
        }
    }
}
