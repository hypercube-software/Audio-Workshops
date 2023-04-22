package com.hypercube.workshop.audioworkshop.synth;

import com.hypercube.workshop.audioworkshop.common.AudioOutputDevice;
import com.hypercube.workshop.audioworkshop.common.AudioOutputLine;
import com.hypercube.workshop.audioworkshop.common.vca.SimpleVCA;
import com.hypercube.workshop.audioworkshop.common.vca.VCA;
import com.hypercube.workshop.audioworkshop.common.vco.CorrectVCO;
import com.hypercube.workshop.audioworkshop.common.vco.NaiveVCO;
import com.hypercube.workshop.audioworkshop.common.vco.VCO;
import com.hypercube.workshop.midiworkshop.common.CustomMidiEvent;
import com.hypercube.workshop.midiworkshop.common.MidiInDevice;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.ShortMessage;
import javax.sound.sampled.LineUnavailableException;
import java.nio.ByteOrder;

@Slf4j
@Component
public class AudioSynth {
    final int SAMPLE_RATE = 44100;
    private volatile int midiNode;
    private volatile boolean stop;


    void synth(MidiInDevice midiInDevice, AudioOutputDevice audioOutputDevice) {
        try {
            int bufferSizeMs = 100;
            VCA vca = new SimpleVCA(SAMPLE_RATE, 10);
            VCO vco = new CorrectVCO(bufferSizeMs, SAMPLE_RATE, 16, ByteOrder.BIG_ENDIAN, vca);
            //VCA vca = new AdsrVCA(SAMPLE_RATE, 50, 10, 50);
            //VCO vco = new WavetableVCO(WavetableType.SINE, bufferSizeMs, SAMPLE_RATE, 16, ByteOrder.BIG_ENDIAN, vca);

            Thread thread = new Thread(() -> {
                try (AudioOutputLine line = new AudioOutputLine(audioOutputDevice, SAMPLE_RATE, 16, bufferSizeMs)) {
                    line.start();
                    while (!stop) {
                        byte[] data = vco.generateSignal(VCO.midiNoteToFrequency(midiNode));
                        line.sendBuffer(data);
                    }
                    log.info("Terminating...");
                    midiInDevice.stopListening();
                } catch (LineUnavailableException e) {
                    throw new RuntimeException(e);
                }
            });
            thread.setPriority(Thread.MAX_PRIORITY);
            stop = false;
            log.info("Play some notes ! Use the pitch bend to exit...");
            thread.start();
            midiInDevice.listen(evt -> onMidiEvent(midiInDevice, evt, vca));
        } catch (MidiUnavailableException e) {
            log.error("The Output device is Unavailable: " + midiInDevice.getName());
        }
    }

    private void onMidiEvent(MidiInDevice midiInDevice, CustomMidiEvent evt, VCA vca) {
        MidiMessage msg = evt.getMessage();
        if (msg.getStatus() == ShortMessage.NOTE_ON) {
            midiNode = (int) msg.getMessage()[1];
            int midiVelocity = (int) msg.getMessage()[2];
            midiVelocity = Math.min(midiVelocity, 110);
            log.info(String.format("MIDI: %s Note: %d %f Hz Velocity: %d Status: %02x", evt.getHexValues(), midiNode, NaiveVCO.midiNoteToFrequency(midiNode), midiVelocity, msg.getStatus()));
            vca.onNoteOn(midiVelocity / 127.f);
        } else if (msg.getStatus() == ShortMessage.NOTE_OFF) {
            int receivedMidiNodeOff = (int) msg.getMessage()[1];
            if (midiNode == receivedMidiNodeOff) {
                vca.onNoteOff();
                log.info(String.format("MIDI: %s Note: %d %f Hz Velocity: %d Status: %02x", evt.getHexValues(), midiNode, NaiveVCO.midiNoteToFrequency(midiNode), 0, msg.getStatus()));
            }
        } else if (msg.getStatus() == ShortMessage.PITCH_BEND) {
            stop = true;
        }
    }
}
