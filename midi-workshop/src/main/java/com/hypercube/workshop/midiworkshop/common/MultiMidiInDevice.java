package com.hypercube.workshop.midiworkshop.common;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class MultiMidiInDevice extends MidiInDevice {
    private final List<MidiInDevice> devices;
    Object closeSignal = new Object();

    private AtomicInteger runningListeners = new AtomicInteger(0);

    public MultiMidiInDevice(List<MidiInDevice> devices) {
        super(null);
        this.devices = devices;
    }

    @Override
    public void open() throws MidiUnavailableException {
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
    public void close() {
        for (var device : devices) {
            device.close();
        }
    }

    @Override
    public String getName() {
        var name = new StringBuilder();
        for (var device : devices) {
            if (name.length() > 0)
                name.append(",");
            name.append(device.getName());
        }
        return name.toString();
    }

    public void stopListening() {
        synchronized (closeSignal) {
            closeSignal.notify();
        }
    }

    public void listen(MidiListener listener) throws MidiUnavailableException {
        try {
            open();
            runningListeners.set(0);
            for (var device : devices) {
                device.device.getTransmitter().setReceiver(new Receiver() {
                    @Override
                    public void send(MidiMessage message, long timeStamp) {
                        listener.onEvent(new CustomMidiEvent(message, timeStamp));
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

            try {
                synchronized (closeSignal) {
                    closeSignal.wait();
                }
            } catch (InterruptedException e) {
                return;
            }
        } finally {
            close();
        }

    }
}
