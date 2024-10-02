package com.hypercube.workshop.audioworkshop.common.insights.dft.gradient;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GradientGeneratorTest {

    @ParameterizedTest
    @CsvSource({"50,#7F7F7F", "0,#000000", "100,#FFFFFF"})
    void interpolate(String percent, String expected) {
        // GIVEN
        var colors = List.of(GradientMarker.fromHexa("#000", 0), GradientMarker.fromHexa("#FFF", 100));
        GradientGenerator gradientGenerator = new GradientGenerator(colors);
        // WHEN
        var actual = gradientGenerator.interpolate(Double.parseDouble(percent));
        // THEN
        assertEquals(expected, actual.toString());
    }

    @ParameterizedTest
    @CsvSource({"10,#232B3F", "40,#8CACFF", "60,#B2BAC8", "80,#D8C891", "100,#FFD65B"})
    void interpolateWith3Markers(String percent, String expected) {
        // GIVEN
        var colors = List.of(GradientMarker.fromHexa("#000", 0), GradientMarker.fromHexa("#8CACFF", 40), GradientMarker.fromHexa("#FFD65B", 100));
        GradientGenerator gradientGenerator = new GradientGenerator(colors);
        // WHEN
        var actual = gradientGenerator.interpolate(Double.parseDouble(percent));
        // THEN
        assertEquals(expected, actual.toString());
    }

    @Test
    void savePng() throws IOException {
        // GIVEN
        GradientGenerator gradientGenerator = GradientGenerator.fftGradient();
        // WHEN
        File gradientFile = new File("target/gradient.png");
        gradientGenerator.saveGradientAsPng(800, 40, gradientFile);
        // THEN
        assertTrue(gradientFile.exists());
    }
}