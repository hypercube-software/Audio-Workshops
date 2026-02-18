package com.hypercube.workshop.midiworkshop.api.sysex.library.io;

import com.hypercube.workshop.midiworkshop.api.CustomMidiEvent;
import com.hypercube.workshop.midiworkshop.api.devices.MidiInDevice;
import com.hypercube.workshop.midiworkshop.api.devices.MidiOutDevice;
import com.hypercube.workshop.midiworkshop.api.errors.MidiConfigError;
import com.hypercube.workshop.midiworkshop.api.sysex.library.MidiRequestSequence;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceDefinition;
import com.hypercube.workshop.midiworkshop.api.sysex.library.io.request.MidiRequest;
import com.hypercube.workshop.midiworkshop.api.sysex.library.io.response.MidiRequestResponse;
import com.hypercube.workshop.midiworkshop.api.sysex.library.io.response.MidiResponseMapper;
import com.hypercube.workshop.midiworkshop.api.sysex.library.io.response.SysExListener;
import com.hypercube.workshop.midiworkshop.api.sysex.macro.CommandCall;
import com.hypercube.workshop.midiworkshop.api.sysex.macro.CommandMacro;
import com.hypercube.workshop.midiworkshop.api.sysex.util.MidiEventBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class MidiDeviceRequester {
    public static final String MACRO_CALL_SEPARATOR = ";";

    /**
     * Send a list of commands to a device ignoring potential responses
     */
    public void updateDevice(MidiDeviceDefinition device, MidiInDevice midiInDevice, MidiOutDevice midiOutDevice, List<CommandCall> commands) {
        for (CommandCall commandCall : commands) {
            CommandMacro commandMacro = device.getMacro(commandCall);
            queryDevice(device, midiInDevice, midiOutDevice, commandMacro, commandCall);
        }
    }

    /**
     * Request a device and expect a response
     */
    public MidiRequestResponse queryDevice(MidiDeviceDefinition device, MidiInDevice midiInDevice, MidiOutDevice midiOutDevice, CommandMacro macro, CommandCall commandCall) {
        String errorMessage = null;
        try (ByteArrayOutputStream requestBuffer = new ByteArrayOutputStream()) {
            try (ByteArrayOutputStream responseBuffer = new ByteArrayOutputStream()) {
                for (MidiRequest r : forgeMidiRequestSequence(device, macro, commandCall).getMidiRequests()) {
                    List<CustomMidiEvent> events = MidiEventBuilder.parse(r.getValue());
                    final SysExListener listener;
                    Integer singleResponseSize = r.getResponseSize();
                    if (singleResponseSize != null) {
                        if (singleResponseSize > 0) {
                            int nbRequests = events.size();
                            int totalSize = singleResponseSize * nbRequests;
                            log.info("Send '{}' to {} and expected a response of {} * {} = {} bytes", r.getName(), device.getDeviceName(), nbRequests, singleResponseSize, totalSize);
                        } else {
                            log.info("Send '{}' to {} and expected a response of unknown size", r.getName(), device.getDeviceName());
                        }
                        listener = new SysExListener();
                        midiInDevice.addSysExListener(listener);
                    } else {
                        log.info("Send '{}' to {}", r.getName(), device.getDeviceName());
                        listener = null;
                    }
                    for (CustomMidiEvent evt : events) {
                        log.info("Send %s to %s".formatted(evt.getHexValuesSpaced(), device.getDeviceName()));
                        midiOutDevice.send(evt);
                        requestBuffer.write(evt.getMessage()
                                .getMessage());
                        if (listener != null) {
                            int expectedSize = singleResponseSize;
                            if (expectedSize > 0) {
                                errorMessage = waitDeviceResponse(device, listener, expectedSize);
                            } else if (expectedSize == 0) {
                                errorMessage = waitDeviceResponse(device, listener);
                            }
                            byte[] sysExResponse = listener.getSysExResponse();
                            responseBuffer.write(sysExResponse);
                            log.info("Receive %d (0x%X) bytes, %d (0x%X) in total".formatted(sysExResponse.length, sysExResponse.length, responseBuffer.size(), responseBuffer.size()));
                        }
                    }
                    if (listener != null) {
                        midiInDevice.removeListener(listener);
                    }
                }
                return new MidiRequestResponse(requestBuffer.toByteArray(), responseBuffer.toByteArray(), errorMessage);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Given a CommandMacro without parameters, expand it to get the list of MIDI messages
     *
     * @param device       device to query
     * @param commandMacro typically something like "getAll() : --- : A();B();C()"
     * @return The list of MIDI messages payloads
     */
    public MidiRequestSequence forgeMidiRequestSequence(MidiDeviceDefinition device, CommandMacro commandMacro, CommandCall commandCall) {
        File configFile = device.getDefinitionFile();
        String deviceName = device.getDeviceName();
        // if the mapper name is not set, mapper is null
        // if the mapper name is specified, we are looking for the corresponding MidiResponseMapper or raise an error if not found
        MidiResponseMapper mapper = Optional.ofNullable(commandMacro.getMapperName())
                .map(mapperName -> device.getMapper(mapperName)
                        .orElseThrow(() -> new MidiConfigError("Undefined request Mapper: '" + commandMacro.getMapperName() + "' in " + configFile.toString())))
                .orElse(null);
        List<MidiRequest> result = Arrays.stream(commandMacro.expand(commandCall)
                        .split(MACRO_CALL_SEPARATOR))
                .flatMap(
                        commandCallText -> expandWithPath(device, configFile, mapper, commandCallText, "/" + commandCall.name()).stream()
                )
                .toList();
        Integer totalSize = result.stream()
                .filter(r -> r.getResponseSize() != null)
                .mapToInt(MidiRequest::getResponseSize)
                .sum();
        if (totalSize > 0 && commandMacro.getResponseSize() != null && commandMacro.getResponseSize() > 0 && totalSize.intValue() != commandMacro.getResponseSize()) {
            throw new MidiConfigError("Response size of the macro '%s' is not the same as the calculated one: 0x%X (defined) != 0x%X (computed)".formatted(commandMacro.getName(), commandMacro.getResponseSize(), totalSize));
        }
        return new MidiRequestSequence(totalSize, result);
    }

    /**
     * Resolve recursively all macro calls in the command call. This mean we can do macros calling macros
     *
     * @param device      device owning the macro
     * @param commandCall command to expand
     * @return expanded command with its path
     */
    public List<MidiRequest> expand(MidiDeviceDefinition device, File configFile, MidiResponseMapper mapper, String commandCall) {
        return expandWithPath(device, configFile, mapper, commandCall, "");
    }

    /**
     * Recursively, expand a payloadBody using macro for a specific device
     *
     * @param device      device owning the macro
     * @param configFile  from where device macro are loaded
     * @param mapper      mapper used to read the response
     * @param payloadBody current payload to expand. Typically, contains a list of macro calls
     * @param path        keep the context of all resolved macros for debug
     * @return list of requests produced by payloadBody
     */
    public List<MidiRequest> expandWithPath(MidiDeviceDefinition device, File configFile, MidiResponseMapper mapper, String payloadBody, String path) {
        log.trace("Expand " + payloadBody);
        boolean hasMacroCall = payloadBody.contains("(");
        if (hasMacroCall) {
            return CommandCall.parse(configFile, payloadBody)
                    .stream()
                    .flatMap(commandCall -> {
                        String newPath = path + "/" + commandCall.name();
                        CommandMacro macro = device.getMacro(commandCall);
                        String expanded = macro.expand(commandCall);
                        MidiResponseMapper newMapper = macro.getMapperName() != null ? getMacroMapper(device, macro) : mapper;
                        // the expanded macro can contain again a list of macro commandCall, so we recurse after splitting with ";"
                        List<MidiRequest> midiRequests = Arrays.stream(expanded.split(MACRO_CALL_SEPARATOR))
                                .flatMap(expandedCommand -> expandWithPath(device, configFile, newMapper, expandedCommand, newPath).stream())
                                .toList();
                        if (macro.getResponseSize() != null) {
                            midiRequests.forEach(r -> {
                                r.setResponseSize(macro.getResponseSize());
                            });
                        }
                        return midiRequests.stream();
                    })
                    .toList();
        } else {
            // There is no more macro commandCall to resolve we return the string "as is" with its path
            MidiRequest midiRequest = new MidiRequest(path, payloadBody, mapper);
            return List.of(midiRequest);
        }
    }

    private MidiResponseMapper getMacroMapper(MidiDeviceDefinition device, CommandMacro macro) {
        return device.getMapper(macro.getMapperName())
                .orElseThrow(() -> new MidiConfigError("Unknown mapper '%s' for device '%s'".formatted(macro.getMapperName(), device.getDeviceName())));
    }

    /**
     * Wait a device response without knowing the size
     */
    private String waitDeviceResponse(MidiDeviceDefinition device, SysExListener listener) {
        String errorMessage = null;
        int previousSize = listener.getCurrentSize();
        Instant start = Instant.now();
        for (; ; ) {
            if (Duration.between(start, Instant.now())
                    .getSeconds() > 3) {
                int received = listener.getCurrentSize() - previousSize;
                if (received == 0) {
                    errorMessage = "No response from device %s".formatted(device.getDeviceName());
                } else {
                    return null; // we received something
                }
                log.error(errorMessage);
                break;
            }
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return errorMessage;
    }

    /**
     * Wait a response from the device with an exact size
     */
    private String waitDeviceResponse(MidiDeviceDefinition device, SysExListener listener, int expectedSize) {
        String errorMessage = null;
        int previousSize = listener.getCurrentSize();
        Instant start = Instant.now();
        while (listener.getCurrentSize() - previousSize < expectedSize) {
            if (Duration.between(start, Instant.now())
                    .getSeconds() > 3) {
                int received = listener.getCurrentSize() - previousSize;
                if (received == 0) {
                    errorMessage = "No response from device %s".formatted(device.getDeviceName());
                } else {
                    errorMessage = "Incomplete response from device %s, received only %d bytes over %d".formatted(device.getDeviceName(), received, expectedSize);
                }
                log.error(errorMessage);
                break;
            }
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return errorMessage;
    }

}
