package com.hypercube.workshop.midiworkshop.api.sysex.util;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

public class LCDScreenDump {

    private static final int ON_COLOR = 0xFF00FF00;
    private static final int OFF_COLOR = 0xFF000000;

    public static BufferedImage create(int width, int height, boolean[] pixelData) {
        if (pixelData.length < width * height) {
            throw new IllegalArgumentException("Data array too small");
        }

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int[] raster = ((DataBufferInt) image.getRaster()
                .getDataBuffer()).getData();

        for (int i = 0; i < width * height; i++) {
            raster[i] = pixelData[i] ? ON_COLOR : OFF_COLOR;
        }

        return image;
    }
}
