package com.hypercube.workshop.midiworkshop.api.listener;

import com.hypercube.workshop.midiworkshop.api.CustomMidiEvent;
import com.hypercube.workshop.midiworkshop.api.MidiInDevice;
import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.SysexMessage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@RequiredArgsConstructor
@Slf4j
public class SysExMidiListener implements MidiListener {
    @Getter
    private final MidiListener listener;
    private final ByteArrayOutputStream currentSysex = new ByteArrayOutputStream();

    @Override
    public void onEvent(MidiInDevice device, CustomMidiEvent event) {
        if (event.getMessage() instanceof SysexMessage sysexMsg) {
            // This fix an insane bug in Java MIDI API where sysex can be received "splited"
            // msg.getMessage() will give you a final F7 whereas it is absolutely not completely received !
            byte[] data = sysexMsg.getStatus() == 0xF0 ? sysexMsg.getMessage() : sysexMsg.getData();
            try {
                currentSysex.write(data);
                if (data[data.length - 1] == (byte) 0xF7) {
                    SysexMessage sysexMessage = new SysexMessage(currentSysex.toByteArray(), currentSysex.size());
                    currentSysex.reset();
                    listener.onEvent(device, new CustomMidiEvent(sysexMessage));
                } else {
                    log.warn("Receive partial SysEx... Message is bigger than driver buffer, that's fine.");
                }
            } catch (InvalidMidiDataException | IOException e) {
                throw new MidiError(e);
            }

        }
    }
}
