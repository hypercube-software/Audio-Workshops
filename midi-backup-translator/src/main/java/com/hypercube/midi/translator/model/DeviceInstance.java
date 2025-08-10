package com.hypercube.midi.translator.model;

import com.hypercube.midi.translator.config.project.ProjectDevice;
import com.hypercube.workshop.midiworkshop.api.CustomMidiEvent;
import com.hypercube.workshop.midiworkshop.api.MidiOutDevice;
import com.hypercube.workshop.midiworkshop.api.sysex.library.request.MidiRequest;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.SysexMessage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class DeviceInstance {
    public static final int DEFAULT_INACTIVITY_TIMEOUT_MS = 1000;
    public static final int DEFAULT_SYSEX_PAUSE_MS = 0;
    public static final int NOOP_TIME_MS = 200;
    public static final boolean DEFAULT_ENABLED = true;
    @Getter
    private final ProjectDevice setting;
    private final ByteArrayOutputStream state = new ByteArrayOutputStream();
    @Getter
    private long lastTimeReceiveMs = 0;
    @Getter
    @Setter
    private MidiRequest currentRequest;
    @Getter
    private long currentResponseSize = 0;

    public boolean isEnabled() {
        return Optional.ofNullable(setting.getEnabled())
                .orElse(DEFAULT_ENABLED);
    }

    public int getInactivityTimeoutMs() {
        return Optional.ofNullable(setting.getInactivityTimeoutMs())
                .orElse(DEFAULT_INACTIVITY_TIMEOUT_MS);
    }

    public int getSysexPauseMs() {
        return Optional.ofNullable(setting.getSysExPauseMs())
                .orElse(DEFAULT_SYSEX_PAUSE_MS);
    }

    private void readyToReceiveNextResponse() {
        updateLastTimeReceiveMs();
        currentResponseSize = 0;
    }

    public void updateLastTimeReceiveMs() {
        lastTimeReceiveMs = System.currentTimeMillis();
    }

    public void addBytes(byte[] data) {
        try {
            state.write(data);
            currentResponseSize += data.length;
            updateLastTimeReceiveMs();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void save() throws IOException {
        File file = getBackupFile();
        Files.write(file.toPath(), state.toByteArray());
    }

    public File getBackupFile() {
        return new File("BulkDump %s.syx".formatted(setting.getName()));
    }

    public int getStateSize() {
        return state.size();
    }

    public List<CustomMidiEvent> loadState() {
        try {
            List<CustomMidiEvent> events = new ArrayList<>();
            byte[] data = Files.readAllBytes(getBackupFile().toPath());
            ByteArrayOutputStream eventPayload = null;
            for (int i = 0; i < data.length; i++) {
                if (data[i] == (byte) 0xF0) {
                    if (eventPayload != null) {
                        events.add(forgeSysExEvent(eventPayload));
                    }
                    eventPayload = new ByteArrayOutputStream();
                }
                eventPayload.write(data[i]);
            }
            if (eventPayload.size() > 0) {
                events.add(forgeSysExEvent(eventPayload));
            }
            return events;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InvalidMidiDataException e) {
            throw new RuntimeException(e);
        }
    }

    private void waitReceive(MidiOutDevice out, int previousSize) {
        log.info("    Waiting data...");
        long now = System.currentTimeMillis();
        while (getStateSize() == previousSize && (System.currentTimeMillis() - now < 3000)) {
            sleep(out, NOOP_TIME_MS);
        }
    }

    /**
     * Despite we want to sleep, we still need to send ActiveSensing
     *
     * @param out
     * @param timeMs
     */
    public void sleep(MidiOutDevice out, long timeMs) {
        try {
            long start = System.currentTimeMillis();
            for (; ; ) {
                long now = System.currentTimeMillis();
                long deltaMs = now - start;
                if (deltaMs > timeMs) {
                    break;
                }
                out.sendActiveSensing();
                Thread.sleep(100);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void waitIdle(MidiOutDevice out) {
        for (; ; ) {
            long now = System.currentTimeMillis();
            long inactivityMs = now - getLastTimeReceiveMs();
            if (inactivityMs > getInactivityTimeoutMs() || isResponseComplete()) {
                break;
            }
            sleep(out, NOOP_TIME_MS);
        }
    }

    private boolean isResponseComplete() {
        return currentRequest.getResponseSize() != null && currentResponseSize == currentRequest.getResponseSize();
    }

    public void sendAndWaitResponse(MidiOutDevice out, CustomMidiEvent customMidiEvent) {
        int previousSize = getStateSize();
        readyToReceiveNextResponse();
        out.send(customMidiEvent);
        waitReceive(out, previousSize);
        waitIdle(out);
    }

    private CustomMidiEvent forgeSysExEvent(ByteArrayOutputStream eventPayload) throws InvalidMidiDataException {
        byte[] bytes = eventPayload.toByteArray();
        return new CustomMidiEvent(new SysexMessage(bytes, bytes.length), -1);
    }
}
