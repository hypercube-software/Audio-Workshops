package com.hypercube.workshop.midiworkshop.api.sysex.library.io.response;

import com.hypercube.workshop.midiworkshop.api.CustomMidiEvent;
import com.hypercube.workshop.midiworkshop.api.devices.MidiInDevice;
import com.hypercube.workshop.midiworkshop.api.listener.MidiListener;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/*
This class is designed to receive multiple SysEx and return the whole thing in a single buffer
 */
@Slf4j
public class SysExAggregatorListener implements MidiListener {
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private final Phaser responseReceived = new Phaser(1);

    @Override
    public void onEvent(MidiInDevice device, CustomMidiEvent event) {
        try {
            buffer.write(event.getMessage()
                    .getMessage());
            responseReceived.arrive();
        } catch (IOException e) {
            log.error("Unexpected error", e);
        }
    }

    public int getCurrentSize() {
        return buffer.size();
    }

    public byte[] getSysExResponse() {
        return buffer.toByteArray();
    }

    public void reset() {
        buffer.reset();
    }

    public Optional<byte[]> waitResponse(int timeoutInSec) throws InterruptedException {
        int currentPhase = responseReceived.getPhase();
        try {
            int newPhase = responseReceived.awaitAdvanceInterruptibly(currentPhase, timeoutInSec, TimeUnit.SECONDS);
            byte[] sysExResponse = getSysExResponse();
            /*String hexDump = IntStream.range(0, sysExResponse.length)
                    .mapToObj(i -> String.format("%02X", sysExResponse[i]))
                    .collect(Collectors.joining(" "));
            log.info("Received {}", hexDump);*/
            reset();
            return Optional.ofNullable(sysExResponse);
        } catch (TimeoutException e) {
            return Optional.empty();

        }
    }
}
