package com.hypercube.mpm.midi;

import com.hypercube.mpm.app.DeviceStateManager;
import com.hypercube.workshop.midiworkshop.api.CustomMidiEvent;
import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.ShortMessage;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class VirtualKeyboard {
    private final Map<KeyCode, Integer> keyToMidiNoteMap = forgeKeyToNoteMap();
    private final Map<KeyCode, Boolean> keyState = new HashMap<>();
    private final MidiRouter midiRouter;
    private final DeviceStateManager deviceStateManager;

    public void translateKeyDown(KeyEvent keyEvent) {
        KeyCode code = keyEvent.getCode();
        translateKeyDown(code)
                .forEach(midiRouter::sendToMainDestination);
    }

    public void translateKeyUp(KeyEvent keyEvent) {
        KeyCode code = keyEvent.getCode();
        translateKeyUp(code)
                .ifPresent(midiRouter::sendToMainDestination);
    }

    /**
     * Return a list of event instead of a single one, in case we need to NoteOff before NoteOn
     */
    public List<CustomMidiEvent> translateKeyDown(KeyCode code) {
        if (keyToMidiNoteMap.containsKey(code)) {
            List<CustomMidiEvent> events = new ArrayList<>();
            if (keyIsOn(code)) {
                events.add(forgeNoteOff(code));
            }
            events.add(forgeNoteOn(code));
            setNoteOn(code);
            return events;
        }
        return List.of();
    }

    public Optional<CustomMidiEvent> translateKeyUp(KeyCode code) {
        if (keyToMidiNoteMap.containsKey(code) && keyIsOn(code)) {
            return Optional.of(forgeNoteOff(code));
        }
        return Optional.empty();
    }

    private CustomMidiEvent forgeNoteOn(KeyCode code) {
        int midiNote = keyToMidiNoteMap.get(code);
        int channel = deviceStateManager.getCurrentOutputChannel();
        log.info("Key on {} {} => Midi note {} on channel {}", code, code
                .getCode(), midiNote, channel);
        try {
            return new CustomMidiEvent(new ShortMessage(ShortMessage.NOTE_ON, channel, midiNote, 100));// velocity 100
        } catch (InvalidMidiDataException e) {
            throw new MidiError(e);
        }
    }

    private CustomMidiEvent forgeNoteOff(KeyCode code) {
        int midiNote = keyToMidiNoteMap.get(code);
        int channel = deviceStateManager.getCurrentOutputChannel();
        log.info("Key off {} {} => Midi note {} on channel {}", code, code.getCode(), midiNote, channel);
        setNoteOff(code);
        try {
            return new CustomMidiEvent(new ShortMessage(ShortMessage.NOTE_OFF, channel, midiNote, 0));
        } catch (InvalidMidiDataException e) {
            throw new MidiError(e);
        }
    }

    private void setNoteOff(KeyCode code) {
        keyState.put(code, false);
    }

    private void setNoteOn(KeyCode code) {
        keyState.put(code, true);
    }

    private Boolean keyIsOn(KeyCode code) {
        return keyState.getOrDefault(code, false);
    }

    private Map<KeyCode, Integer> forgeKeyToNoteMap() {
        Map<KeyCode, Integer> map = new HashMap<>();
        int note = 60;
        map.put(KeyCode.A, note++); // C
        map.put(KeyCode.DIGIT2, note++);
        map.put(KeyCode.Z, note++); // D
        map.put(KeyCode.DIGIT3, note++);
        map.put(KeyCode.E, note++); // E
        map.put(KeyCode.R, note++); // F
        map.put(KeyCode.DIGIT5, note++);
        map.put(KeyCode.T, note++); // G
        map.put(KeyCode.DIGIT6, note++);
        map.put(KeyCode.Y, note++); // A
        map.put(KeyCode.DIGIT7, note++);
        map.put(KeyCode.U, note++); // B
        map.put(KeyCode.I, note); // C
        note = 60 - 12;
        map.put(KeyCode.W, note++); // C
        map.put(KeyCode.S, note++);
        map.put(KeyCode.X, note++); // D
        map.put(KeyCode.D, note++);
        map.put(KeyCode.C, note++); // E
        map.put(KeyCode.V, note++); // F
        map.put(KeyCode.G, note++);
        map.put(KeyCode.B, note++); // G
        map.put(KeyCode.H, note++);
        map.put(KeyCode.N, note++); // A
        map.put(KeyCode.J, note++);
        map.put(KeyCode.COMMA, note++); // B
        map.put(KeyCode.SEMICOLON, note); // C
        return map;
    }
}
