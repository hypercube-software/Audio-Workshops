package com.hypercube.midi.translator.config.yaml;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.hypercube.midi.translator.config.project.ProjectConfiguration;
import com.hypercube.midi.translator.config.project.translation.MidiTranslation;
import com.hypercube.midi.translator.config.project.translation.MidiTranslationPayload;
import com.hypercube.midi.translator.error.ConfigError;
import com.hypercube.workshop.midiworkshop.common.sysex.library.MidiDeviceLibrary;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

/**
 * This deserializer rely on {@link MidiDeviceLibrary} to resolve macros on the fly. This mean the returned MidiTranslation does not contain any macro call in its value
 */
public class MidiTranslationDeserializer extends StdDeserializer<MidiTranslation> {
    private static final Pattern translationRegExp = Pattern.compile("(?<cc>[0-9]+)\\s*=>\\s*(?<payload>[^\\[\\]]+)($|(\\s+\\[(?<lowerBound>[+-0123456789]+),(?<upperBound>[+-0123456789]+)\\]))");
    private static final Pattern translationPayloadRegExp = Pattern.compile("\\s*([vA-F0-9]{2})");
    private final MidiDeviceLibrary midiDeviceLibrary;
    private final File configFile;

    public MidiTranslationDeserializer(MidiDeviceLibrary midiDeviceLibrary, File configFile) {
        super((Class<?>) null);
        this.midiDeviceLibrary = midiDeviceLibrary;
        this.configFile = configFile;
    }

    @Override
    public MidiTranslation deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        String translationDefinition = jsonParser.getText();
        var translationMatcher = translationRegExp.matcher(translationDefinition);
        if (translationMatcher.find()) {
            int cc = Integer.parseInt(translationMatcher.group("cc"));
            int lowerBound = Optional.ofNullable(translationMatcher.group("lowerBound"))
                    .map(Integer::parseInt)
                    .orElse(0);
            int upperBound = Optional.ofNullable(translationMatcher.group("upperBound"))
                    .map(Integer::parseInt)
                    .orElse(127);

            String rawPayload = translationMatcher.group("payload");
            // expand the macro if there is one
            ProjectConfiguration projectConfiguration = (ProjectConfiguration) jsonParser.getParsingContext()
                    .getParent()
                    .getCurrentValue();
            List<String> expandedTexts = midiDeviceLibrary.expand(configFile, projectConfiguration.getTranslate()
                    .getToDevice(), rawPayload);
            return forgeMidiTranslation(translationDefinition, cc, lowerBound, upperBound, expandedTexts);
        } else {
            throw new ConfigError("Unexpected Translation definition: " + translationDefinition);
        }
    }

    /**
     * Given a translation payload, properly expanded without any macro call, generate a {@link MidiTranslation}
     */
    private static MidiTranslation forgeMidiTranslation(String translationDefinition, int cc, int lowerBound, int upperBound, List<String> rawPayloads) {
        var payloads = rawPayloads.stream()
                .map(rawPayload -> {
                    var payloadMatcher = translationPayloadRegExp.matcher(rawPayload);
                    // we need to convert the payload string in a list of bytes
                    // "vv" will be repplaced by 0, and we store its position in valueIndex
                    int valueIndex = -1;
                    List<Byte> list = new ArrayList<>();
                    while (payloadMatcher.find()) {
                        String byteValue = payloadMatcher.group(1);
                        if (byteValue.equals("vv")) {
                            valueIndex = list.size();
                            list.add((byte) 0);
                        } else {
                            try {
                                list.add((byte) Integer.parseInt(byteValue, 16));
                            } catch (NumberFormatException e) {
                                throw new ConfigError("Unexpected Translation definition: '" + translationDefinition + "' for value '" + byteValue + "'");
                            }
                        }
                    }
                    byte[] payload = new byte[list.size()];
                    IntStream.range(0, list.size())
                            .forEach(i -> payload[i] = list.get(i));
                    return new MidiTranslationPayload(valueIndex, payload);
                })
                .toList();
        return new MidiTranslation(cc, lowerBound, upperBound, payloads);
    }
}
