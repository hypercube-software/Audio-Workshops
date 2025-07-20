package com.hypercube.workshop.midiworkshop.common.sysex.util;

import com.hypercube.workshop.midiworkshop.common.sysex.checksum.DefaultChecksum;
import com.hypercube.workshop.midiworkshop.common.sysex.library.device.MidiControllerValue;
import com.hypercube.workshop.midiworkshop.common.sysex.library.request.MidiRequest;

import java.util.Arrays;
import java.util.List;

public record SysExTemplate(MidiRequest midiRequest, Integer msbIndex, Integer lsbIndex, CheckSumDef checkSumDef,
                            byte[] payload, SysExChecksum sysExChecksum) {
    public static SysExTemplate of(MidiRequest midiRequest) {
        String payloadTemplate = midiRequest.getValue();
        List<String> bytes = Arrays.stream(payloadTemplate.split(" "))
                .toList();
        byte[] payload = new byte[bytes.size()];
        Integer msbIndex = null;
        Integer lsbIndex = null;
        CheckSumDef checkSumDef = null;
        for (int i = 0; i < bytes.size(); i++) {
            String chunk = bytes.get(i);
            if (chunk
                    .equals("msb")) {
                msbIndex = i;
                payload[i] = 0;
            } else if (chunk
                    .equals("lsb")) {
                lsbIndex = i;
                payload[i] = 0;
            } else if (chunk.startsWith("CK")) {
                checkSumDef = new CheckSumDef();
                checkSumDef.setPosition(i);
                checkSumDef.setSize(Integer.parseInt(chunk.substring(2)));
            } else {
                payload[i] = (byte) Integer.parseInt(chunk, 16);
            }
        }
        SysExChecksum checksum = checkSumDef != null ? new DefaultChecksum() : null;
        return new SysExTemplate(midiRequest, msbIndex, lsbIndex, checkSumDef, payload, checksum);
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
