package com.hypercube.workshop.midiworkshop.common.sysex.manufacturer.alesis;

import com.hypercube.workshop.midiworkshop.common.errors.MidiError;
import com.hypercube.workshop.midiworkshop.common.sysex.device.Device;
import com.hypercube.workshop.midiworkshop.common.sysex.manufacturer.Manufacturer;
import com.hypercube.workshop.midiworkshop.common.sysex.parser.ManufacturerSysExParser;
import com.hypercube.workshop.midiworkshop.common.sysex.util.BitStreamReader;
import com.hypercube.workshop.midiworkshop.common.sysex.util.SysExReader;

import java.util.Arrays;
import java.util.List;

public class AlesisSysExParser extends ManufacturerSysExParser {
    private static final String characterTable = " !\"#$%&’()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[¥]^_`abcdefghijklmnopqrstuvwxyz{|}→←";

    /**
     * This key is the right one to use. I found it after hours of reverse engineering
     */
    private static final String officialKeyReverted = """
            0 A1 A2 A3 A4 A5 A6 A7
            0 B2 B3 B4 B5 B6 B7 A0
            0 C3 C4 C5 C6 C7 B0 B1
            0 D4 D5 D6 D7 C0 C1 C2
            0 E5 E6 E7 D0 D1 D2 D3
            0 F6 F7 E0 E1 E2 E3 E4
            0 G7 F0 F1 F2 F3 F4 F5
            0 G0 G1 G2 G3 G4 G5 G6
            """;
    /**
     * This key can be found in the service manual and does not work
     */
    private static final String officialKey = """
            0 A6 A5 A4 A3 A2 A1 A0
            0 B5 B4 B3 B2 B1 B0 A7
            0 C4 C3 C2 C1 C0 B7 B6
            0 D3 D2 D1 D0 C7 C6 C5
            0 E2 E1 E0 D7 D6 D5 D4
            0 F1 F0 E7 E6 E5 E4 E3
            0 G0 F7 F6 F5 F4 F3 F2
            0 G7 G6 G5 G4 G3 G2 G1
            """;

    private List<Integer> decodingKey = getDecodingKey();

    int getCharCode(Character c) {
        return characterTable.indexOf(c) & 0x7F; // 6 bit code
    }

    Character getChar(int code) {
        return characterTable.charAt(code);
    }

    public void dumpASCIITable() {
        for (int i = 0; i < characterTable.length(); i++) {
            String padded = BitStreamReader.getBinary7(i);
            String reverted = BitStreamReader.getBinary7Inverted(i);
            System.out.println("Code of %2d %c: %s reverted: %s".formatted(i, characterTable.charAt(i), padded, reverted));
        }
    }

    /**
     * Alesis Quadrasynth SYSEX use a complex 7 bit stream that need to be converted to a 8 bit stream given a decoding key
     * <p>It is decoded in blocks of 56 bits, producing 7 bytes</p>
     *
     * @param sysexPayload
     * @return
     */
    public byte[] unpackAlesisMidiBuffer(byte[] sysexPayload) {
        final int inputBlockSizeInBytes = 8;
        final int outputBlockSizeInBytes = 7;
        final BitStreamReader bsr = new BitStreamReader(sysexPayload);
        final int nbBlocks = sysexPayload.length / inputBlockSizeInBytes;
        final int outputSize = nbBlocks * outputBlockSizeInBytes;
        final byte[] output = new byte[outputSize];
        if (sysexPayload.length % inputBlockSizeInBytes != 0) {
            throw new RuntimeException("Invalid size, should be multiple of %d bytes".formatted(inputBlockSizeInBytes));
        }
        for (int block = 0; block < nbBlocks; block++) {
            for (int bitIndex = 0; bitIndex < decodingKey.size(); bitIndex++) {
                int bit = bsr.readBit();
                int blockbitpos = decodingKey.get(bitIndex);
                if (blockbitpos == -1) {
                    //System.out.println("skip\n");
                    continue;
                }
                int blockPos = blockbitpos / 8;
                int bitPos = blockbitpos % 8;
                int mask = 1 << bitPos;
                int globalBytePos = blockPos + (block * outputBlockSizeInBytes);
                if (bit == 1) {
                    output[globalBytePos] = (byte) (output[globalBytePos] | mask);
                }
                /*System.out.println("blocksize %s: %3d BLOCK [%d-%d-%d] Bit Mask %s = %d Content: %s".formatted(outputBlockSize, globalBytePos, block, blockPos, bitPos, getBinaryString(mask, 8), bit,
                        getBinaryString(output[globalBytePos], 8)));*/
            }
        }
        return output;
    }

    private static List<Integer> getDecodingKey() {
        List<Integer> key = Arrays.stream(officialKeyReverted.trim()
                        .split("\\s"))
                .map(v -> {
                    if (v.equals("0")) {
                        return -1;
                    } else {
                        int offset = v.charAt(0) - 'A';
                        int bit = v.charAt(1) - '0';
                        return bit + offset * 8;
                    }
                })
                .toList();
        if (key.size() != 64) {
            throw new MidiError("Invalid decoding key, should have 64 bits");
        }
        return key;
    }

    @Override
    public Device parse(Manufacturer manufacturer, SysExReader buffer) {
        throw new MidiError("Not Implemented yet");
    }
}
