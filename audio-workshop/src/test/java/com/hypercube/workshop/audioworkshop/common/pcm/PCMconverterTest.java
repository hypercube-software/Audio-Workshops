package com.hypercube.workshop.audioworkshop.common.pcm;

import com.hypercube.workshop.audioworkshop.common.line.AudioLineFormat;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class PCMconverterTest {
    static int MONO = 1;
    static int STEREO = 2;

    @Test
    void signed16BitsBEStereo() {
        AudioLineFormat format = new AudioLineFormat(250, 44100, BitDepth.BIT_DEPTH_16, STEREO, PCMEncoding.SIGNED, ByteOrder.BIG_ENDIAN);
        ByteBuffer data = forgeInput(format, "7F00 7F01 8000 8001 0000 0001");
        float[][] expected = new float[][]{new float[]{0.9921875F, -1.0F, 0.0F}, new float[]{0.992218F, -0.9999695F, 3.0517578E-5F}};
        float[][] output = new float[STEREO][expected[0].length];
        PCMconverter.getPCMtoSampleFunction(format)
                .convert(data, output, expected[0].length, STEREO);
        assertArrayEquals(expected, output);
    }

    @Test
    void signed16BitsLEStereo() {
        AudioLineFormat format = new AudioLineFormat(250, 44100, BitDepth.BIT_DEPTH_16, STEREO, PCMEncoding.SIGNED, ByteOrder.LITTLE_ENDIAN);
        ByteBuffer data = forgeInput(format, "007F 017F 0080 0180 0000 0100");
        float[][] expected = new float[][]{new float[]{0.9921875F, -1.0F, 0.0F}, new float[]{0.992218F, -0.9999695F, 3.0517578E-5F}};
        float[][] output = new float[STEREO][expected[0].length];
        PCMconverter.getPCMtoSampleFunction(format)
                .convert(data, output, expected[0].length, STEREO);
        assertArrayEquals(expected, output);
    }

    @Test
    void signed16BitsBE() {
        AudioLineFormat format = new AudioLineFormat(250, 44100, BitDepth.BIT_DEPTH_16, MONO, PCMEncoding.SIGNED, ByteOrder.BIG_ENDIAN);
        ByteBuffer data = forgeInput(format, "7F00 8000 0000");
        float[][] expected = new float[][]{new float[]{0.9921875F, -1.0F, 0.0F}};
        float[][] output = new float[MONO][expected[0].length];
        PCMconverter.getPCMtoSampleFunction(format)
                .convert(data, output, expected[0].length, MONO);
        assertArrayEquals(expected, output);
    }

    @Test
    void signed16BitsLE() {
        AudioLineFormat format = new AudioLineFormat(250, 44100, BitDepth.BIT_DEPTH_16, MONO, PCMEncoding.SIGNED, ByteOrder.LITTLE_ENDIAN);
        ByteBuffer data = forgeInput(format, "FF7F 0080 0000 FFFF");
        float[][] expected = new float[][]{new float[]{0.9999695F, -1.0F, 0.0F, -3.0517578E-5F}};
        float[][] output = new float[MONO][expected[0].length];
        PCMconverter.getPCMtoSampleFunction(format)
                .convert(data, output, expected[0].length, MONO);
        assertArrayEquals(expected, output);
    }

    @Test
    void signed24BitsLE() {
        AudioLineFormat format = new AudioLineFormat(250, 44100, BitDepth.BIT_DEPTH_24, MONO, PCMEncoding.SIGNED, ByteOrder.LITTLE_ENDIAN);
        ByteBuffer data = forgeInput(format, "000000 7FFFFF 800000 FFFFFF");
        float[][] expected = new float[][]{new float[]{0.0F, 0.9999999F, -1.0F, -1.1920929E-7F}};
        float[][] output = new float[MONO][expected[0].length];
        PCMconverter.getPCMtoSampleFunction(format)
                .convert(data, output, expected[0].length, MONO);
        assertArrayEquals(expected, output);
    }

    @Test
    void signed32BitsBE() {
        AudioLineFormat format = new AudioLineFormat(250, 44100, BitDepth.BIT_DEPTH_32, MONO, PCMEncoding.SIGNED, ByteOrder.BIG_ENDIAN);
        ByteBuffer data = forgeInput(format, "00000000 7FFFFFFF 80000000 FFFFFFFF");
        float[][] expected = new float[][]{new float[]{-0.0F, -1.0F, 1.0F, 4.656613E-10F}};
        float[][] output = new float[MONO][expected[0].length];
        PCMconverter.getPCMtoSampleFunction(format)
                .convert(data, output, expected[0].length, MONO);
        assertArrayEquals(expected, output);
    }

    @Test
    void signed8BitsLE() {
        AudioLineFormat format = new AudioLineFormat(250, 44100, BitDepth.BIT_DEPTH_8, MONO, PCMEncoding.SIGNED, ByteOrder.LITTLE_ENDIAN);
        ByteBuffer data = forgeInput(format, "7F 80 00");
        float[][] expected = new float[][]{new float[]{0.9921875F, -1.0F, 0.0F}};
        float[][] output = new float[MONO][expected[0].length];
        PCMconverter.getPCMtoSampleFunction(format)
                .convert(data, output, expected[0].length, MONO);
        assertArrayEquals(expected, output);
    }

    @Test
    void unsigned8BitsLE() {
        AudioLineFormat format = new AudioLineFormat(250, 44100, BitDepth.BIT_DEPTH_8, MONO, PCMEncoding.UNSIGNED, ByteOrder.LITTLE_ENDIAN);
        ByteBuffer data = forgeInput(format, "00 7F 80 FF");
        float[][] expected = new float[][]{new float[]{-1.0F, -0.0078125F, 0.0F, 0.9921875F}};
        float[][] output = new float[MONO][expected[0].length];
        PCMconverter.getPCMtoSampleFunction(format)
                .convert(data, output, expected[0].length, MONO);
        assertArrayEquals(expected, output);
    }

    @Test
    void unsigned16BitsBE() {
        AudioLineFormat format = new AudioLineFormat(250, 44100, BitDepth.BIT_DEPTH_16, MONO, PCMEncoding.UNSIGNED, ByteOrder.BIG_ENDIAN);
        ByteBuffer data = forgeInput(format, "0000 7FFF 8000 FFFF");
        float[][] expected = new float[][]{new float[]{-1.0F, -3.0517578E-5F, 0.0F, 0.9999695F}};
        float[][] output = new float[MONO][expected[0].length];
        PCMconverter.getPCMtoSampleFunction(format)
                .convert(data, output, expected[0].length, MONO);
        assertArrayEquals(expected, output);
    }

    @Test
    void unsigned24BitsLE() {
        AudioLineFormat format = new AudioLineFormat(250, 44100, BitDepth.BIT_DEPTH_24, MONO, PCMEncoding.UNSIGNED, ByteOrder.LITTLE_ENDIAN);
        ByteBuffer data = forgeInput(format, "000000 7FFFFF 800000 FFFFFF");
        float[][] expected = new float[][]{new float[]{-1.0F, -1.1920929E-7F, 0.0F, 0.9999999F}};
        float[][] output = new float[MONO][expected[0].length];
        PCMconverter.getPCMtoSampleFunction(format)
                .convert(data, output, expected[0].length, MONO);
        assertArrayEquals(expected, output);
    }

    private ByteBuffer forgeInput(AudioLineFormat format, String hexa) {
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
