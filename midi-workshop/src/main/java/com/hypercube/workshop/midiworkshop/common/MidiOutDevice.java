package com.hypercube.workshop.midiworkshop.common;

import com.hypercube.workshop.midiworkshop.common.errors.MidiError;

import javax.sound.midi.*;
import java.util.List;
import java.util.stream.IntStream;

public class MidiOutDevice extends AbstractMidiDevice {
    public static final int CTRL_SUSTAIN_PEDAL = 64;
    public static final int CTRL_ALL_NOTE_OFF = 123;
    public static final int CTRL_ALL_SOUND_OFF = 120;
    public static final int CTRL_ALL_CONTROLLERS_OFF = 121;
    private final Receiver receiver;
    private final ShortMessage clockMessage;

    private final ShortMessage activeSensingMessage;

    private final ShortMessage playMessage;
    private final ShortMessage pauseMessage;
    private final ShortMessage resumeMessage;


    public MidiOutDevice(MidiDevice device) throws MidiUnavailableException {

        super(device);
        receiver = device.getReceiver();
        try {
            clockMessage = new ShortMessage(ShortMessage.TIMING_CLOCK);
            activeSensingMessage = new ShortMessage(ShortMessage.ACTIVE_SENSING);
            playMessage = new ShortMessage(ShortMessage.START);
            resumeMessage = new ShortMessage(ShortMessage.CONTINUE);
            pauseMessage = new ShortMessage(ShortMessage.STOP);
        } catch (InvalidMidiDataException e) {
            throw new MidiError(e);
        }

    }

    public void send(MidiEvent evt) {
        receiver.send(evt.getMessage(), evt.getTick());
    }

    public void bindToSequencer(Sequencer sequencer) throws MidiUnavailableException {
        sequencer.getTransmitter()
                .setReceiver(new ControlChangeFilter(receiver, List.of(
                        MidiOutDevice.CTRL_ALL_CONTROLLERS_OFF,
                        MidiOutDevice.CTRL_ALL_NOTE_OFF,
                        MidiOutDevice.CTRL_ALL_SOUND_OFF
                )));
    }

    public void sendActiveSensing() {
        receiver.send(activeSensingMessage, -1);
    }

    public void sendClock() {
        receiver.send(clockMessage, -1);
    }

    public void sendPlay() {
        receiver.send(playMessage, -1);
    }

    public void sendPause() {
        receiver.send(pauseMessage, -1);
    }

    public void sendResume() {
        receiver.send(resumeMessage, -1);
    }

    public void bind(Sequencer sequencer) throws MidiUnavailableException {
        sequencer.getTransmitter()
                .setReceiver(device.getReceiver());
    }

    public void sendAllNoteOff(int channel) {
        try {
            receiver.send(new ShortMessage(ShortMessage.CONTROL_CHANGE, channel, CTRL_ALL_NOTE_OFF, 0), -1);
        } catch (InvalidMidiDataException e) {
            throw new MidiError(e);
        }
    }

    public void sendAllSoundsOff(int channel) {
        try {
            receiver.send(new ShortMessage(ShortMessage.CONTROL_CHANGE, channel, CTRL_ALL_SOUND_OFF, 0), -1);
        } catch (InvalidMidiDataException e) {
            throw new MidiError(e);
        }
    }

    public void sendAllcontrollersOff(int channel) {
        try {
            receiver.send(new ShortMessage(ShortMessage.CONTROL_CHANGE, channel, CTRL_ALL_CONTROLLERS_OFF, 0), -1);
        } catch (InvalidMidiDataException e) {
            throw new MidiError(e);
        }
    }

    /**
     * This is already done by javax.sound.midi.MidiDevice::close()
     */
    public void sendAllOff() {
        IntStream.range(0, 16)
                .forEach(ch -> {
                    sendAllNoteOff(ch);
                    sendAllcontrollersOff(ch);
                    sendAllSoundsOff(ch);
                });
    }
}
