package com.hypercube.workshop.midiworkshop.api.sysex.parser;

import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import com.hypercube.workshop.midiworkshop.api.sysex.device.Device;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceDefinition;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.Manufacturer;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamReader;
import com.hypercube.workshop.midiworkshop.api.sysex.util.SysExReader;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;

import static com.hypercube.workshop.midiworkshop.api.sysex.util.SysExConstants.SYSEX_END;

@Slf4j
public class ManufacturerSysExParser {
    private List<Integer> decodingKey;

    /**
     * Parse a SysEx buffer and reconstruct the device memory
     *
     * @param manufacturer Device's manufacturer
     * @param buffer       SYSEX buffer
     * @return the Device with its memory properly set
     */
    public Device parse(Manufacturer manufacturer, SysExReader buffer) {
        throw new MidiError("Not Implemented");
    }

    public void parseIdentityResponse(Manufacturer manufacturer, int deviceId, SysExReader sysExReader) {
        StringBuilder sb = new StringBuilder();
        for (; ; ) {
            int value = sysExReader.getByte();
            if (value == SYSEX_END) {
                break;
            }
            if (!sb.isEmpty()) {
                sb.append(" ");
            }
            sb.append("%02X".formatted(value));
        }
        log.info("Identity Response for {} (device id: {}): {}", manufacturer.name(), deviceId, sb);
    }

    /**
     * Alesis Quadrasynth SYSEX use a complex 7 bit stream that need to be converted to a 8 bit stream given a decoding key
     * <p>It is decoded in blocks of 56 bits, producing 7 bytes</p>
     *
     * @param device
     * @param sysexPayload
     * @return
     */
    public byte[] unpackMidiBuffer(MidiDeviceDefinition device, byte[] sysexPayload) {
        if (device.getDecodingKey() == null) {
            return sysexPayload;
        }
        final int inputBlockSizeInBytes = 8;
        final int outputBlockSizeInBytes = 7;
        final BitStreamReader bsr = new BitStreamReader(sysexPayload);
        bsr.skipBytes(device.getDecodingKey()
                .getStart());
        final int nbBlocks = sysexPayload.length / inputBlockSizeInBytes;
        final int outputSize = nbBlocks * outputBlockSizeInBytes;
        final byte[] output = new byte[outputSize];
        int remain = sysexPayload.length % inputBlockSizeInBytes;
        if (remain != 0) {
            sysexPayload = Arrays.copyOfRange(sysexPayload, 0, sysexPayload.length + remain);
            //throw new RuntimeException("Invalid size, should be multiple of %d bytes".formatted(inputBlockSizeInBytes));
        }
        for (int block = 0; block < nbBlocks; block++) {
            List<Integer> decodingMap = device.getDecodingKey()
                    .getMapping();
            for (int bitIndex = 0; bitIndex < decodingMap
                    .size(); bitIndex++) {
                int bit = bsr.readBit();
                int blockbitpos = decodingMap.get(bitIndex);
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
}
