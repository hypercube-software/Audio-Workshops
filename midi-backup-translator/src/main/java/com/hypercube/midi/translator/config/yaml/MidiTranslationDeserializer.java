package com.hypercube.midi.translator.config.yaml;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.hypercube.midi.translator.config.project.translation.MidiTranslation;
import com.hypercube.midi.translator.error.ConfigError;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

public class MidiTranslationDeserializer extends StdDeserializer<MidiTranslation> {
    private static final Pattern translationRegExp = Pattern.compile("(?<cc>[0-9]+)\\s*=>(?<payload>(\\s*([vA-F0-9]{2}))+)(\\s+\\[(?<lowerBound>[+-0123456789]+),(?<upperBound>[+-0123456789]+)\\])?");
    private static final Pattern translationPayloadRegExp = Pattern.compile("\\s*([vA-F0-9]{2})");

    public MidiTranslationDeserializer() {
        super((Class<?>) null);
    }

    @Override
    public MidiTranslation deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        String value = jsonParser.getText();
        var translationMatcher = translationRegExp.matcher(value);
        if (translationMatcher.find()) {
            int cc = Integer.parseInt(translationMatcher.group("cc"));
            int lowerBound = Optional.ofNullable(translationMatcher.group("lowerBound"))
                    .map(Integer::parseInt)
                    .orElse(0);
            int upperBound = Optional.ofNullable(translationMatcher.group("upperBound"))
                    .map(Integer::parseInt)
                    .orElse(127);
            int valueIndex = -1;
            var payloadMatcher = translationPayloadRegExp.matcher(translationMatcher.group("payload"));
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
                        throw new ConfigError("Unexpected Translation definition: '" + value + "' for value '" + byteValue + "'");
                    }
                }
            }
            byte[] payload = new byte[list.size()];
            IntStream.range(0, list.size())
                    .forEach(i -> payload[i] = list.get(i));
            return new MidiTranslation(cc, valueIndex, payload, lowerBound, upperBound);
        } else {
            throw new ConfigError("Unexpected Translation definition: " + value);
        }
    }
}
