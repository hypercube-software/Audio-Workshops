package com.hypercube.workshop.syntheditor.service;

import com.hypercube.workshop.midiworkshop.api.MidiPortsManager;
import com.hypercube.workshop.midiworkshop.api.devices.MidiInDevice;
import com.hypercube.workshop.midiworkshop.api.devices.MidiOutDevice;
import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import com.hypercube.workshop.midiworkshop.api.sysex.device.Devices;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.dump.DeviceMemoryDumper;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.dump.DeviceMemoryVisitor;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.map.MemoryField;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.map.MemoryMap;
import com.hypercube.workshop.midiworkshop.api.sysex.device.memory.primitives.MemoryInt24;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.Manufacturer;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.roland.RolandDevice;
import com.hypercube.workshop.syntheditor.infra.bus.SynthEditorBusListener;
import com.hypercube.workshop.syntheditor.infra.bus.WebSocketBus;
import com.hypercube.workshop.syntheditor.infra.bus.dto.ParameterUpdateDTO;
import com.hypercube.workshop.syntheditor.model.EditableParameter;
import com.hypercube.workshop.syntheditor.model.EditableParameters;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Setter
@Getter
@Slf4j
@RequiredArgsConstructor
public class SynthEditorService implements SynthEditorBusListener {
    private final WebSocketBus bus;
    private final MidiPortsManager midiPortsManager = new MidiPortsManager();
    private MidiInDevice inputDevice;
    private MidiOutDevice outputDevice;
    private RolandDevice device;
    private EditableParameters editableParameters = new EditableParameters();

    @PostConstruct
    public void init() {
        System.setProperty(Devices.SYSTEM_PROPERTY_FORCE_DEVICE, "DS-330"); // Because Roland don't use unique ids for Sound Canvas
        midiPortsManager.collectDevices();
        device = Manufacturer.ROLAND.getDevice("DS-330");
        collectDS330Parameters();
    }

    public void updateParameters() {
        if (inputDevice == null || outputDevice == null) {
            return;
        }
        inputDevice.startListening();
        int progress = 0;
        var params = editableParameters.getAll();
        for (int i = 0; i < params.size(); i++) {
            var p = params.get(i);
            log.info("Query param " + p.getPath() + " of size %d at 0x%06X".formatted(p.getSize(), p.getAddress()));
            int value = device.requestMemory(inputDevice, outputDevice, MemoryInt24.fromPacked(p.getAddress()), MemoryInt24.from(p.getSize()));
            p.setValue(value);
            progress = (i + 1) * 100 / params.size();
            bus.sendProgress(progress);
        }
        inputDevice.stopListening();
        inputDevice.waitNotListening();
    }

    public synchronized void closeCurrentInputDevice() {
        if (inputDevice != null) {
            log.info("Close INPUT Device: {}", inputDevice.getName());
            inputDevice.close();
            inputDevice = null;
        }
    }

    public synchronized void closeCurrentOutputDevice() {
        if (outputDevice != null) {
            log.info("Close OUTPUT Device: {}", outputDevice.getName());
            outputDevice.close();
            outputDevice = null;
        }
    }

    public synchronized void changeInput(String deviceName) {
        closeCurrentInputDevice();
        inputDevice = midiPortsManager.getInput(deviceName)
                .orElseThrow(() -> new MidiError("Device not found:" + deviceName));
        log.info("Open INPUT Device: {}", deviceName);
        inputDevice.open();
    }

    public synchronized void changeOutput(String deviceName) {
        closeCurrentOutputDevice();
        outputDevice = midiPortsManager.getOutput(deviceName)
                .orElseThrow(() -> new MidiError("Device not found:" + deviceName));
        log.info("Open OUTPUT Device: {}", deviceName);
        outputDevice.open();
    }

    @Override
    public void onMsg(ParameterUpdateDTO parameterUpdateDTO) {
        if (outputDevice != null) {
            device.sendData(outputDevice, MemoryInt24.fromPacked(parameterUpdateDTO.getAddress()), parameterUpdateDTO.getValue());
        }
    }

    @Override
    public void onSessionClosed() {
        closeCurrentInputDevice();
        closeCurrentOutputDevice();
    }

    private void collectDS330Parameters() {
        editableParameters.clear();
        device.loadMemoryMap();
        DeviceMemoryDumper dumper = new DeviceMemoryDumper(device.getMemory());
        dumper.visitMemory(new DeviceMemoryVisitor() {
            final List<String> mapOfInterest = List.of("CommonPatchParams Zone", "PatchParams Zone", "PatchSends");
            MemoryMap currentMap;

            @Override
            public void onNewTopLevelMemoryMap(MemoryMap memoryMap) {
                currentMap = memoryMap;
            }

            @Override
            public void onNewEntry(String path, MemoryField field, MemoryInt24 addr) {
                if (mapOfInterest.contains(currentMap.getName()) && !field.getName()
                        .equals("reserved")) {
                    String paramPath = path.substring(currentMap.getName()
                            .length() + 1);
                    editableParameters.add(new EditableParameter(paramPath, addr.packedValue(), field.getSize()
                            .value(), 0));
                }
            }
        });
    }
}
