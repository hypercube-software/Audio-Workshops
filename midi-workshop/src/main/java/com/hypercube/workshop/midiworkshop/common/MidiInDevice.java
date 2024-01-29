package com.hypercube.workshop.midiworkshop.common;

import org.jline.utils.Log;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;

public class MidiInDevice extends AbstractMidiDevice {
    final Object closeSignal = new Object();

    public MidiInDevice(MidiDevice device) {
        super(device);
    }

    public void stopListening() {
        synchronized (closeSignal) {
            closeSignal.notifyAll();
        }
    }

    public void listen(MidiListener listener) throws MidiUnavailableException {
        try {
            open();
            device.getTransmitter()
                    .setReceiver(new Receiver() {
                        @Override
                        public void send(MidiMessage message, long timeStamp) {
                            listener.onEvent(new CustomMidiEvent(message, timeStamp));
                        }

                        @Override
                        public void close() {
                            stopListening();
                        }
                    });

            synchronized (closeSignal) {
                closeSignal.wait();
            }
        } catch (InterruptedException e) {
            Log.warn("Interrupted", e);
            Thread.currentThread()
                    .interrupt();
        } finally {
            close();
        }

    }
}
