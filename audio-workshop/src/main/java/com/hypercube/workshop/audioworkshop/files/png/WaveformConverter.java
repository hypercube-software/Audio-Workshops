package com.hypercube.workshop.audioworkshop.files.png;

import com.hypercube.workshop.audioworkshop.common.errors.AudioError;
import com.hypercube.workshop.audioworkshop.common.line.AudioLineFormat;
import com.hypercube.workshop.audioworkshop.common.pcm.BitDepth;
import com.hypercube.workshop.audioworkshop.common.pcm.PCMEncoding;
import com.hypercube.workshop.audioworkshop.common.pcm.PCMconverter;
import com.hypercube.workshop.audioworkshop.files.riff.RiffAudioInfo;
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
import java.nio.ByteOrder;
import java.util.Locale;

@Slf4j
@RequiredArgsConstructor
public class WaveformConverter {
    private final RiffReader riffReader;
    private final RiffFileInfo info;
    private WaveformConverterState state;

    public void saveCsv(File csvFile) {
        log.info("Generate {}", csvFile.getAbsolutePath());
        csvFile.getParentFile()
                .mkdirs();
        RiffAudioInfo format = info.getAudioInfo();
        int nbChannels = format.getNbChannels();
        var pcmConverter = PCMconverter.getPCMtoSampleFunction(new AudioLineFormat(0,
                format.getBitPerSample(), BitDepth.valueOf(format.getBitPerSample()), nbChannels, PCMEncoding.SIGNED, ByteOrder.LITTLE_ENDIAN));
        int bufferSizeInBytes = format.getFrameSizeInBytes() * 1024;
        int bufferSizeInSamples = bufferSizeInBytes / format.getFrameSizeInBytes();
        float[][] samples = new float[nbChannels][bufferSizeInSamples];
        try (PrintWriter out = new PrintWriter(new FileOutputStream(csvFile))) {
            out.println("Sep=,");
            for (int c = 0; c < nbChannels; c++) {
                if (c > 0) {
                    out.print(",");
                }
                out.print(WaveChannels.valueOfOrdinal(c)
                        .getShortTitle());
            }
            out.println();
            riffReader.streamChunk(info.getDataChunk(), bufferSizeInBytes, (pcmBuffer, size) -> {
                int nbSamples = size / format.getFrameSizeInBytes();
                checkBufferSize(size, format);

                pcmConverter.convert(pcmBuffer, samples, nbSamples, nbChannels);
                for (int s = 0; s < nbSamples; s++) {
                    for (int c = 0; c < nbChannels; c++) {
                        if (c > 0) {
                            out.print(",");
                        }
                        out.print(String.format(Locale.ENGLISH, "%f", samples[c][s]));
                    }
                    out.println();
                }
            });
        } catch (IOException e) {
            throw new AudioError(e);
        }
    }

    private static void checkBufferSize(int size, RiffAudioInfo format) {
        if (size % format.getFrameSizeInBytes() != 0)
            throw new IllegalArgumentException("buffer size %d is not a multiple of frame size %d in bytes".formatted(size, format.getFrameSizeInBytes()));
    }

    public void savePng(File pngFile, int imageWidth, int imageHeight) {
        log.info("Generate {}", pngFile.getAbsolutePath());
        pngFile.getParentFile()
                .mkdirs();
        RiffAudioInfo format = info.getAudioInfo();
        int nbChannels = format.getNbChannels();
        var pcmConverter = PCMconverter.getPCMtoSampleFunction(new AudioLineFormat(0,
                format.getBitPerSample(), BitDepth.valueOf(format.getBitPerSample()), nbChannels, PCMEncoding.SIGNED, ByteOrder.LITTLE_ENDIAN));

        BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
        int samplesPerPixel = Math.max(1, format.getNbSamples() / imageWidth);
        int bufferSizeInBytes = format.getFrameSizeInBytes() * 1024;
        int bufferSizeInSamples = bufferSizeInBytes / format.getFrameSizeInBytes();
        float[][] samples = new float[nbChannels][bufferSizeInSamples];
        state = new WaveformConverterState(image, samplesPerPixel, format);
        try {
            riffReader.streamChunk(info.getDataChunk(), bufferSizeInBytes, (pcmBuffer, size) -> {
                int nbSamples = size / format.getFrameSizeInBytes();
                checkBufferSize(size, format);
                pcmConverter.convert(pcmBuffer, samples, nbSamples, nbChannels);
                state.updateImage(samples, nbSamples);
            });
            ImageIO.write(state.getImage(), "png", pngFile);
        } catch (IOException e) {
            throw new AudioError(e);
        }
    }
}
