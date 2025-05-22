package com.hypercube.workshop.midiworkshop.common;

import com.hypercube.workshop.midiworkshop.common.listener.MidiListener;
import lombok.extern.slf4j.Slf4j;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class MultiMidiInDevice extends MidiInDevice {
    private final List<MidiInDevice> devices;
    private final AtomicInteger runningListeners = new AtomicInteger(0);

    public MultiMidiInDevice(List<MidiInDevice> devices) {
        super(null);
        this.devices = devices;
    }

    @Override
    public void open() {
        for (var device : devices) {
            device.open();
        }
    }

    @Override
    public boolean isOpen() {
        boolean allOpen = true;
        for (var device : devices) {
            allOpen &= device.isOpen();
        }
        return allOpen;
    }

    @Override
    public void close() throws IOException {
        for (var device : devices) {
            device.close();
        }
    }

    @Override
    public String getName() {
        var name = new StringBuilder();
        for (var device : devices) {
            if (!name.isEmpty())
                name.append(",");
            name.append(device.getName());
        }
        return name.toString();
    }

    @Override
    public void listen(MidiListener listener) throws MidiUnavailableException {
        runningListeners.set(0);
        for (var device : devices) {
            device.device.getTransmitter()
                    .setReceiver(new Receiver() {
                        @Override
                        public void send(MidiMessage message, long timeStamp) {
                            try {
                                listener.onEvent(device, new CustomMidiEvent(message, timeStamp));
                            } catch (RuntimeException e) {
                                log.error("Unexpected error in midi listener", e);
                            }
                        }

                        @Override
                        public void close() {
                            int v = runningListeners.addAndGet(-1);
                            if (v <= 0) {
                                stopListening();
                            }
                        }
                    });
            runningListeners.addAndGet(1);
        }
        waitNotListening();
    }
}
