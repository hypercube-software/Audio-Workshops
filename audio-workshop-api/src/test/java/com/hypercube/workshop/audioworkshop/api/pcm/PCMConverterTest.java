package com.hypercube.workshop.audioworkshop.api.pcm;

import com.hypercube.workshop.audioworkshop.api.format.PCMFormat;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PCMConverterTest {
    static int MONO = 1;
    static int STEREO = 2;

    /**
     * Things to know with Java: 0.0 == -0.0 but Double.valueOf(0.0).equals(Double.valueOf(-0.0)) is FALSE !
     * <a href="https://stackoverflow.com/a/14771363/7921777">See that</a>
     * Because of this it is impossible to properly use Junit assertArrayEquals
     */

    void assertSameArrays(double[] expected, double[] actual) {
        assertEquals(expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            if (expected[i] != actual[i]) {
                throw new AssertionError("Element %d not equals: %.28f %.28f".formatted(i, expected[i], actual[i]));
            }
        }
    }

    void assertSameArrays(double[][] expected, double[][] actual) {
        assertEquals(expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            assertSameArrays(expected[i], actual[i]);
        }
    }

    @Test
    void signed16BitsBEStereo() {
        PCMFormat format = new PCMFormat(44100, BitDepth.BIT_DEPTH_16, STEREO, PCMEncoding.SIGNED, ByteOrder.BIG_ENDIAN);
        ByteBuffer data = forgeInput(format, "7F00 7F01 8000 8001 0000 0001");
        double[][] expected = new double[][]{new double[]{0.9921875, -1, 0}, new double[]{0.992218017578125, -0.999969482421875, 0.000030517578125}};
        double[][] output = new double[STEREO][expected[0].length];
        PCMConverter.getPCMtoSampleFunction(format)
                .convert(data, output, expected[0].length, STEREO);
        assertSameArrays(expected, output);
    }

    @Test
    void signed16BitsLEStereo() {
        PCMFormat format = new PCMFormat(44100, BitDepth.BIT_DEPTH_16, STEREO, PCMEncoding.SIGNED, ByteOrder.LITTLE_ENDIAN);
        ByteBuffer data = forgeInput(format, "007F 017F 0080 0180 0000 0100");
        double[][] expected = new double[][]{new double[]{0.9921875, -1, 0},
                new double[]{0.992218017578125, -0.999969482421875, 0.000030517578125}};
        double[][] output = new double[STEREO][expected[0].length];
        PCMConverter.getPCMtoSampleFunction(format)
                .convert(data, output, expected[0].length, STEREO);
        assertSameArrays(expected, output);
    }

    @Test
    void signed16BitsBE() {
        PCMFormat format = new PCMFormat(44100, BitDepth.BIT_DEPTH_16, MONO, PCMEncoding.SIGNED, ByteOrder.BIG_ENDIAN);
        ByteBuffer data = forgeInput(format, "7F00 8000 0000");
        double[][] expected = new double[][]{new double[]{0.9921875, -1, 0}};
        double[][] output = new double[MONO][expected[0].length];
        PCMConverter.getPCMtoSampleFunction(format)
                .convert(data, output, expected[0].length, MONO);
        assertSameArrays(expected, output);
    }

    @Test
    void signed16BitsLE() {
        PCMFormat format = new PCMFormat(44100, BitDepth.BIT_DEPTH_16, MONO, PCMEncoding.SIGNED, ByteOrder.LITTLE_ENDIAN);
        ByteBuffer data = forgeInput(format, "FF7F 0080 0000 FFFF");
        double[][] expected = new double[][]{new double[]{0.999969482421875, -1, 0, -0.000030517578125}};
        double[][] output = new double[MONO][expected[0].length];
        PCMConverter.getPCMtoSampleFunction(format)
                .convert(data, output, expected[0].length, MONO);
        assertSameArrays(expected, output);
    }

    @Test
    void signed24BitsLE() {
        PCMFormat format = new PCMFormat(44100, BitDepth.BIT_DEPTH_24, MONO, PCMEncoding.SIGNED, ByteOrder.LITTLE_ENDIAN);
        ByteBuffer data = forgeInput(format, "000000 7FFFFF 800000 FFFFFF");
        double[][] expected = new double[][]{new double[]{0, 0.9999998807907104, -1, -0.00000011920928955078125}};
        double[][] output = new double[MONO][expected[0].length];
        PCMConverter.getPCMtoSampleFunction(format)
                .convert(data, output, expected[0].length, MONO);
        assertSameArrays(expected, output);
    }

    @Test
    void signed32BitsBE() {
        PCMFormat format = new PCMFormat(44100, BitDepth.BIT_DEPTH_32, MONO, PCMEncoding.SIGNED, ByteOrder.BIG_ENDIAN);
        ByteBuffer data = forgeInput(format, "00000000 7FFFFFFF 80000000 FFFFFFFF");
        double[][] expected = new double[][]{new double[]{-0, -0.9999999995343387, 1, 0.00000000046566128730773926}};
        double[][] output = new double[MONO][expected[0].length];
        PCMConverter.getPCMtoSampleFunction(format)
                .convert(data, output, expected[0].length, MONO);
        assertSameArrays(expected, output);
    }

    @Test
    void signed8BitsLE() {
        PCMFormat format = new PCMFormat(44100, BitDepth.BIT_DEPTH_8, MONO, PCMEncoding.SIGNED, ByteOrder.LITTLE_ENDIAN);
        ByteBuffer data = forgeInput(format, "7F 80 00");
        double[][] expected = new double[][]{new double[]{0.9921875, -1, 0}};
        double[][] output = new double[MONO][expected[0].length];
        PCMConverter.getPCMtoSampleFunction(format)
                .convert(data, output, expected[0].length, MONO);
        assertSameArrays(expected, output);
    }

    @Test
    void unsigned8BitsLE() {
        PCMFormat format = new PCMFormat(44100, BitDepth.BIT_DEPTH_8, MONO, PCMEncoding.UNSIGNED, ByteOrder.LITTLE_ENDIAN);
        ByteBuffer data = forgeInput(format, "00 7F 80 FF");
        double[][] expected = new double[][]{new double[]{-1, -0.0078125, 0, 0.9921875}};
        double[][] output = new double[MONO][expected[0].length];
        PCMConverter.getPCMtoSampleFunction(format)
                .convert(data, output, expected[0].length, MONO);
        assertSameArrays(expected, output);
    }

    @Test
    void unsigned16BitsBE() {
        PCMFormat format = new PCMFormat(44100, BitDepth.BIT_DEPTH_16, MONO, PCMEncoding.UNSIGNED, ByteOrder.BIG_ENDIAN);
        ByteBuffer data = forgeInput(format, "0000 7FFF 8000 FFFF");
        double[][] expected = new double[][]{new double[]{-1, -0.000030517578125, 0, 0.999969482421875}};
        double[][] output = new double[MONO][expected[0].length];
        PCMConverter.getPCMtoSampleFunction(format)
                .convert(data, output, expected[0].length, MONO);
        assertSameArrays(expected, output);
    }

    @Test
    void unsigned24BitsLE() {
        PCMFormat format = new PCMFormat(44100, BitDepth.BIT_DEPTH_24, MONO, PCMEncoding.UNSIGNED, ByteOrder.LITTLE_ENDIAN);
        ByteBuffer data = forgeInput(format, "000000 7FFFFF 800000 FFFFFF");
        double[][] expected = new double[][]{new double[]{-1, -0.00000011920928955078125, 0, 0.9999998807907104}};
        double[][] output = new double[MONO][expected[0].length];
        PCMConverter.getPCMtoSampleFunction(format)
                .convert(data, output, expected[0].length, MONO);
        assertSameArrays(expected, output);
    }

    private ByteBuffer forgeInput(PCMFormat format, String hexa) {
        Integer[] data = Pattern.compile("[0-9A-F]{2}\\s?")
                .matcher(hexa)
                .results()
                .map(r -> Integer.parseInt(r.group()
                        .trim(), 16))
                .toArray(Integer[]::new);
        byte[] result = new byte[data.length];
        IntStream.range(0, data.length)
                .forEach(i -> result[i] = (byte) (data[i] & 0xFF));
        return ByteBuffer.wrap(result)
                .order(format.getByteOrder());
    }
}
