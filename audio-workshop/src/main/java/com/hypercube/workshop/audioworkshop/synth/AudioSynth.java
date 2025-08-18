package com.hypercube.workshop.audioworkshop.synth;

import com.hypercube.workshop.audioworkshop.api.device.AudioOutputDevice;
import com.hypercube.workshop.audioworkshop.api.errors.AudioError;
import com.hypercube.workshop.audioworkshop.api.format.PCMBufferFormat;
import com.hypercube.workshop.audioworkshop.api.line.AudioOutputLine;
import com.hypercube.workshop.audioworkshop.api.pcm.BitDepth;
import com.hypercube.workshop.audioworkshop.api.pcm.PCMEncoding;
import com.hypercube.workshop.audioworkshop.synth.vca.SimpleVCA;
import com.hypercube.workshop.audioworkshop.synth.vca.VCA;
import com.hypercube.workshop.audioworkshop.synth.vco.CorrectVCO;
import com.hypercube.workshop.audioworkshop.synth.vco.VCO;
import com.hypercube.workshop.midiworkshop.api.CustomMidiEvent;
import com.hypercube.workshop.midiworkshop.api.devices.MidiInDevice;
import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;
import javax.sound.sampled.LineUnavailableException;
import java.nio.ByteOrder;

@Slf4j
@Component
public class AudioSynth {
    static final int SAMPLE_RATE = 44100;
    private volatile int midiNode;
    private volatile boolean stop;

    private void onMidiEvent(CustomMidiEvent evt, VCA vca) {
        MidiMessage msg = evt.getMessage();
        if (msg.getStatus() == ShortMessage.NOTE_ON) {
            midiNode = msg.getMessage()[1];
            int midiVelocity = msg.getMessage()[2];
            midiVelocity = Math.min(midiVelocity, 110);
            log.info(String.format("MIDI: %s Note: %d %f Hz Velocity: %d Status: %02x", evt.getHexValues(), midiNode, VCO.midiNoteToFrequency(midiNode), midiVelocity, msg.getStatus()));
            vca.onNoteOn(midiVelocity / 127.f);
        } else if (msg.getStatus() == ShortMessage.NOTE_OFF) {
            int receivedMidiNodeOff = msg.getMessage()[1];
            if (midiNode == receivedMidiNodeOff) {
                vca.onNoteOff();
                log.info(String.format("MIDI: %s Note: %d %f Hz Velocity: %d Status: %02x", evt.getHexValues(), midiNode, VCO.midiNoteToFrequency(midiNode), 0, msg.getStatus()));
            }
        } else if (msg.getStatus() == ShortMessage.PITCH_BEND) {
            stop = true;
        }
    }

    void synth(MidiInDevice midiInDevice, AudioOutputDevice audioOutputDevice) {
        try {
            int bufferSizeMs = 100;
            PCMBufferFormat format = new PCMBufferFormat(100, SAMPLE_RATE, BitDepth.BIT_DEPTH_16, 1, PCMEncoding.SIGNED, ByteOrder.BIG_ENDIAN);
            VCA vca = new SimpleVCA(SAMPLE_RATE, 10);
            VCO vco = new CorrectVCO(bufferSizeMs, SAMPLE_RATE, 16, ByteOrder.BIG_ENDIAN, vca);
            //VCA vca = new AdsrVCA(SAMPLE_RATE, 50, 10, 50);
            //VCO vco = new WavetableVCO(WavetableType.SINE, bufferSizeMs, SAMPLE_RATE, 16, ByteOrder.BIG_ENDIAN, vca);

            Thread thread = new Thread(() -> {
                try (AudioOutputLine line = new AudioOutputLine(audioOutputDevice, format)) {
                    line.start();
                    while (!stop) {
                        byte[] data = vco.generateSignal(VCO.midiNoteToFrequency(midiNode));
                        line.sendBuffer(data, data.length);
                    }
                    log.info("Terminating...");
                    midiInDevice.stopListening();
                } catch (LineUnavailableException e) {
                    throw new AudioError(e);
                }
            });
            thread.setPriority(Thread.MAX_PRIORITY);
            stop = false;
            log.info("Play some notes ! Use the pitch bend to exit...");
            thread.start();
            midiInDevice.listen((device, evt) -> onMidiEvent(evt, vca));
        } catch (MidiError e) {
            log.error("The Output device is Unavailable: " + midiInDevice.getName());
        }
    }
}
