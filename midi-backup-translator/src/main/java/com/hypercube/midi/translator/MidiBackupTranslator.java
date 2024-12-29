package com.hypercube.midi.translator;

import com.hypercube.midi.translator.config.project.ProjectConfiguration;
import com.hypercube.workshop.midiworkshop.common.CustomMidiEvent;
import com.hypercube.workshop.midiworkshop.common.MidiInDevice;
import com.hypercube.workshop.midiworkshop.common.MidiOutDevice;
import com.hypercube.workshop.midiworkshop.common.errors.MidiError;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Decimate messages to fit a certain bandwidth for CC and SysEx
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MidiBackupTranslator {
    private final ProjectConfiguration conf;
    private MidiInDevice midiInDevice;
    private MidiOutDevice midiOutDevice;
    private int bandwidth;
    private Thread consumer;
    private ConcurrentLinkedDeque<CustomMidiEvent> eventQueue = new ConcurrentLinkedDeque<>();
    private boolean stop;
    private volatile CustomMidiEvent lastDropped;

    public void close() {
        if (midiInDevice != null) {
            midiInDevice.stopListening();
        }
    }

    void translate(MidiInDevice in, MidiOutDevice out, int bandwidth) {
        try {
            this.midiInDevice = in;
            this.midiOutDevice = out;
            this.bandwidth = bandwidth;
            consumer = new Thread(this::consumerLoop);
            consumer.setPriority(Thread.MAX_PRIORITY);
            consumer.start();
            in.listen(this::onMidiEvent);
        } catch (MidiUnavailableException e) {
            log.error("The Output device is Unavailable: " + in.getName());
        } finally {
            stop = true;
            try {
                consumer.join();
            } catch (InterruptedException e) {
                throw new MidiError(e);
            }
        }
    }

    private void consumerLoop() {
        stop = false;
        long prevSend = -1;
        long prevSize = -1;
        // Midi throughtput 3125 bytes / sec
        double maxBytesPerSec = bandwidth;
        log.info("maxBytesPerSec: " + maxBytesPerSec);
        while (!stop) {
            long now = System.nanoTime();
            double delta = now - prevSend;
            double deltaSec = delta / 1e9d;
            double currentBandwidth = prevSize / deltaSec;

            var event = eventQueue.pollFirst();
            if (event != null) {
                int status = event.getMessage()
                        .getStatus();
                boolean requireThrottlings = status == ShortMessage.CONTROL_CHANGE || status == SysexMessage.SYSTEM_EXCLUSIVE;
                if (requireThrottlings) {
                    int payloadSize = event.getMessage()
                            .getLength();
                    if (prevSend == -1 || !requireThrottlings || currentBandwidth < maxBytesPerSec) {
                        midiOutDevice.send(event);
                        prevSend = now;
                        prevSize = payloadSize;
                    } else {
                        lastDropped = event;
                        //log.info("Dropped!  %s payloadSize: %d bytesPerSec: %f >= %f".formatted(event.getHexValues(), payloadSize, currentBandwidth, maxBytesPerSec));
                    }
                } else {
                    midiOutDevice.send(event);
                }
            } else if (lastDropped != null && currentBandwidth < maxBytesPerSec) {
                //log.info("Send last dropped: %s".formatted(lastDropped.getHexValues()));
                midiOutDevice.send(lastDropped);
                prevSend = now;
                prevSize = lastDropped.getMessage()
                        .getLength();
                lastDropped = null;
            }
        }
    }

    private void onMidiEvent(MidiInDevice midiInDevice, CustomMidiEvent customMidiEvent) {
        if (customMidiEvent.getMessage()
                .getStatus() == ShortMessage.CONTROL_CHANGE) {
            int cc = customMidiEvent.getMessage()
                    .getMessage()[1];
            int value = customMidiEvent.getMessage()
                    .getMessage()[2];
            var translation = conf.getTranslationsMap()
                    .get(cc);
            if (translation != null) {
                byte[] payload = translation.getPayload();
                payload[translation.getValueIndex()] = (byte) translation.scaledCC(value);
                try {
                    CustomMidiEvent evt = new CustomMidiEvent(new SysexMessage(payload, payload.length), -1);
                    eventQueue.add(evt);
                } catch (InvalidMidiDataException e) {
                    throw new MidiError(e);
                }
            } else {
                eventQueue.add(customMidiEvent);
            }
        } else {
            eventQueue.add(customMidiEvent);
        }
    }
}
