package com.hypercube.workshop.audioworkshop.api.insights.dft.gradient;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GradientColorTest {

    @ParameterizedTest
    @CsvSource({"#FAB,#FFAABB", "#123,#112233", "#FFAABB,#FFAABB", "#112233,#112233"})
    void fromHexa(String input, String expected) {
        // When
        String actual = GradientColor.fromHexa(input)
                .toString();
        // Then
        assertEquals(expected, actual);
    }
}