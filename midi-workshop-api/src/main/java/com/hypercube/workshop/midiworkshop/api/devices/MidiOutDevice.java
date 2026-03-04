package com.hypercube.workshop.midiworkshop.api.devices;

import com.hypercube.workshop.midiworkshop.api.ControlChangeFilter;
import com.hypercube.workshop.midiworkshop.api.CustomMidiEvent;
import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import com.hypercube.workshop.midiworkshop.api.presets.MidiPreset;
import lombok.extern.slf4j.Slf4j;

import javax.sound.midi.*;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

@Slf4j
public class MidiOutDevice extends AbstractMidiDevice {
    public static final int CTRL_SUSTAIN_PEDAL = 64;
    public static final int CTRL_ALL_NOTE_OFF = 123;
    public static final int CTRL_ALL_SOUND_OFF = 120;
    public static final int CTRL_ALL_CONTROLLERS_OFF = 121;
    public static final int CC_BANK_SELECT_MSB = 0x00;
    public static final int CC_BANK_SELECT_LSB = 0x20;
    public static final int NO_TIME_STAMP = -1;
    private final ShortMessage clockMessage;
    private final ShortMessage activeSensingMessage;
    private final ShortMessage playMessage;
    private final ShortMessage pauseMessage;
    private final ShortMessage resumeMessage;
    private Receiver receiver;

    public MidiOutDevice(MidiDevice device) {
        super(device);
        try {
            clockMessage = new ShortMessage(ShortMessage.TIMING_CLOCK);
            activeSensingMessage = new ShortMessage(ShortMessage.ACTIVE_SENSING);
            playMessage = new ShortMessage(ShortMessage.START);
            resumeMessage = new ShortMessage(ShortMessage.CONTINUE);
            pauseMessage = new ShortMessage(ShortMessage.STOP);
        } catch (InvalidMidiDataException e) {
            throw new MidiError(device, e);
        }

    }

    @Override
    public void close() {
        if (getOpenCount() == 1) {
            Optional.ofNullable(receiver)
                    .ifPresent(Receiver::close);
            receiver = null;
        }
        super.close();
    }

    @Override
    public void open() {
        super.open();
        try {
            if (super.isOpen()) {
                if (receiver == null) {
                    receiver = device.getReceiver();
                } else {
                    log.info("Receiver already set for " + getName());
                }
            } else {
                receiver = null;
            }
        } catch (MidiUnavailableException e) {
            throw new MidiError(device, e);
        }
        if (receiver == null) {
            throw new MidiError("Unable to listen " + getName());
        }
    }

    @Override
    public boolean isOpen() {
        return super.isOpen() && receiver != null;
    }

    public void send(MidiEvent evt) {
        send(evt.getMessage(), evt.getTick());
    }

    public void send(MidiMessage msg, long timestamp) {
        if (!isOpen()) {
            throw new MidiError(device, "Open the device before sending anything");
        }
        receiver.send(msg, timestamp);
    }

    public void sendPresetChange(MidiPreset preset) {
        preset.getCommands()
                .stream()
                .map(cmd -> new CustomMidiEvent(cmd))
                .forEach(evt -> {
                    log.info("Send " + evt.getHexValues());
                    send(evt);
                });
    }

    public void sendProgramChange(int channel, int prg) {
        try {
            send(new ShortMessage(ShortMessage.PROGRAM_CHANGE, channel, prg, 0), NO_TIME_STAMP);
        } catch (InvalidMidiDataException e) {
            throw new MidiError(device, e);
        }
    }

    public void sendBankMSB(int channel, int bankMSB) {
        try {
            send(new ShortMessage(ShortMessage.CONTROL_CHANGE, channel, CC_BANK_SELECT_MSB, bankMSB), NO_TIME_STAMP);
        } catch (InvalidMidiDataException e) {
            throw new MidiError(device, e);
        }
    }

    public void sendBankLSB(int channel, int bankLSB) {
        try {
            send(new ShortMessage(ShortMessage.CONTROL_CHANGE, channel, CC_BANK_SELECT_LSB, bankLSB), NO_TIME_STAMP);
        } catch (InvalidMidiDataException e) {
            throw new MidiError(device, e);
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

    public void bind(Sequencer sequencer) throws MidiUnavailableException {
        sequencer.getTransmitter()
                .setReceiver(device.getReceiver());
    }

    public void sendAllNoteOff(int channel) {
        try {
            send(new ShortMessage(ShortMessage.CONTROL_CHANGE, channel, CTRL_ALL_NOTE_OFF, CC_BANK_SELECT_MSB), NO_TIME_STAMP);
        } catch (InvalidMidiDataException e) {
            throw new MidiError(device, e);
        }
    }

    public void sendAllSoundsOff(int channel) {
        try {
            send(new ShortMessage(ShortMessage.CONTROL_CHANGE, channel, CTRL_ALL_SOUND_OFF, CC_BANK_SELECT_MSB), NO_TIME_STAMP);
        } catch (InvalidMidiDataException e) {
            throw new MidiError(device, e);
        }
    }

    public void sendAllControllersOff(int channel) {
        try {
            send(new ShortMessage(ShortMessage.CONTROL_CHANGE, channel, CTRL_ALL_CONTROLLERS_OFF, CC_BANK_SELECT_MSB), NO_TIME_STAMP);
        } catch (InvalidMidiDataException e) {
            throw new MidiError(device, e);
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
