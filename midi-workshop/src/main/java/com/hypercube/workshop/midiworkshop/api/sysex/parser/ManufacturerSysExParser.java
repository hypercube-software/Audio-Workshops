package com.hypercube.workshop.midiworkshop.api.sysex.parser;

import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import com.hypercube.workshop.midiworkshop.api.sysex.device.Device;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceDecodingKey;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceDefinition;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.Manufacturer;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamReader;
import com.hypercube.workshop.midiworkshop.api.sysex.util.SysExReader;
import lombok.extern.slf4j.Slf4j;

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
     * Many synth (especially Alesis Quadrasynth) SYSEX use a complex 7 bit stream that need to be converted to a 8 bit stream given a decoding key
     * <p>It is decoded in blocks of 56 bits, producing 7 bytes</p>
     *
     * @param device
     * @param sysexPayload
     * @return
     */
    public byte[] unpackMidiBuffer(MidiDeviceDefinition device, final byte[] payload) {
        MidiDeviceDecodingKey decodingKey = device.getDecodingKey();
        if (decodingKey == null) {
            return payload;
        }
        final int inputBlockSizeInBytes = 8; // we read 8 "packed" bytes
        final int outputBlockSizeInBytes = 7; //  we produce 7 "unpacked" bytes
        final int decodingStart = decodingKey.getStart(); // lower bound included
        final int decodingEnd = payload.length - decodingKey.getEnd(); // upperbound excluded
        final int packedPayloadSize = decodingEnd - decodingStart; // available bytes to decode in the payload
        final int nbBlocks = (int) Math.ceil((double) packedPayloadSize / inputBlockSizeInBytes); // how many blocks to read
        final int inputSize = nbBlocks * inputBlockSizeInBytes;
        final int outputSize = nbBlocks * outputBlockSizeInBytes;
        final byte[] input = new byte[inputSize];
        final byte[] output = new byte[outputSize];
        // copy the "packed bytes" from the payload into our decoding input buffer
        System.arraycopy(payload, decodingStart, input, 0, packedPayloadSize);
        // Now we can decode
        final BitStreamReader bsr = new BitStreamReader(input);
        for (int block = 0; block < nbBlocks; block++) {
            List<Integer> decodingMap = decodingKey
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
                //System.out.println("blocksize %s: %3d BLOCK [%d-%d-%d] Bit Mask %s = %d Content: %s".formatted(outputBlockSizeInBytes, globalBytePos, block, blockPos, bitPos, getBinaryString(mask, 8), bit,
                //        getBinaryString(output[globalBytePos], 8)));
            }
        }
        return output;
    }
}
