package com.hypercube.workshop.audioworkshop.common.pcm;

import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class PCMconverterTest {
    static int MONO = 1;
    static int STEREO = 2;

    @Test
    void signed16BitsLEStereo() {
        byte[] data = forgeInput("7F00 7F01 8000 8001 0000 0001");
        float[][] expected = new float[][]{new float[]{0.9921875F, -1.0F, 0.0F}, new float[]{0.992218F, -0.9999695F, 3.0517578E-5F}};
        float[][] output = new float[STEREO][expected[0].length];
        PCMconverter.convert(data, output, expected[0].length, STEREO, BitDepth.BIT_DEPTH_16, true, true);
        assertArrayEquals(expected, output);
    }

    @Test
    void signed16BitsLE() {
        byte[] data = forgeInput("7F00 8000 0000");
        float[][] expected = new float[][]{new float[]{0.9921875F, -1.0F, 0.0F}};
        float[][] output = new float[MONO][expected[0].length];
        PCMconverter.convert(data, output, expected[0].length, MONO, BitDepth.BIT_DEPTH_16, true, true);
        assertArrayEquals(expected, output);
    }

    @Test
    void signed16BitsBE() {
        byte[] data = forgeInput("FF7F 0080 0000 FFFF");
        float[][] expected = new float[][]{new float[]{0.9999695F, -1.0F, 0.0F, -3.0517578E-5F}};
        float[][] output = new float[MONO][expected[0].length];
        PCMconverter.convert(data, output, expected[0].length, MONO, BitDepth.BIT_DEPTH_16, false, true);
        assertArrayEquals(expected, output);
    }

    @Test
    void signed24BitsLE() {
        byte[] data = forgeInput("000000 7FFFFF 800000 FFFFFF");
        float[][] expected = new float[][]{new float[]{0.0F, 0.9999999F, -1.0F, -1.1920929E-7F}};
        float[][] output = new float[MONO][expected[0].length];
        PCMconverter.convert(data, output, expected[0].length, MONO, BitDepth.BIT_DEPTH_24, true, true);
        assertArrayEquals(expected, output);
    }

    @Test
    void signed32BitsLE() {
        byte[] data = forgeInput("00000000 7FFFFFFF 80000000 FFFFFFFF");
        float[][] expected = new float[][]{new float[]{-0.0F, -1.0F, 1.0F, 4.656613E-10F}};
        float[][] output = new float[MONO][expected[0].length];
        PCMconverter.convert(data, output, expected[0].length, MONO, BitDepth.BIT_DEPTH_32, true, true);
        assertArrayEquals(expected, output);
    }

    @Test
    void signed8BitsLE() {
        byte[] data = forgeInput("7F 80 00");
        float[][] expected = new float[][]{new float[]{0.9921875F, -1.0F, 0.0F}};
        float[][] output = new float[MONO][expected[0].length];
        PCMconverter.convert(data, output, expected[0].length, MONO, BitDepth.BIT_DEPTH_8, true, true);
        assertArrayEquals(expected, output);
    }

    @Test
    void unsigned8BitsLE() {
        byte[] data = forgeInput("00 7F 80 FF");
        float[][] expected = new float[][]{new float[]{-1.0F, -0.0078125F, 0.0F, 0.9921875F}};
        float[][] output = new float[MONO][expected[0].length];
        PCMconverter.convert(data, output, expected[0].length, MONO, BitDepth.BIT_DEPTH_8, true, false);
        assertArrayEquals(expected, output);
    }

    @Test
    void unsigned16BitsLE() {
        byte[] data = forgeInput("0000 7FFF 8000 FFFF");
        float[][] expected = new float[][]{new float[]{-1.0F, -3.0517578E-5F, 0.0F, 0.9999695F}};
        float[][] output = new float[MONO][expected[0].length];
        PCMconverter.convert(data, output, expected[0].length, MONO, BitDepth.BIT_DEPTH_16, true, false);
        assertArrayEquals(expected, output);
    }

    @Test
    void unsigned24BitsLE() {
        byte[] data = forgeInput("000000 7FFFFF 800000 FFFFFF");
        float[][] expected = new float[][]{new float[]{-1.0F, -1.1920929E-7F, 0.0F, 0.9999999F}};
        float[][] output = new float[MONO][expected[0].length];
        PCMconverter.convert(data, output, expected[0].length, MONO, BitDepth.BIT_DEPTH_24, true, false);
        assertArrayEquals(expected, output);
    }

    private byte[] forgeInput(String hexa) {
        Integer[] data = Pattern.compile("[0-9A-F]{2}\\s?")
                .matcher(hexa)
                .results()
                .map(r -> Integer.parseInt(r.group()
                        .trim(), 16))
                .toArray(Integer[]::new);
        byte[] result = new byte[data.length];
        IntStream.range(0, data.length)
                .forEach(i -> result[i] = (byte) (data[i] & 0xFF));
        return result;
    }
}
