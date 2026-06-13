package com.hypercube.workshop.midiworkshop.presets.kurzweil;

import com.hypercube.workshop.midiworkshop.api.CustomMidiEvent;
import com.hypercube.workshop.midiworkshop.api.MidiNote;
import com.hypercube.workshop.midiworkshop.api.MidiPortsManager;
import com.hypercube.workshop.midiworkshop.api.config.ConfigHelper;
import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import com.hypercube.workshop.midiworkshop.api.ports.local.in.MidiInPort;
import com.hypercube.workshop.midiworkshop.api.ports.local.out.MidiOutPort;
import com.hypercube.workshop.midiworkshop.api.sysex.library.MidiDeviceLibrary;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceDefinition;
import com.hypercube.workshop.midiworkshop.api.sysex.library.io.MidiDeviceRequester;
import com.hypercube.workshop.midiworkshop.api.sysex.library.io.response.MidiRequestResponse;
import com.hypercube.workshop.midiworkshop.api.sysex.macro.CommandCall;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.KObject;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.KurzweilSysExParser;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io.KFDeserializer;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io.KurzweilFileConverter;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io.sample.KFSoundBlockDeserializer;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.soundblock.KFSoundBlock;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.model.BaseObject;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.model.DataACK;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.model.ObjectInfo;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.model.ObjectWrite;
import com.hypercube.workshop.midiworkshop.api.sysex.sds.DumpHeader;
import com.hypercube.workshop.midiworkshop.api.sysex.sds.LoopType;
import com.hypercube.workshop.midiworkshop.api.sysex.sds.SampleDumpStandard;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamWriter;
import com.hypercube.workshop.midiworkshop.api.sysex.util.MidiEventBuilder;
import com.hypercube.workshop.midiworkshop.api.sysex.util.SysExReader;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.SysexMessage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Slf4j
public class KurzweilExplorer {
    private final MidiDeviceLibrary library;
    private final MidiDeviceRequester midiDeviceRequester;
    private final MidiPortsManager midiPortsManager;
    private final KurzweilSysExParser kurzweilSysExParser = new KurzweilSysExParser();
    BlockingQueue<Boolean> signalQueue = new ArrayBlockingQueue<>(1);
    AtomicBoolean skip = new AtomicBoolean(false);

    public void dirBank(String deviceName, int type, int bank) {
        converseWith(deviceName, (device, input, output) -> {
            String typeId = MidiEventBuilder.to14Bits(type, false);
            String bankId = MidiEventBuilder.to7Bits(bank);
            List<ObjectInfo> result = queryList(device, input, output, "DIR_BANK(%s,%s,00)".formatted(typeId, bankId));
            result.forEach(info -> log.info("{} {}", info.getObjectId(), info.getName()));
        });
    }

    public void loadObject(String deviceName, int type, int id) {
        converseWith(deviceName, (device, input, output) -> {
            String typeId = MidiEventBuilder.to14Bits(type, false);
            String objectId = MidiEventBuilder.to14Bits(id, false);
            ObjectWrite result = query(device, input, output, "READ(%s,%s,00)".formatted(typeId, objectId));
            String json = KFDeserializer.toJson(result.getKfObject());
            log.info("{} {}: {}", result.getObjectId(), result.getName(), json);
        });
    }

    public void sendPatch(String deviceName, Path filename) {
        KurzweilSysExParser p = new KurzweilSysExParser();
        converseWith(deviceName, (device, input, output) -> {
            try {
                byte[] content = Files.readAllBytes(filename);
                List<byte[]> sysEx = SysExReader.splitSysEx(content);
                List<BaseObject> objects = sysEx.stream()
                        .map(payload -> p.parse(payload)
                                .getFirst())
                        .toList();
                input.addSysExListener((inPort, event) -> {
                    if (!skip.get()) {
                        var m = p.parse(event.getMessage()
                                        .getMessage())
                                .getFirst();
                        log.info("MainListener received: {} {}", m.getClass()
                                .getSimpleName(), event.getHexValues());
                        if (m instanceof DataACK) {
                            signalQueue.add(true);
                        }
                    }
                });

                for (int o = 0; o < objects.size(); o++) {
                    byte[] payload = sysEx.get(o);
                    BaseObject r = objects.get(o);
                    if (KObject.fromType(r.getObjectType())
                            .orElseThrow() == KObject.SOUND_BLOCK && r instanceof ObjectWrite write) {
                        sendSample(filename, device, input, output, (KFSoundBlock) write.getKfObject());
                    } else {
                        log.info("Send {}", r.getClass()
                                .getSimpleName());
                        MidiEvent evt = new CustomMidiEvent(new SysexMessage(payload, payload.length));
                        output.send(evt);
                        signalQueue.take();
                    }
                }

            } catch (IOException | InvalidMidiDataException | InterruptedException e) {
                throw new RuntimeException(e);
            }

        });
    }

    public void sendSample(String deviceName, int objectId) {

    }

    public void getSample(String deviceName, int objectId) {
        converseWith(deviceName, (device, input, output) -> {
            String sampleId = MidiEventBuilder.to14Bits(objectId, false);
            String cmtDirSample = "DIR($0106,%s)".formatted(sampleId);
            String cmdLoadSample = "READ($0106, %s, 00)".formatted(sampleId);
            ObjectInfo sampleInfo = query(device, input, output, cmtDirSample);
            log.info("Sample Block Name (via INFO) : {}, size {}", sampleInfo.getName(), "%02X".formatted(sampleInfo.getSize()));
            if (sampleInfo.getSize() > 0) {
                ObjectWrite sampleData = query(device, input, output, cmdLoadSample);
                log.info("Sample Block Name (via WRITE): {}, size {}", sampleData.getName(), "%02X".formatted(sampleData.getSize()));
                try {
                    Files.write(Path.of("Sample Info %d.dat".formatted(objectId)), sampleInfo.getName()
                            .getBytes());
                    Files.write(Path.of("Sample Block %d.dat".formatted(objectId)), sampleData.getPayload());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                ByteBuffer b = ByteBuffer.wrap(sampleData.getPayload());
                b.order(ByteOrder.BIG_ENDIAN);
                // SFH struct start at 0xC
                b.position(0x0C);
                int rootNote = b.get() + 12;
                int mask = b.get();
                float amp1 = b.get() * 0.5f;
                float amp2 = b.get() * 0.5f;
                int pitch = 6147 - (b.getShort() & 0xFFFF);
                int nameOffset = b.getShort();
                // loop offsets ??
                // 87803 is encoded to $156FB but in the payload we have $C156FD, a jump of $C bytes/samples ?
                b.position(0x1C);
                int sos = b.getInt() & 0x000FFFFF;
                int alt = b.getInt() & 0x000FFFFF;
                int los = b.getInt() & 0x000FFFFF;
                int eos = b.getInt() & 0x000FFFFF;

                short rawDecay = b.getShort(0x30);
                int dbPerSecDecay = Math.abs(rawDecay) / 4;

                short rawRelease = b.getShort(0x34);
                int dbPerSecRelease = Math.abs(rawRelease) / 4;

                int loopType = (mask & 0b00000011); // normal, reverse, bidirect
                boolean ignoreRel = (mask & 0b00000100) != 0;
                boolean altSwitch = (mask & 0b00001000) != 0;
                boolean ramBased = (mask & 0b00010000) != 0;
                boolean shareWare = (mask & 0b00100000) != 0;
                boolean needsLoad = (mask & 0b01000000) != 0;
                boolean loopOff = (mask & 0b10000000) != 0;
                log.info("Root Note: {} = {}", rootNote, MidiNote.fromValue(rootNote)
                        .name());
                log.info("loopType: {}", loopType);
                log.info("ignoreRel: {}", ignoreRel);
                log.info("altSwitch: {}", altSwitch);
                log.info("ramBased: {}", ramBased);
                log.info("shareWare: {}", shareWare);
                log.info("needsLoad: {}", needsLoad);
                log.info("loopOff: {}", loopOff);

                log.info("amp1: {} dB (Volume adjust)", amp1);
                log.info("amp2: {} dB (Alt Volume Adjust)", amp2);
                log.info("pitch: {} cents", pitch);
                log.info("nameOffset: {}", nameOffset);
                log.info("Decay Rate : {} dB/sec", dbPerSecDecay);
                log.info("Release Rate : {} dB/sec", dbPerSecRelease);
                log.info("sos: {} (LOOP START en sample sur 24 bits ?)", sos);
                log.info("alt: {}  (LOOP END en samples sur 24 bits ?)", alt);
                log.info("los: {}", los);
                log.info("eos: {}", eos);

                try {
                    Files.write(Path.of("target/output %s %s.dat".formatted(sampleData.getClass()
                            .getSimpleName(), sampleData.getFormat())), sampleData.getPayload());

                } catch (IOException e) {
                    throw new MidiError(e);
                }
            } else {
                log.error("SampleId {} does not exists", sampleId);
            }
        });
    }

    public void listProgramBanks(String deviceName) {
        converseWith(deviceName, (device, input, output) -> {
            CommandCall cmd = CommandCall.parse(device, "READ_PROGRAM_BANKS_NIBBLE()")
                    .getFirst();
            MidiRequestResponse response = midiDeviceRequester.queryAndAggregate(device, input, output, cmd, null);
            kurzweilSysExParser.parse(response.response());
        });
    }

    private void sendSample(Path sysEx, MidiDeviceDefinition device, MidiInPort input, MidiOutPort output, KFSoundBlock soundBlock) {
        Path f = Path.of(sysEx.getParent()
                .toString(), "%d - %s.pcm".formatted(soundBlock.getObjectId(), soundBlock.getName()));
        String objectId = MidiEventBuilder.to14Bits(soundBlock.getObjectId(), false);
        skip.set(true);
        ObjectInfo currentSample = query(device, input, output, "DIR_SOUND_BLOCK(%s)".formatted(objectId));
        try {
            if (currentSample.getSize() == 0) {
                log.info("{} {}: Sample not present, uploading...", currentSample.getObjectId(), currentSample.getName());
                if (f.toFile()
                        .exists()) {
                    var sbHeader = soundBlock.getHeaders()
                            .getFirst();
                    byte[] data = Files.readAllBytes(f);
                    SampleDumpStandard sampleDumpStandard = new SampleDumpStandard(device, input, output);
                    DumpHeader header = new DumpHeader(0,
                            soundBlock.getObjectId() - 1,
                            sbHeader.bitDepth(),
                            sbHeader.sampleRate(),
                            (int) sbHeader.sampleLength(),
                            LoopType.FORWARD_ONLY,
                            (int) sbHeader.sampleLoopStart(),
                            (int) sbHeader.sampleEnd());
                    sampleDumpStandard.sendSample(header, data);
                    skip.set(false);
                    updateSoundBlock(device, input, output, soundBlock, objectId);
                }
            } else {
                skip.set(false);
                if (currentSample.getName()
                        .equals(soundBlock.getName())) {
                    log.info("{} {}: Sample present, not uploading again", currentSample.getObjectId(), currentSample.getName());
                    updateSoundBlock(device, input, output, soundBlock, objectId);
                } else {
                    log.info("{} {}: Another sample present ({}), need to relocate objectId for {}", currentSample.getObjectId(), currentSample.getName(),
                            currentSample.getName(), soundBlock.getName());
                }
            }
        } catch (IOException | InvalidMidiDataException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void updateSoundBlock(MidiDeviceDefinition device, MidiInPort input, MidiOutPort output, KFSoundBlock newSoundBlock, String objectId) throws InvalidMidiDataException, InterruptedException, IOException {
        ObjectWrite actualSampleBlock = query(device, input, output, "READ_SOUND_BLOCK_NIBBLE(%s)".formatted(objectId));
        KFSoundBlock actualKfSoundBlock = (KFSoundBlock) actualSampleBlock.getKfObject();
        actualKfSoundBlock.getHeaders()
                .forEach(h -> {
                    log.info("Actual Sound Block: {}", KFDeserializer.toJson(h));
                });
        long baseMemoryAddress = actualKfSoundBlock.getHeaders()
                .getFirst()
                .sampleStart();
        log.info("Sound block {} located at address ${}/{}", objectId, "%X".formatted(baseMemoryAddress), baseMemoryAddress);
        newSoundBlock.getHeaders()
                .forEach(h -> {
                    log.info("Before relocation: {}", KFDeserializer.toJson(h));
                    h.relocate(baseMemoryAddress);
                    log.info("After relocation: {}", KFDeserializer.toJson(h));
                });

        KurzweilFileConverter kurzweilFileConverter = new KurzweilFileConverter();
        ByteArrayOutputStream sysex = new ByteArrayOutputStream();
        BitStreamWriter out = new BitStreamWriter();
        KFSoundBlockDeserializer soundBlockDeserializer = new KFSoundBlockDeserializer();
        soundBlockDeserializer.serializeContent(newSoundBlock, out);
        byte[] unpackedContent = out.toByteArray();
        kurzweilFileConverter.writeObject(sysex, newSoundBlock, unpackedContent);
        byte[] payload = sysex.toByteArray();
        MidiEvent evt = new CustomMidiEvent(new SysexMessage(payload, payload.length));
        output.send(evt);
        signalQueue.take();
    }

    private <T extends BaseObject> T query(MidiDeviceDefinition device, MidiInPort input, MidiOutPort output, String cmtDirSample) {
        return (T) midiDeviceRequester.query(device, input, output, CommandCall.parse(device, cmtDirSample)
                        .getFirst())
                .map(response -> {
                    try {
                        Files.write(Path.of("target/Object dump.syx"), response.response());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    return kurzweilSysExParser.parse(response.response())
                            .getFirst();
                })
                .orElseThrow();
    }

    private <T extends BaseObject> List<T> queryList(MidiDeviceDefinition device, MidiInPort input, MidiOutPort output, String cmtDirSample) {
        var response = midiDeviceRequester.queryAndAggregate(device, input, output, CommandCall.parse(device, cmtDirSample)
                .getFirst(), null);
        return (List<T>) kurzweilSysExParser.parse(response.response());
    }

    private void converseWith(String deviceName, DeviceConversation deviceConversation) {
        var device = library.getDevice(deviceName)
                .orElseThrow();
        try (var output = midiPortsManager.getOutput(device.getOutputMidiDevice())
                .orElse(null)) {
            if (output == null) {
                log.error("Output MIDI Device not found: {}", device.getOutputMidiDevice());
                return;
            }
            try (var input = midiPortsManager.getInput(device.getInputMidiDevice())
                    .orElse(null)) {
                if (input == null) {
                    log.error("Input MIDI Device not found: {}", device.getInputMidiDevice());
                    return;
                }
                output.open();
                input.open();
                deviceConversation.execute(device, input, output);
            }
        }
    }

    @PostConstruct
    private void init() {
        library.load(ConfigHelper.getApplicationFolder(this.getClass()));
        midiPortsManager.collectHardwareDevices();
    }
}
