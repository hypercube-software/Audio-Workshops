package com.hypercube.workshop.midiworkshop.sysex;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.hypercube.workshop.midiworkshop.api.MidiPortsManager;
import com.hypercube.workshop.midiworkshop.api.devices.MidiInDevice;
import com.hypercube.workshop.midiworkshop.api.devices.MidiOutDevice;
import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import com.hypercube.workshop.midiworkshop.api.presets.generic.MidiPresetCrawler;
import com.hypercube.workshop.midiworkshop.api.presets.yamaha.CS1XPresetGenerator;
import com.hypercube.workshop.midiworkshop.api.sysex.device.Device;
import com.hypercube.workshop.midiworkshop.api.sysex.device.Devices;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.dump.DeviceMemoryDumper;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.dump.DeviceMemoryVisitor;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.map.MemoryField;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.map.MemoryMap;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.map.MemoryMapFormat;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.primitives.MemoryInt24;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
    private CyclicBarrier listenerThreadReady = new CyclicBarrier(2);
    private CyclicBarrier sysExReceived = new CyclicBarrier(2);

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
        m.collectDevices();
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
    public void readMemory(@ShellOption(value = "-m", help = "Device Model") String modelName,
                           @ShellOption(value = "-i", help = "Input MIDI Port") String inputDevice,
                           @ShellOption(value = "-o", help = "Output MIDI Port") String outputDevice,
                           @ShellOption(value = "-a", help = "Roland Packed Address of a parameter") String address,
                           @ShellOption(value = "-s", help = "Parameter size in bytes") int size) throws IOException, MidiUnavailableException {
        MidiPortsManager m = new MidiPortsManager();
        m.collectDevices();
        m.listPorts();

        var device = Manufacturer.ROLAND.getDevice(modelName);
        device.loadMemoryMap();
        System.setProperty(Devices.SYSTEM_PROPERTY_FORCE_DEVICE, modelName);
        try (var out = m.openOutput(outputDevice)) {
            try (var in = m.openInput(inputDevice)) {
                in.startListening();
                MemoryInt24 paramSize = MemoryInt24.from(size);
                MemoryInt24 paramAddress = MemoryInt24.from(address, true);
                log.info("Query device at address {} with size {}", paramAddress, paramSize);
                int value = device.requestMemory(in, out, paramAddress, paramSize);
                log.info("Value at {} is {}", paramAddress, value);
                in.stopListening();
                in.waitNotListening();
            }
        }
    }

    @ShellMethod(value = "Use the device memory map to query data and save everything to disk as SysEx file")
    public void dumpMemory(@ShellOption(value = "-m", help = "Device Model") String modelName, @ShellOption(value = "-i", defaultValue = "") String inputDevice, @ShellOption(value = "-o") String outputDevice, @ShellOption(value = "-f") File output) throws InterruptedException, IOException {
        if (output.exists()) {
            Files.delete(output.toPath());
        }
        MidiPortsManager m = new MidiPortsManager();
        m.collectDevices();
        m.listPorts();

        try (var out = m.openOutput(outputDevice)) {
            try (var in = inputDevice.isEmpty() ? m.openInputs(m.getInputs()) : m.openInput(inputDevice)) {
                var model = Manufacturer.ROLAND.getDevice(modelName);
                model.loadMemoryMap();
                DeviceMemoryDumper dumper = new DeviceMemoryDumper(model.getMemory());
                dumper.dumpMemoryMap(new File("target/map.dat"));
                dumper.dumpMemory(new File("target/mem.dat"));
                AtomicInteger total = new AtomicInteger();
                MidiMonitorEventListener listener = (device, evt) -> onMidiEvent(device, evt, output, total);

                Thread listenerThread = initListener(in, listener);
                receiveNonBulkMemory(dumper, model, out);
                receiveBulkMemory(model, out);
                midiMonitor.close();
                listenerThread.join();
            }
        }
    }

    @ShellMethod(value = "Use the device library to dump all presets names of a synth")
    public void dumpPresets(@ShellOption(value = "-d", help = "Device Name") String deviceName) throws InterruptedException, IOException {
        MidiPresetCrawler midiPresetCrawler = new MidiPresetCrawler();
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
            midiPresetCrawler.crawlAllPatches(deviceName, (device, midiPreset) -> {
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
            });
            mapper.writeValue(out, devicePresets);
        }
    }

    @ShellMethod("Generate CS1X voices SysEx")
    public void dumpCS1XVoices(@ShellOption(value = "-d", help = "Device Name") String deviceName) throws InterruptedException, IOException {
        CS1XPresetGenerator cs1XPresetGenerator = new CS1XPresetGenerator();
        cs1XPresetGenerator.dumpCS1XVoices(deviceName);
    }

    private void receiveBulkMemory(Device model, MidiOutDevice out) {
        model.getMemory()
                .getMemoryMaps()
                .stream()
                .filter(MemoryMap::isTopLevel)
                .filter(mm -> mm.getFormat() == MemoryMapFormat.NIBBLES)
                .forEach(memoryMap -> {
                    log.info(memoryMap.getName() + ":" + memoryMap.getBaseAddress()
                            .toString() + " " + memoryMap.getEffectiveSize()
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
                    sysExReceived.reset();
                });
    }

    private void receiveNonBulkMemory(DeviceMemoryDumper dumper, Device model, MidiOutDevice out) {
        dumper.visitMemory(new DeviceMemoryVisitor() {
            @Override
            public void onNewTopLevelMemoryMap(MemoryMap memoryMap) {
                // Nothing to do
            }

            @Override
            public void onNewEntry(String path, MemoryField field, MemoryInt24 addr) {
                if (!path.contains("Bulk") && !field.isArray()) {
                    log.info("Visit {}, {} {} {} {}", path, field.getParent()
                            .getName(), addr.toString(), field.getName(), field.getSize()
                            .toString());
                    var size = field.getSize();
                    model.requestData(out, addr, size);
                    waitSysExReceived();
                    sysExReceived.reset();
                }
            }
        });
    }

    private Thread initListener(MidiInDevice inputDevice, MidiMonitorEventListener listener) throws InterruptedException {
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
        try {
            sysExReceived.await(500, TimeUnit.MILLISECONDS);
            return true;
        } catch (TimeoutException e) {
            return false;
        } catch (Exception e) {
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

    private void onMidiEvent(MidiInDevice device, MidiEvent evt, File output, AtomicInteger total) {
        if (evt.getMessage()
                .getStatus() == SysexMessage.SYSTEM_EXCLUSIVE) {
            try {
                log.info("Receive SYSEX from device " + device.getName());
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
                sysExReceived.await();
            } catch (IOException | InterruptedException | BrokenBarrierException e) {
                throw new MidiError("Unable to write to " + output.getAbsolutePath(), e);
            }
        }
    }
}
