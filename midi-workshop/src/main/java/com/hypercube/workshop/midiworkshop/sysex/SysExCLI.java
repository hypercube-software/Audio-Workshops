package com.hypercube.workshop.midiworkshop.sysex;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.hypercube.workshop.midiworkshop.api.MidiPortsManager;
import com.hypercube.workshop.midiworkshop.api.config.ConfigHelper;
import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import com.hypercube.workshop.midiworkshop.api.ports.local.in.MidiInPort;
import com.hypercube.workshop.midiworkshop.api.ports.local.out.MidiOutPort;
import com.hypercube.workshop.midiworkshop.api.presets.crawler.CrawlingDomain;
import com.hypercube.workshop.midiworkshop.api.presets.crawler.MidiPresetCrawler;
import com.hypercube.workshop.midiworkshop.api.sysex.device.Device;
import com.hypercube.workshop.midiworkshop.api.sysex.device.Devices;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.dump.DeviceMemoryDumper;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.dump.DeviceMemoryVisitor;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.MemorySpace;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.MemorySpaceAddressing;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.map.MemoryField;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.map.MemoryMap;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.map.MemoryMapFormat;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.primitives.MemoryInt28;
import com.hypercube.workshop.midiworkshop.api.sysex.library.MidiDeviceLibrary;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceBank;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceDefinition;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceMode;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDevicePreset;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.Manufacturer;
import com.hypercube.workshop.midiworkshop.api.sysex.parser.SysExParser;
import com.hypercube.workshop.midiworkshop.api.sysex.yaml.mixin.MidiDeviceBankMixin;
import com.hypercube.workshop.midiworkshop.api.sysex.yaml.mixin.MidiDeviceDefinitionMixin;
import com.hypercube.workshop.midiworkshop.api.sysex.yaml.mixin.MidiDeviceModeMixin;
import com.hypercube.workshop.midiworkshop.api.sysex.yaml.serializer.MidiDevicePresetSerializer;
import com.hypercube.workshop.midiworkshop.monitor.MidiMonitor;
import com.hypercube.workshop.midiworkshop.monitor.MidiMonitorEventListener;
import com.hypercube.workshop.midiworkshop.sysex.samplers.SampleDumpStandardExample;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.SysexMessage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.hypercube.workshop.midiworkshop.api.sysex.util.SysExConstants.SYSEX_GENERAL_INFORMATION;
import static com.hypercube.workshop.midiworkshop.api.sysex.util.SysExConstants.SYSEX_IDENTITY_RESPONSE;

@ShellComponent()
@ShellCommandGroup("SysEx CLI")
@Slf4j
@RequiredArgsConstructor
public class SysExCLI {
    private final SysExParser sysExParser;
    private final MidiMonitor midiMonitor;
    private final MidiPresetCrawler midiPresetCrawler;
    private final MidiDeviceLibrary library;
    private final MidiPortsManager midiPortsManager;
    private final SampleDumpStandardExample sampleDumpStandardExample;
    private final CyclicBarrier listenerThreadReady = new CyclicBarrier(2);
    private final Semaphore sysExReceived = new Semaphore(0);

    @ShellMethod(value = "parse SysEx file and dump the device memory to disk")
    public void parse(@ShellOption(value = "-i", help = "*.syx file") File input, @ShellOption(value = "-o", help = "Memory Dump in TXT format") File output) {
        Device d = sysExParser.parse(input);
        if (d != null) {
            DeviceMemoryDumper dumper = new DeviceMemoryDumper(d.getMemory());
            dumper.dumpMemoryMap(new File("target/memorymap.txt"));
            dumper.dumpMemory(output);
        }
    }


    @ShellMethod(value = "read device identity")
    public void queryIdentity(
            @ShellOption(value = "-i", help = "Input MIDI Port") String inputDevice,
            @ShellOption(value = "-o", help = "Output MIDI Port") String outputDevice,
            @ShellOption(value = "-f") File output) throws IOException, MidiUnavailableException, InterruptedException {
        MidiPortsManager m = new MidiPortsManager();
        m.collectHardwareDevices();
        m.listPorts();
        try (var out = m.openOutput(outputDevice)) {
            try (var in = m.openInput(inputDevice)) {
                AtomicInteger total = new AtomicInteger();
                MidiMonitorEventListener listener = (device, evt) -> onMidiEvent(device, evt, output, total);
                Thread listenerThread = initListener(in, listener);
                Device.sendIdentityRequest(out, 0);
                int nbSysExReceived = 0;
                while (waitSysExReceived()) {
                    nbSysExReceived++;
                    log.info(nbSysExReceived + " SysEx received");
                }
                if (nbSysExReceived == 0) {
                    throw new MidiError("No SysEx received, your device does not recognize Identity Request");
                }
                midiMonitor.close();
                listenerThread.join();
            }
        }
    }

    @ShellMethod(value = "read device memory")
    public void readMemory(@ShellOption(value = "-m", help = "Device Model (any manufacturer)") String modelName,
                           @ShellOption(value = "-r", defaultValue = "", help = "Memory space name (optional when the mmap has only Default)") String memorySpaceName,
                           @ShellOption(value = "-p", defaultValue = "-1", help = "Preset number for preset-addressed spaces") int presetNumber,
                           @ShellOption(value = "-i", help = "Input MIDI Port") String inputDevice,
                           @ShellOption(value = "-o", help = "Output MIDI Port") String outputDevice,
                           @ShellOption(value = "-a", help = "Packed address of a parameter (2 to 4 MIDI bytes, e.g. 0128 or 400000)") String address,
                           @ShellOption(value = "-s", help = "Parameter size in bytes") int size) throws IOException, MidiUnavailableException {
        MidiPortsManager m = new MidiPortsManager();
        m.collectHardwareDevices();
        m.listPorts();

        var device = Manufacturer.searchDevice(modelName);
        device.loadMemoryMap();
        MemorySpace memorySpace = device.getMemory()
                .resolveMemorySpace(memorySpaceName);
        System.setProperty(Devices.SYSTEM_PROPERTY_FORCE_DEVICE, modelName);
        try (var out = m.openOutput(outputDevice)) {
            try (var in = m.openInput(inputDevice)) {
                MemoryInt28 paramAddress = MemoryInt28.from(address, true);
                MemoryInt28 paramSize = MemoryInt28.from(size, paramAddress.byteCount());
                log.info("Query device in memory space {} at address {} with size {}", memorySpace.getName(), paramAddress, paramSize);
                int value = switch (device.getMemorySpaceAddressing(memorySpace.getName())) {
                    case BYTE -> device.requestMemory(in, out, memorySpace.getName(), paramAddress, paramSize);
                    case PRESET -> {
                        if (presetNumber < 0) {
                            throw new MidiError("Memory space %s is preset-addressed, use -p".formatted(memorySpace.getName()));
                        }
                        yield device.requestPresetMemory(in, out, memorySpace.getName(), presetNumber, paramAddress);
                    }
                };
                log.info("Value at {} is {}", paramAddress, value);
            }
        }
    }

    @ShellMethod(value = "Use the device memory map to query data and save everything to disk as SysEx file")
    public void dumpMemory(@ShellOption(value = "-m", help = "Device Model (any manufacturer)") String modelName,
                           @ShellOption(value = "-r", defaultValue = "", help = "Memory space name (optional when the mmap has only Default)") String memorySpaceName,
                           @ShellOption(value = "-p", defaultValue = "-1", help = "Preset number for preset-addressed spaces") int presetNumber,
                           @ShellOption(value = "-i", defaultValue = "") String inputDevice,
                           @ShellOption(value = "-o") String outputDevice,
                           @ShellOption(value = "-f") File output) throws InterruptedException, IOException {
        if (output.exists()) {
            Files.delete(output.toPath());
        }
        MidiPortsManager m = new MidiPortsManager();
        m.collectHardwareDevices();
        m.listPorts();

        try (var out = m.openOutput(outputDevice)) {
            try (var in = inputDevice.isEmpty() ? m.openInputs(m.getInputs()) : m.openInput(inputDevice)) {
                var model = Manufacturer.searchDevice(modelName);
                model.loadMemoryMap();
                System.setProperty(Devices.SYSTEM_PROPERTY_FORCE_DEVICE, modelName);
                MemorySpace memorySpace = model.getMemory()
                        .resolveMemorySpace(memorySpaceName);
                MemorySpaceAddressing addressing = MemorySpace.DEFAULT_NAME.equals(memorySpace.getName())
                        ? MemorySpaceAddressing.BYTE
                        : model.getMemorySpaceAddressing(memorySpace.getName());
                validatePresetNumber(memorySpace, addressing, presetNumber);
                DeviceMemoryDumper dumper = new DeviceMemoryDumper(model.getMemory());
                AtomicInteger total = new AtomicInteger();
                MidiMonitorEventListener listener = (device, evt) -> onMidiEvent(device, evt, output, total);

                sysExReceived.drainPermits();
                Thread listenerThread = initListener(in, listener);
                if (MemorySpace.DEFAULT_NAME.equals(memorySpace.getName())) {
                    receiveNonBulkMemory(dumper, model, out);
                    receiveBulkMemory(model, out);
                } else {
                    dumpMemorySpace(model, memorySpace, addressing, presetNumber, out);
                }
                dumper.dumpMemoryMap(new File("target/map.dat"));
                dumper.dumpMemory(new File("target/mem.dat"));
                midiMonitor.close();
                listenerThread.join();
            }
        }
    }

    private void validatePresetNumber(MemorySpace memorySpace, MemorySpaceAddressing addressing, int presetNumber) {
        if (addressing == MemorySpaceAddressing.PRESET) {
            if (presetNumber < 0) {
                throw new MidiError("Memory space %s is preset-addressed, use -p".formatted(memorySpace.getName()));
            }
        } else if (presetNumber >= 0) {
            throw new MidiError("Preset number is only valid for preset-addressed memory spaces");
        }
    }

    /**
     * Dump a named memory space. A byte-addressed space is dumped zone by zone,
     * in declaration order, with blocks limited by the device.
     */
    private void dumpMemorySpace(Device device, MemorySpace memorySpace, MemorySpaceAddressing addressing,
                                 int presetNumber, MidiOutPort out) {
        switch (addressing) {
            case BYTE -> dumpByteMemorySpace(device, memorySpace, out);
            case PRESET -> {
                log.info("Dump memory space {} preset {}", memorySpace.getName(), presetNumber);
                device.requestPreset(out, memorySpace.getName(), presetNumber);
                if (!waitSysExReceived(2_000)) {
                    throw new MidiError("Timeout waiting SysEx for memory space " + memorySpace.getName());
                }
            }
        }
    }

    private void dumpByteMemorySpace(Device device, MemorySpace memorySpace, MidiOutPort out) {
        int maxBlockSize = device.getMaxBlockSize(memorySpace.getName());
        for (MemoryMap zone : memorySpace.getMemoryMaps()) {
            int total = zone.getEffectiveSize()
                    .value();
            int offset = 0;
            while (offset < total) {
                int blockSize = Math.min(maxBlockSize, total - offset);
                MemoryInt28 baseAddress = zone.getBaseAddress();
                MemoryInt28 address = MemoryInt28.from(baseAddress.value() + offset, baseAddress.byteCount());
                MemoryInt28 size = MemoryInt28.from(blockSize, baseAddress.byteCount());
                log.info("Dump {} zone {} {}/{} at {}", memorySpace.getName(), zone.getName(), offset, total, address);
                device.requestMemorySpace(out, memorySpace.getName(), address, size);
                if (!waitSysExReceived(2_000)) {
                    throw new MidiError("Timeout waiting SysEx for memory space %s zone %s at %s"
                            .formatted(memorySpace.getName(), zone.getName(), address));
                }
                offset += blockSize;
            }
        }
    }

    @ShellMethod(value = "Use the device library to dump all presets names of a synth")
    public void dumpPresets(@ShellOption(value = "-d", help = "Device Name") String deviceName) throws InterruptedException, IOException {
        library.load(ConfigHelper.getApplicationFolder(this.getClass()));
        midiPortsManager.collectHardwareDevices();
        var mapper = new ObjectMapper(new YAMLFactory());
        mapper.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
        mapper.addMixIn(MidiDeviceBank.class, MidiDeviceBankMixin.class);
        mapper.addMixIn(MidiDeviceDefinition.class, MidiDeviceDefinitionMixin.class);
        mapper.addMixIn(MidiDeviceMode.class, MidiDeviceModeMixin.class);
        SimpleModule module = new SimpleModule();
        module.addSerializer(MidiDevicePreset.class, new MidiDevicePresetSerializer());
        mapper.registerModule(module);
        try (PrintWriter out = new PrintWriter(new FileOutputStream("%s-presets.yml".formatted(deviceName)))) {
            MidiDeviceDefinition devicePresets = new MidiDeviceDefinition();
            midiPresetCrawler.crawlAllPatches(CrawlingDomain.everything(deviceName), (device, midiPreset, currentCount, totalCount) -> {
                devicePresets.setDeviceName(device.getDeviceName());
                devicePresets.setBrand(device.getBrand());
                String modeName = midiPreset.getId()
                        .deviceMode();
                String bankName = midiPreset.getId()
                        .bankName();
                MidiDeviceMode mode = devicePresets.getDeviceModes()
                        .get(modeName);
                if (mode == null) {
                    mode = new MidiDeviceMode();
                    mode.setName(modeName);
                    devicePresets.getDeviceModes()
                            .put(modeName, mode);
                }
                MidiDeviceBank bank = mode.getBank(bankName)
                        .orElse(null);
                if (bank == null) {
                    bank = new MidiDeviceBank();
                    bank.setName(bankName);
                    bank.setPresetFormat(device.getPresetFormat());
                    mode.getBanks()
                            .put(bankName, bank);
                }
                String command = midiPreset.getCommand();
                List<String> drumMap = midiPreset.getDrumKitNotes()
                        .stream()
                        .map(drumKitNote -> "%02X | %s".formatted(drumKitNote.note(), drumKitNote.title()))
                        .toList();
                MidiDevicePreset midiDevicePreset = new MidiDevicePreset(null, midiPreset.getId()
                        .name(), command, midiPreset.getId()
                        .category(), "", drumMap);
                bank.getPresets()
                        .add(midiDevicePreset);
            }, null);
            mapper.writeValue(out, devicePresets);
        }
    }

    @ShellMethod("Request a sample via Sample Dump Standard")
    public void sdsRequest(@ShellOption(value = "-d", help = "Device Name") String deviceName,
                           @ShellOption(value = "-s", help = "Sample ID") int sampleId,
                           @ShellOption(value = "-f", help = "Output PCM file") String filename
    ) {
        sampleDumpStandardExample.request(deviceName, sampleId, filename);
    }

    private void receiveBulkMemory(Device model, MidiOutPort out) {
        model.getMemory()
                .getMemoryMaps()
                .stream()
                .filter(MemoryMap::isTopLevel)
                .filter(mm -> mm.getFormat() == MemoryMapFormat.NIBBLES)
                .forEach(memoryMap -> {
                    log.info("{}:{} {}", memoryMap.getName(), memoryMap.getBaseAddress()
                            .toString(), memoryMap.getEffectiveSize()
                            .toString());
                    model.requestData(out, memoryMap.getBaseAddress(), memoryMap.getEffectiveSize());
                    int nbSysExReceived = 0;
                    while (waitSysExReceived()) {
                        nbSysExReceived++;
                        log.info(nbSysExReceived + " SysEx received");
                    }
                    if (nbSysExReceived == 0) {
                        throw new MidiError("Your MemoryMap is not correct, no SysEx received");
                    }
                });
    }

    private void receiveNonBulkMemory(DeviceMemoryDumper dumper, Device model, MidiOutPort out) {
        dumper.visitMemory(new DeviceMemoryVisitor() {
            @Override
            public void onNewTopLevelMemoryMap(MemoryMap memoryMap) {
                // Nothing to do
            }

            @Override
            public void onNewEntry(String path, MemoryField field, MemoryInt28 addr) {
                if (!path.contains("Bulk") && !field.isArray()) {
                    log.info("Visit {}, {} {} {} {}", path, field.getParent()
                            .getName(), addr.toString(), field.getName(), field.getSize()
                            .toString());
                    var size = field.getSize();
                    model.requestData(out, addr, size);
                    waitSysExReceived();
                }
            }
        });
    }

    private Thread initListener(MidiInPort inputDevice, MidiMonitorEventListener listener) throws InterruptedException {
        Thread listenerThread = new Thread(() -> {
            try {
                listenerThreadReady.await();
            } catch (Exception e) {
                throw new MidiError(e);
            }
            midiMonitor.monitor(inputDevice, listener);
        });
        listenerThread.start();
        waitListenerThread();
        Thread.sleep(1000);
        return listenerThread;
    }

    private boolean waitSysExReceived() {
        return waitSysExReceived(500);
    }

    private boolean waitSysExReceived(long timeoutMs) {
        try {
            return sysExReceived.tryAcquire(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MidiError(e);
        }
    }

    private void waitListenerThread() throws InterruptedException {
        try {
            listenerThreadReady.await();
        } catch (BrokenBarrierException e) {
            throw new MidiError(e);
        }
    }

    private void onMidiEvent(MidiInPort midiInPort, MidiEvent evt, File output, AtomicInteger total) {
        if (evt.getMessage()
                .getStatus() == SysexMessage.SYSTEM_EXCLUSIVE) {
            try {
                log.info("Receive SYSEX from midiInPort {}", midiInPort.getName());
                byte[] data = evt.getMessage()
                        .getMessage();
                if (data[3] == SYSEX_GENERAL_INFORMATION && data[4] == SYSEX_IDENTITY_RESPONSE) {
                    int manufacturerID = data[5];
                    if (manufacturerID == 0) {
                        manufacturerID = ((0xFF & data[6]) << 8) + (0xFF & data[7]);
                    }
                    String name = Manufacturer.search(manufacturerID)
                            .map(Manufacturer::getTitle)
                            .orElse("");
                    log.info("Manufacturer ID: 0x%04X %s".formatted(manufacturerID, name));
                }
                total.addAndGet(data.length);
                Files.write(output.toPath(), data, StandardOpenOption.APPEND, StandardOpenOption.CREATE);
                log.info("Current Size: %d , received %d bytes".formatted(total.get(), data.length));
                try {
                    sysExParser.parse(data);
                } catch (RuntimeException e) {
                    // Keep the raw frame on disk even if the layout cannot
                    // parse it (identity reply, unrelated device, ...).
                    log.warn("Unable to parse SysEx: {}", e.getMessage());
                }
                sysExReceived.release();
            } catch (IOException e) {
                throw new MidiError("Unable to write to " + output.getAbsolutePath(), e);
            }
        }
    }
}
