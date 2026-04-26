package com.hypercube.workshop.midiworkshop.api.sysex.sds;

import com.hypercube.workshop.midiworkshop.api.CustomMidiEvent;
import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import com.hypercube.workshop.midiworkshop.api.listener.SysExMidiListener;
import com.hypercube.workshop.midiworkshop.api.ports.local.in.MidiInPort;
import com.hypercube.workshop.midiworkshop.api.ports.local.out.MidiOutPort;
import com.hypercube.workshop.midiworkshop.api.sysex.checksum.XORChecksum;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceDefinition;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

@Slf4j
public class SampleDumpStandard {
    private static final List<Class<? extends SampleDumpStandardMessage>> messages = List.of(
            DumpHeader.class,
            DumpPacket.class,
            DumpACK.class,
            DumpNACK.class,
            DumpCancel.class,
            DumpRequest.class,
            DumpWait.class
    );
    private final MidiDeviceDefinition deviceDefinition;
    private final MidiInPort inPort;
    private final MidiOutPort outPort;
    private final SysExMidiListener listener = new SysExMidiListener();
    private final Map<Class<? extends SampleDumpStandardMessage>, Method> fromBytesMethods = new HashMap<>();
    private final Map<Class<? extends SampleDumpStandardMessage>, Method> MatchesMethods = new HashMap<>();

    public SampleDumpStandard(MidiDeviceDefinition deviceDefinition, MidiInPort inPort, MidiOutPort outPort) {
        this.deviceDefinition = deviceDefinition;
        this.inPort = inPort;
        this.outPort = outPort;
        SampleDumpStandard.messages.forEach(clazz -> {
            try {
                fromBytesMethods.put(clazz, clazz.getMethod("fromBytes", byte[].class));
            } catch (NoSuchMethodException e) {
                throw new MidiError("Method 'fromBytes(byte[] payload)' is missing in class " + clazz.getName());
            }
            try {
                MatchesMethods.put(clazz, clazz.getMethod("matches", byte[].class));
            } catch (NoSuchMethodException e) {
                throw new MidiError("Method 'matches(byte[] payload)' is missing in class " + clazz.getName());
            }
        });
    }

    private static int computeChecksum(DumpHeader header, int packetId, byte[] packetBytes) {
        XORChecksum chk = new XORChecksum();
        chk.update(0x7E);
        chk.update(header.channel());
        chk.update(0x02);
        chk.update(packetId);
        for (byte packetByte : packetBytes) {
            chk.update(packetByte);
        }
        return chk.getValue();
    }

    /**
     * SampleId      : 200
     * SampleRate    : 44099
     * BitDepth      : 16
     * LoopType      : FORWARD_ONLY
     * sampleLength  : 88200
     * loopStart     : 88199
     * loopEnd       : 88199
     *
     */
    public void sendSample(int channel, int sampleId, byte[] data) {
        int sampleLength = data.length / 2;
        int packetId = 0;
        try {
            inPort.addListener(listener);
            // for some reason K2600R create the sample at sampleId+1
            DumpHeader header = new DumpHeader(channel, sampleId - 1, 16, 44100, sampleLength, LoopType.FORWARD_ONLY, sampleLength - 1, sampleLength - 1);
            List<DumpPacket> packets = forgePackets(header, data);
            send(header);
            var response = listener.waitResponse(3)
                    .map(this::parseResponse)
                    .orElseThrow();
            log.info("{} ", response.getClass()
                    .getSimpleName());
            if (response instanceof DumpACK ack) {
                int packetNumber = 0;
                for (DumpPacket packet : packets) {
                    packetId = packet.packetId();
                    log.info("Packet {} Id: {}", packetNumber, packetId);
                    send(packet);
                    response = listener.waitResponse(3)
                            .map(this::parseResponse)
                            .orElseThrow();
                    log.info("{} ", response.getClass()
                            .getSimpleName());
                    if (!(response instanceof DumpACK)) {
                        break;
                    }
                    packetNumber++;
                }
            }
        } catch (Exception e) {
            send(new DumpCancel(channel, packetId));
            throw new RuntimeException(e);
        } finally {
            inPort.removeListener(listener);
        }
    }

    public List<DumpPacket> requestSample(int channel, int sampleId) {
        List<DumpPacket> packets = new ArrayList<>();
        try {
            inPort.addListener(listener);
            send(new DumpRequest(channel, sampleId - 1));
            try (FileOutputStream dump = new FileOutputStream("SampleDumpResponses.syx")) {
                DumpHeader header = (DumpHeader) listener.waitResponse(5)
                        .map(r -> {
                            try {
                                dump.write(r);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            return parseResponse(r);
                        })
                        .orElseThrow();
                log.info("SampleId      : {}", header.sampleId());
                log.info("SampleRate    : {}", header.sampleFrequency());
                log.info("BitDepth      : {}", header.bitDepth());
                log.info("LoopType      : {}", header.loopType());
                log.info("sampleLength  : {}", header.sampleLength());
                log.info("loopStart     : {}", header.loopStart());
                log.info("loopEnd       : {}", header.loopEnd());
                send(new DumpACK(channel, 0));
                int totalSampleRead = 0;
                try (FileOutputStream pcm = new FileOutputStream("received.pcm")) {
                    for (int i = 0; totalSampleRead < header.sampleLength(); i++) {
                        DumpPacket packet = (DumpPacket) listener.waitResponse(3)
                                .map(r -> {
                                    try {
                                        dump.write(r);
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                    return parseResponse(r);
                                })
                                .orElseThrow();
                        packets.add(packet);
                        byte[] pcmData = unpack(header, packet);
                        pcm.write(pcmData);
                        int nbSamples = pcmData.length / 2;
                        totalSampleRead += nbSamples;
                        log.info("{}: PacketId {} size {} nbSamples {} checksum {} , total samples read {}",
                                i, "%02X".formatted(packet.packetId()), packet.data().length, nbSamples, "%02X".formatted(packet.checksum()),
                                totalSampleRead);
                        if (packet.check()) {
                            send(new DumpACK(channel, packet.packetId()));
                        } else {
                            log.info("Checksum error");
                            send(new DumpCancel(channel, packet.packetId()));
                            break;
                        }
                    }
                }
                log.info("Done");
                return packets;
            }
        } catch (Exception e) {
            if (!packets.isEmpty()) {
                send(new DumpCancel(channel, packets.getLast()
                        .packetId()));
            }
            throw new RuntimeException(e);
        } finally {
            inPort.removeListener(listener);
        }
    }

    private List<DumpPacket> forgePackets(DumpHeader header, byte[] data) {
        List<DumpPacket> result = new ArrayList<>();
        int midiBytePerSamples = 3;
        int totalMidiBytes = header.sampleLength() * midiBytePerSamples;
        int nbPackets = (int) Math.ceil((double) totalMidiBytes / 120);
        int samplesPerPacket = 120 / midiBytePerSamples;
        ByteBuffer buf = ByteBuffer.wrap(data);
        buf.order(ByteOrder.BIG_ENDIAN);
        for (int packetNumber = 0; packetNumber < nbPackets; packetNumber++) {
            int packetId = packetNumber % 128;
            byte[] packetBytes = packSamples(samplesPerPacket, buf);
            String hex = HexFormat.ofDelimiter(" ")
                    .withUpperCase()
                    .formatHex(packetBytes);
            log.info("Packet {}: {}", packetId, hex);
            int checksum = computeChecksum(header, packetId, packetBytes);
            DumpPacket packet = new DumpPacket(header.channel(), packetId, packetBytes, checksum);
            result.add(packet);
        }
        return result;
    }

    private byte[] packSamples(int samplesPerPacket, ByteBuffer buf) {
        ByteArrayOutputStream packetContent = new ByteArrayOutputStream();
        for (int i = 0; i < samplesPerPacket; i++) {
            if (buf.hasRemaining()) {
                // 1. Read the signed short (e.g., 0)
                short signedShort = buf.getShort();

                // 2. Convert to unsigned 16-bit (e.g., 32768 / 0x8000)
                // Adding 32768 centers the wave for the SDS protocol
                int unsigned16 = (signedShort + 32768) & 0xFFFF;

                // 3. Left-justify into the 21-bit SDS word
                // No & 0xFFFF here! We need all 21 bits.
                int s21 = unsigned16 << 5;

                // 4. Extract three 7-bit MIDI bytes
                int a = (s21 >> 14) & 0x7F; // Most Significant Byte
                int b = (s21 >> 7) & 0x7F; // Middle Byte
                int c = s21 & 0x7F;         // Least Significant Byte

                packetContent.write(a);
                packetContent.write(b);
                packetContent.write(c);
            } else {
                // Padding: Fill the rest of the 120-byte packet with silence (0x8000)
                packetContent.write(0x40); // MSB of 0x8000 shifted left by 5
                packetContent.write(0x00);
                packetContent.write(0x00);
            }
        }
        return packetContent.toByteArray();
    }

    private byte[] unpack(DumpHeader header, DumpPacket packet) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] payload = packet.data();
        int nbSamples = payload.length / 3;
        int remain = payload.length % 3;
        if (remain > 0) {
            String hex = HexFormat.ofDelimiter(" ")
                    .withUpperCase()
                    .formatHex(payload);
            log.warn("Wrong size for packet: {} bytes, {} bytes not used: {}", payload.length, remain, hex);
        }
        for (int j = 0; j < payload.length; ) {
            int a = payload[j++] & 0x7F;
            int b = payload[j++] & 0x7F;
            int c = payload[j++] & 0x7F;
            int sample21 = a << 14 | b << 7 | c;
            int sample16 = sample21 >> 5; // left justified !
            int sample16Signed = sample16 - 32768;
            int d = (sample16Signed >> 8) & 0xFF;
            int e = (sample16Signed >> 0) & 0xFF;
            out.write(d);
            out.write(e);
        }
        return out.toByteArray();
    }

    private void send(SampleDumpStandardMessage sampleDumpStandardMessage) {
        CustomMidiEvent request = sampleDumpStandardMessage.getRequest();
        log.info("Send {}", request.getHexValuesSpaced());
        outPort.send(request);
    }

    SampleDumpStandardMessage parseResponse(byte[] payload) {
        return messages.stream()
                .filter(clazz -> {
                    try {
                        return (boolean) MatchesMethods.get(clazz)
                                .invoke(null, (Object) payload);
                    } catch (Exception e) {
                        throw new MidiError(e);
                    }
                })
                .findFirst()
                .map(clazz -> {
                    try {
                        return (SampleDumpStandardMessage) fromBytesMethods.get(clazz)
                                .invoke(null, (Object) payload);
                    } catch (Exception e) {
                        throw new MidiError(e);
                    }
                })
                .orElse(null);
    }
}
