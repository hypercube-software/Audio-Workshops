package com.hypercube.workshop.midiworkshop.common.sysex.util;

import com.hypercube.workshop.midiworkshop.common.sysex.checksum.DefaultChecksum;
import com.hypercube.workshop.midiworkshop.common.sysex.library.device.MidiControllerValue;
import com.hypercube.workshop.midiworkshop.common.sysex.library.request.MidiRequest;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;

public record SysExTemplate(MidiRequest midiRequest, Integer msbIndex, Integer lsbIndex, CheckSumDef checkSumDef,
                            byte[] payload, SysExChecksum sysExChecksum) {
    public static SysExTemplate of(MidiRequest midiRequest) {
        String payloadTemplate = midiRequest.getValue();
        List<String> bytes = Arrays.stream(payloadTemplate.split(" "))
                .toList();
        ByteArrayOutputStream payload = new ByteArrayOutputStream();
        Integer msbIndex = null;
        Integer lsbIndex = null;
        CheckSumDef checkSumDef = null;
        for (String chunk : bytes) {
            int currentPayloadPosition = payload.size();
            if (chunk
                    .equals("msb")) {
                msbIndex = currentPayloadPosition;
                payload.write(0);
            } else if (chunk
                    .equals("lsb")) {
                lsbIndex = currentPayloadPosition;
                payload.write(0);
            } else if (chunk.startsWith("CK")) {
                checkSumDef = new CheckSumDef();
                checkSumDef.setPosition(currentPayloadPosition);
                int size = Integer.parseInt(chunk.substring(2));
                checkSumDef.setSize(size);
                payload.write(0);
            } else {
                for (int z = 0; z < chunk.length(); z += 2) {
                    String value = chunk.substring(z, z + 2);
                    payload.write(Integer.parseInt(value, 16));
                }
            }
        }
        SysExChecksum checksum = checkSumDef != null ? new DefaultChecksum() : null;
        return new SysExTemplate(midiRequest, msbIndex, lsbIndex, checkSumDef, payload.toByteArray(), checksum);
    }

    public byte[] forgePayload(MidiControllerValue value) {
        if (msbIndex != null) {
            payload[msbIndex] = (byte) value.msb();
        }
        if (lsbIndex != null) {
            payload[lsbIndex] = (byte) value.lsb();
        }
        if (checkSumDef != null) {
            sysExChecksum.reset();
            int start = checkSumDef.getStartPosition();
            int end = checkSumDef.getEndPosition();
            for (int i = start; i <= end; i++) {
                sysExChecksum.update(payload[i]);
            }
            payload[checkSumDef.getPosition()] = (byte) sysExChecksum.getValue();
        }
        return payload;
    }
}
