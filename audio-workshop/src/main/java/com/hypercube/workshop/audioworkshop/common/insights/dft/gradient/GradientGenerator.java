package com.hypercube.workshop.audioworkshop.common.insights.dft.gradient;

import lombok.RequiredArgsConstructor;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
public class GradientGenerator {
    private final List<GradientMarker> markers;

    public static GradientGenerator fftGradient() {
        return new GradientGenerator(List.of(GradientMarker.fromHexa("#000000", 0),
                GradientMarker.fromHexa("#000000", 20),
                GradientMarker.fromHexa("#6D005F", 30),
                GradientMarker.fromHexa("#FF0000", 70),
                GradientMarker.fromHexa("#FFE900", 100)
        ));
    }

    public void saveGradientAsPng(int width, int height, File filename) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        var graphics = image.getGraphics();
        for (int x = 0; x < width; x++) {
            double percent = x * 100.0 / width;
            var color = interpolate(percent);
            graphics.setColor(new Color(color.r(), color.g(), color.b()));
            graphics.drawLine(x, 0, x, height);
        }
        ImageIO.write(image, "png", filename);
    }

    public GradientColor interpolate(double percent) {
        for (int i = 0; i < markers.size() - 1; i++) {
            GradientMarker low = markers.get(i);
            GradientMarker high = markers.get(i + 1);
            if (percent >= low.percent() && percent <= high.percent()) {
                return interpolate(low, high, percent);
            }
        }
        throw new IllegalArgumentException("Unexpected percentage: " + percent);
    }

    private GradientColor interpolate(GradientMarker low, GradientMarker high, double percent) {
        return new GradientColor(interpolate(low.percent(), low.color()
                .r(), high.percent(), high.color()
                .r(), percent),
                interpolate(low.percent(), low.color()
                        .g(), high.percent(), high.color()
                        .g(), percent),
                interpolate(low.percent(), low.color()
                        .b(), high.percent(), high.color()
                        .b(), percent));
    }

    /**
     * Taken from <a href="https://en.wikipedia.org/wiki/Linear_interpolation">here</a>
     */
    private int interpolate(double x0, double y0, double x1, double y1, double x) {
        return (int) (y0 * (x1 - x) / (x1 - x0) + y1 * (x - x0) / (x1 - x0));
    }
}
