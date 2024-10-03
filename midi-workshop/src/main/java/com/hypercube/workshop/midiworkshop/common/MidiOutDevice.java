package com.hypercube.workshop.midiworkshop.common;

import com.hypercube.workshop.midiworkshop.common.errors.MidiError;
import com.hypercube.workshop.midiworkshop.common.presets.MidiPreset;

import javax.sound.midi.*;
import java.io.IOException;
import java.util.List;
import java.util.stream.IntStream;

public class MidiOutDevice extends AbstractMidiDevice {
    public static final int CTRL_SUSTAIN_PEDAL = 64;
    public static final int CTRL_ALL_NOTE_OFF = 123;
    public static final int CTRL_ALL_SOUND_OFF = 120;
    public static final int CTRL_ALL_CONTROLLERS_OFF = 121;
    private Receiver receiver;
    private final ShortMessage clockMessage;

    private final ShortMessage activeSensingMessage;

    private final ShortMessage playMessage;
    private final ShortMessage pauseMessage;
    private final ShortMessage resumeMessage;

    @Override
    public void close() throws IOException {
        receiver.close();
        super.close();
    }

    @Override
    public void open() {
        super.open();
        try {
            receiver = device.getReceiver();
        } catch (MidiUnavailableException e) {
            throw new MidiError(e);
        }
    }

    public MidiOutDevice(MidiDevice device) throws MidiUnavailableException {

        super(device);
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
        if (!isOpen()) {
            throw new MidiError("Open the device before sending anything");
        }
        receiver.send(evt.getMessage(), evt.getTick());
    }

    public void sendPresetChange(MidiPreset preset) {
        try {
            switch (preset.presetFormat()) {
                case BANK_MSB_PRG, BANK_MSB_PRG1 -> {
                    send(new MidiEvent(new ShortMessage(ShortMessage.CONTROL_CHANGE, 0, preset.bank()), 0));
                }
                case BANK_LSB_PRG, BANK_LSB_PRG1 -> {
                    send(new MidiEvent(new ShortMessage(ShortMessage.CONTROL_CHANGE, 32, preset.bank()), 0));
                }
                case BANK_MSB_LSB_PRG, BANK_MSB_LSB_PRG1 -> {
                    int bankMSB = (preset.bank() >> 7) & 0x7F;
                    int bankLSB = (preset.bank()) & 0x7F;
                    send(new MidiEvent(new ShortMessage(ShortMessage.CONTROL_CHANGE, 0, bankMSB), 0));
                    send(new MidiEvent(new ShortMessage(ShortMessage.CONTROL_CHANGE, 32, bankLSB), 0));
                }
            }
            int programNumber = switch (preset.presetFormat()) {
                case BANK_MSB_PRG, BANK_LSB_PRG, BANK_MSB_LSB_PRG -> preset.program();
                case BANK_MSB_PRG1, BANK_LSB_PRG1, BANK_MSB_LSB_PRG1 -> preset.program() - 1;
            };
            send(new MidiEvent(new ShortMessage(ShortMessage.PROGRAM_CHANGE, programNumber, 0), 0));
        } catch (InvalidMidiDataException e) {
            throw new MidiError(e);
        }
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
