package com.hypercube.workshop.midiworkshop.api.ports.local.out;

import com.hypercube.workshop.midiworkshop.api.CustomMidiEvent;
import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import com.hypercube.workshop.midiworkshop.api.ports.AbstractMidiDevice;
import com.hypercube.workshop.midiworkshop.api.presets.MidiPreset;
import lombok.extern.slf4j.Slf4j;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;
import java.util.stream.IntStream;

@Slf4j
public abstract class MidiOutPort extends AbstractMidiDevice {
    public static final int CTRL_SUSTAIN_PEDAL = 64;
    public static final int CTRL_ALL_NOTE_OFF = 123;
    public static final int CTRL_ALL_SOUND_OFF = 120;
    public static final int CTRL_ALL_CONTROLLERS_OFF = 121;
    public static final int CC_BANK_SELECT_MSB = 0x00;
    public static final int CC_BANK_SELECT_LSB = 0x20;
    /**
     * Used for immediate messages
     */
    public static final int NO_TIME_STAMP = -1;
    private final ShortMessage clockMessage;
    private final ShortMessage activeSensingMessage;
    private final ShortMessage playMessage;
    private final ShortMessage pauseMessage;
    private final ShortMessage resumeMessage;

    public MidiOutPort(String name) {
        super(name);
        try {
            clockMessage = new ShortMessage(ShortMessage.TIMING_CLOCK);
            activeSensingMessage = new ShortMessage(ShortMessage.ACTIVE_SENSING);
            playMessage = new ShortMessage(ShortMessage.START);
            resumeMessage = new ShortMessage(ShortMessage.CONTINUE);
            pauseMessage = new ShortMessage(ShortMessage.STOP);
        } catch (InvalidMidiDataException e) {
            throw new MidiError(name, e);
        }
    }

    public void send(MidiEvent evt) {
        send(evt.getMessage(), evt.getTick());
    }

    public abstract void send(MidiMessage msg, long timestamp);

    public void sendPresetChange(MidiPreset preset) {
        preset.getCommands()
                .stream()
                .map(CustomMidiEvent::new)
                .forEach(evt -> {
                    log.info("Send {} through MIDI port {}", evt.getHexValues(), getName());
                    send(evt);
                });
    }

    public void sendProgramChange(int channel, int prg) {
        try {
            send(new ShortMessage(ShortMessage.PROGRAM_CHANGE, channel, prg, 0), NO_TIME_STAMP);
        } catch (InvalidMidiDataException e) {
            throw new MidiError(name, e);
        }
    }

    public void sendBankMSB(int channel, int bankMSB) {
        try {
            send(new ShortMessage(ShortMessage.CONTROL_CHANGE, channel, CC_BANK_SELECT_MSB, bankMSB), NO_TIME_STAMP);
        } catch (InvalidMidiDataException e) {
            throw new MidiError(name, e);
        }
    }

    public void sendBankLSB(int channel, int bankLSB) {
        try {
            send(new ShortMessage(ShortMessage.CONTROL_CHANGE, channel, CC_BANK_SELECT_LSB, bankLSB), NO_TIME_STAMP);
        } catch (InvalidMidiDataException e) {
            throw new MidiError(name, e);
        }
    }


    public void sendActiveSensing() {
        send(activeSensingMessage, NO_TIME_STAMP);
    }

    public void sendClock() {
        send(clockMessage, NO_TIME_STAMP);
    }

    public void sendPlay() {
        send(playMessage, NO_TIME_STAMP);
    }

    public void sendPause() {
        send(pauseMessage, NO_TIME_STAMP);
    }

    public void sendResume() {
        send(resumeMessage, NO_TIME_STAMP);
    }


    public void sendAllNoteOff(int channel) {
        try {
            send(new ShortMessage(ShortMessage.CONTROL_CHANGE, channel, CTRL_ALL_NOTE_OFF, CC_BANK_SELECT_MSB), NO_TIME_STAMP);
        } catch (InvalidMidiDataException e) {
            throw new MidiError(name, e);
        }
    }

    public void sendAllSoundsOff(int channel) {
        try {
            send(new ShortMessage(ShortMessage.CONTROL_CHANGE, channel, CTRL_ALL_SOUND_OFF, CC_BANK_SELECT_MSB), NO_TIME_STAMP);
        } catch (InvalidMidiDataException e) {
            throw new MidiError(name, e);
        }
    }

    public void sendAllControllersOff(int channel) {
        try {
            send(new ShortMessage(ShortMessage.CONTROL_CHANGE, channel, CTRL_ALL_CONTROLLERS_OFF, CC_BANK_SELECT_MSB), NO_TIME_STAMP);
        } catch (InvalidMidiDataException e) {
            throw new MidiError(name, e);
        }
    }

    /**
     * This is already done by javax.sound.midi.MidiDevice::close()
     */
    public void sendAllOff() {
        IntStream.range(0, 16)
                .forEach(ch -> {
                    sendAllNoteOff(ch);
                    sendAllControllersOff(ch);
                    sendAllSoundsOff(ch);
                });
    }

    /**
     * send ActiveSensing for a period of time
     */
    public void sleep(long timeMs) {
        try {
            long start = System.currentTimeMillis();
            for (; ; ) {
                long now = System.currentTimeMillis();
                long deltaMs = now - start;
                if (deltaMs > timeMs) {
                    break;
                }
                sendActiveSensing();
                Thread.sleep(150);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
