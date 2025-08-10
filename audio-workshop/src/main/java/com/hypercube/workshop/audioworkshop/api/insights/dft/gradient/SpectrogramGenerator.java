package com.hypercube.workshop.audioworkshop.api.insights.dft.gradient;

import com.hypercube.workshop.audioworkshop.api.insights.dft.DFTResult;
import lombok.RequiredArgsConstructor;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
public class SpectrogramGenerator {
    private final GradientGenerator gradient = GradientGenerator.fftGradient();

    public void generate(int bitdepth, List<DFTResult> signal, File pngFile, int outputWidth, int outputHeight) throws IOException {
        // dBFS = 20 * log( [sample level] / [max level] )
        double minDbSPL = 20 * Math.log10(1.0 / (double) ((1 << bitdepth) / 2));
        double maxDbSPL = 0;

        if (!signal.isEmpty()) {
            int width = signal.size();
            int height = signal.get(0)
                    .getNbBin();
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < width; x++) {
                DFTResult r = signal.get(x);
                for (int y = 0; y < r.getNbBin(); y++) {
                    double percent = dbToPercent(minDbSPL, maxDbSPL, r.getMagnitudes()[y]);
                    var color = gradient.interpolate(percent);
                    image.setRGB(x, height - y - 1, color.toRGB24());
                }
            }

            Image scaledInstance = image.getScaledInstance(outputWidth, outputHeight, BufferedImage.SCALE_AREA_AVERAGING);
            ImageIO.write(toBufferedImage(scaledInstance), "png", pngFile);
        }
    }

    public BufferedImage toBufferedImage(final Image image) {
        if (image instanceof BufferedImage)
            return (BufferedImage) image;
        if (image instanceof VolatileImage)
            return ((VolatileImage) image).getSnapshot();
        final BufferedImage buffImg = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_RGB);
        final Graphics2D g2 = buffImg.createGraphics();
        g2.drawImage(image, null, null);
        g2.dispose();
        return buffImg;
    }

    private double dbToPercent(double minDbSPL, double maxDbSPL, double magnitude) {
        double result = magnitude;
        result = Math.max(result, minDbSPL);
        result = Math.min(result, maxDbSPL);
        result = (result - minDbSPL) / (maxDbSPL - minDbSPL);
        return result * 100.0;
    }
}
