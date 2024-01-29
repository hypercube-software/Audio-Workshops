package com.hypercube.workshop.midiworkshop;

import com.hypercube.workshop.midiworkshop.common.MidiNote;
import com.hypercube.workshop.midiworkshop.common.seq.KeySignature;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@SpringBootTest(properties = "spring.shell.interactive.enabled=false")
class MidiWorkshopApplicationTests {
    @Test
    void testMidiNote() {
        assertEquals(MidiNote.fromValue(42), MidiNote.fromName("F#1"));
        assertEquals(MidiNote.fromName("F#1")
                .value(), MidiNote.fromName("Gb1")
                .value());
        assertNotEquals(MidiNote.fromValue(43), MidiNote.fromName("F#1"));
        assertNotEquals(MidiNote.fromName("F#1"), MidiNote.fromName("Gb1"));
    }

    @Test
    void testKeySignature() {
        assertEquals("A Major", new KeySignature(3, true).toString());
        assertEquals("Eb Major", new KeySignature(-3, true).toString());
        assertEquals("F# Minor", new KeySignature(3, false).toString());
        assertEquals("C Minor", new KeySignature(-3, false).toString());

        assertEquals("Cb Major", new KeySignature(-7, true).toString());
        assertEquals("C# Major", new KeySignature(7, true).toString());
        assertEquals("Ab Minor", new KeySignature(-7, false).toString());
        assertEquals("A# Minor", new KeySignature(7, false).toString());
    }

}
