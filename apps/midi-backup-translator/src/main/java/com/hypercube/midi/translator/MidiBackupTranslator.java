package com.hypercube.midi.translator;

import com.hypercube.midi.translator.config.project.ProjectConfiguration;
import com.hypercube.workshop.midiworkshop.api.CustomMidiEvent;
import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import com.hypercube.workshop.midiworkshop.api.ports.local.in.MidiInPort;
import com.hypercube.workshop.midiworkshop.api.ports.local.out.MidiOutPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;
import java.io.Closeable;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Decimate messages to fit a certain bandwidth for CC and SysEx
 */
@Slf4j
@RequiredArgsConstructor
public class MidiBackupTranslator implements Closeable {
    private final ProjectConfiguration conf;
    private final ConcurrentLinkedDeque<CustomMidiEvent> eventQueue = new ConcurrentLinkedDeque<>();
    private MidiInPort midiInPort;
    private MidiOutPort midiOutPort;
    private int bandwidth;
    private Thread consumer;
    private boolean stop;
    private volatile CustomMidiEvent lastDropped;

    @Override
    public void close() {
        if (midiInPort != null) {
            midiInPort.close();
        }
    }

    private void consumerLoop() {
        stop = false;
        long prevSend = -1;
        long prevSize = -1;
        double maxBytesPerSec = bandwidth;
        log.info("maxBytesPerSec: {}", maxBytesPerSec);
        while (!stop) {
            long now = System.nanoTime();
            double delta = now - prevSend;
            double deltaSec = delta / 1e9d;
            double currentBandwidth = prevSize / deltaSec;

            var event = eventQueue.pollFirst();
            if (event != null) {
                int status = event.getMessage()
                        .getStatus();
                boolean requireThrottling = status == ShortMessage.CONTROL_CHANGE || status == SysexMessage.SYSTEM_EXCLUSIVE;
                int payloadSize = event.getMessage()
                        .getLength();
                if (requireThrottling) {
                    if (prevSend == -1 || currentBandwidth < maxBytesPerSec) {
                        log.info("Sent!  %s payloadSize: %d bytesPerSec: %f >= %f".formatted(event.getHexValues(), payloadSize, currentBandwidth, maxBytesPerSec));
                        midiOutPort.send(event);
                        prevSend = now;
                        prevSize = payloadSize;
                    } else {
                        lastDropped = event;
                        //log.info("Dropped!  %s payloadSize: %d bytesPerSec: %f >= %f".formatted(event.getHexValues(), payloadSize, currentBandwidth, maxBytesPerSec));
                    }
                } else {
                    //log.info("Unthrottled Sent!  %s payloadSize: %d bytesPerSec: %f >= %f".formatted(event.getHexValues(), payloadSize, currentBandwidth, maxBytesPerSec));
                    midiOutPort.send(event);
                }
            } else if (lastDropped != null && currentBandwidth < maxBytesPerSec) {
                //log.info("Send last dropped: %s".formatted(lastDropped.getHexValues()));
                midiOutPort.send(lastDropped);
                prevSend = now;
                prevSize = lastDropped.getMessage()
                        .getLength();
                lastDropped = null;
            }
        }
    }

    private void onMidiEvent(MidiInPort midiInPort, CustomMidiEvent customMidiEvent) {
        if (customMidiEvent.getMessage()
                .getStatus() == ShortMessage.CONTROL_CHANGE) {
            int cc = customMidiEvent.getMessage()
                    .getMessage()[1];
            int value = customMidiEvent.getMessage()
                    .getMessage()[2];
            var translation = conf.getTranslationsMap()
                    .get(cc);
            if (translation != null) {
                translation.getPayloads()
                        .forEach(p -> {
                            byte[] payload = p.getPayload();
                            payload[p.getValueIndex()] = (byte) translation.scaledCC(value);
                            try {
                                CustomMidiEvent evt = new CustomMidiEvent(new SysexMessage(payload, payload.length), -1);
                                eventQueue.add(evt);
                            } catch (InvalidMidiDataException e) {
                                throw new MidiError(e);
                            }
                        });
            } else {
                eventQueue.add(customMidiEvent);
            }
        } else {
            eventQueue.add(customMidiEvent);
        }
    }

    void translate(MidiInPort in, MidiOutPort out, Integer bandwidth) {
        try {
            this.midiInPort = in;
            this.midiOutPort = out;
            // Midi throughtput 3125 bytes / sec
            this.bandwidth = Optional.ofNullable(bandwidth)
                    .orElse(3125);
            consumer = new Thread(this::consumerLoop);
            consumer.setPriority(Thread.MAX_PRIORITY);
            consumer.start();
            in.listen(this::onMidiEvent);
        } catch (MidiError e) {
            log.error("The Output device is Unavailable: {}", in.getName());
        } finally {
            stop = true;
            try {
                consumer.join();
            } catch (InterruptedException e) {
                throw new MidiError(e);
            }
        }
    }
}
