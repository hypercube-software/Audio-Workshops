package com.hypercube.mpm.midi;

import com.hypercube.workshop.midiworkshop.common.CustomMidiEvent;
import com.hypercube.workshop.midiworkshop.common.sysex.library.device.ControllerValueType;
import com.hypercube.workshop.midiworkshop.common.sysex.library.device.MidiControllerValue;
import com.hypercube.workshop.midiworkshop.common.sysex.library.device.MidiDeviceController;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.ShortMessage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
class MidiTransformerTest {
    MidiTransformer midiTransformer;

    @BeforeEach
    void init() {
        MidiTransformerListener listener = (msg -> log.info("\tTransformer notification: {}", msg));
        midiTransformer = new MidiTransformer(null, null, listener);
    }

    public static Stream<Arguments> transformCC() {
        return Stream.of(
                // 14 Bits NRPN (value and id) to 7 bits CC
                // 1 single param ID at the beginning followed by multiple NRPN values
                Arguments.of("/midi-payloads/Micron_FM Amount 0 to max.bin", 380,
                        new MidiDeviceController(ControllerValueType.NRPN_MSB_LSB, "FM Amount", "0009", 0, 1000),
                        new MidiDeviceController(ControllerValueType.CC, "Filter Freq", "4A", 0, 127),
                        1,
                        3,
                        7),
                // 14 Bits NRPN (value and id) to 7 bits CC
                // each NRPN Value goes with its NRPN param ID
                Arguments.of("/midi-payloads/Mininova_Next_Patch_14bits_NRPN.bin", 58,
                        new MidiDeviceController(ControllerValueType.NRPN_MSB_LSB, "Next Patch", "3F01", 0, 1000),
                        new MidiDeviceController(ControllerValueType.CC, "Filter Freq", "4A", 0, 127),
                        1,
                        1,
                        2),
                // example with no mapping triggered
                Arguments.of("/midi-payloads/Mininova_Next_Patch_14bits_NRPN.bin", 0,
                        new MidiDeviceController(ControllerValueType.NRPN_MSB_LSB, "Next Patch", "0000", 0, 1000),
                        new MidiDeviceController(ControllerValueType.CC, "Filter Freq", "4A", 0, 127),
                        0,
                        0,
                        0),
                // 14Bits CC to 7 bits
                Arguments.of("/midi-payloads/Summit_Filter_freq 0 to max.bin", 251,
                        new MidiDeviceController(ControllerValueType.CC_MSB_LSB, "Filter Freq", "1D3D", 0, 1000),
                        new MidiDeviceController(ControllerValueType.CC, "Filter Freq", "4A", 0, 127),
                        44,
                        65,
                        127),
                // example with NRPN 14 bits param ID and  7 bits value via MSB only
                // each NRPN Value goes with its NRPN param ID
                Arguments.of("/midi-payloads/Summit_Filter_div 0 to max.bin", 127,
                        new MidiDeviceController(ControllerValueType.NRPN_MSB, "Filter divergence", "0030", 0, 1000),
                        new MidiDeviceController(ControllerValueType.CC, "Filter Freq", "4A", 0, 127),
                        43,
                        64,
                        127),
                // example converting 14 bits CC to NRPN 14 bits
                Arguments.of("/midi-payloads/Summit_Filter_freq 0 to max.bin", 504,
                        new MidiDeviceController(ControllerValueType.CC_MSB_LSB, "Filter Freq", "1D3D", 0, 1000),
                        new MidiDeviceController(ControllerValueType.NRPN_MSB_LSB, "FM Amount", "0009", 0, 1000),
                        -1,
                        -1,
                        -1),
                // example converting 7 bits CC to NRPN 14 bits
                Arguments.of("/midi-payloads/CC7 0 to max.bin", 232,
                        new MidiDeviceController(ControllerValueType.CC, "Volume", "07", 0, 127),
                        new MidiDeviceController(ControllerValueType.NRPN_MSB_LSB, "FM Amount", "0009", 0, 1000),
                        -1,
                        -1,
                        -1),
                // example converting 7 bits CC to CC 14 bits
                Arguments.of("/midi-payloads/CC7 0 to max.bin", 230,
                        new MidiDeviceController(ControllerValueType.CC, "Volume", "07", 0, 127),
                        new MidiDeviceController(ControllerValueType.CC_MSB_LSB, "Filter Freq", "1D3D", 0, 1000),
                        -1,
                        -1,
                        -1)
        );
    }

    @ParameterizedTest
    @MethodSource
    void transformCC(String path, int expectedCount, MidiDeviceController src, MidiDeviceController dst,
                     int exppectedThirdValue, int expectedHalfValue, int expectedLastValue) throws IOException, InvalidMidiDataException {
        // GIVEN
        byte[] payload = MidiTransformerTest.class.getResourceAsStream(path)
                .readAllBytes();
        midiTransformer.addControllerMapping(new MidiControllerMapping(src, dst));

        // WHEN
        List<CustomMidiEvent> result = new ArrayList<>();
        for (int i = 0; i < payload.length; i += 3) {
            int status = payload[i] & 0xFF;
            int data1 = payload[i + 1] & 0xFF;
            int data2 = payload[i + 2] & 0xFF;
            CustomMidiEvent evt = new CustomMidiEvent(new ShortMessage(status, data1, data2));
            log.info("Transform MIDI event: {}", evt.toString());
            List<CustomMidiEvent> transformed = midiTransformer.transform(0, evt);
            log.info("\tGot {} events in return", transformed.size());
            result.addAll(transformed);

        }

        // THEN
        int x = 0;
        assertEquals(expectedCount, result.size());
        int prevCCValue = 0;
        for (int i = 0; i < result.size(); i++) {
            var evt = result.get(i);
            byte[] data = evt.getMessage()
                    .getMessage();
            int status = data[0] & 0xFF;
            int ccID = data[1] & 0xFF;
            int ccValue = data[2] & 0xFF;
            log.info("value[%d] = %s".formatted(x, evt.toString()));
            assertEquals(0xB0, status);
            if (ccID == ShortMessage.CONTROL_CHANGE) {
                assertEquals(dst.getId(), ccID);
                assertTrue(prevCCValue <= ccValue);
                assertTrue(ccValue <= 127);
                if (i == result.size() / 3) {
                    assertEquals(exppectedThirdValue, ccValue);
                } else if (i == result.size() / 2) {
                    assertEquals(expectedHalfValue, ccValue);
                } else if (i == result.size() - 1) {
                    assertEquals(expectedLastValue, ccValue);
                }
            }
            prevCCValue = ccValue;
            x++;
        }
    }

    public static Stream<Arguments> rescaleProperly() {
        return Stream.of(
                Arguments.of("1D3D | SrcName | CC_MSB_LSB | 30 | 16320", "0009 | DstName | NRPN_MSB_LSB | 10 | 1000"),
                Arguments.of("4A | SrcName | CC | 30 | 127", "0009 | DstName | NRPN_MSB_LSB | 10 | 1000"),
                Arguments.of("4A | SrcName | CC | 30 | 127", "4B | SrcName | CC | 0 | 64"),
                Arguments.of("0009 | SrcName | NRPN_MSB_LSB | 10 | 1000", "4A | DstName | CC | 30 | 127")
        );
    }

    @ParameterizedTest
    @MethodSource
    void rescaleProperly(String srcDef, String dstDef) {
        // GIVEN
        var src = MidiDeviceController.of(srcDef);
        var dst = MidiDeviceController.of(dstDef);
        int inBetweenValue = (src.getMaxValue() - (src.getMaxValue() - src.getMinValue()) / 2) + src.getMinValue();
        int expectedInBetweenValue = (dst.getMaxValue() - (dst.getMaxValue() - dst.getMinValue()) / 2) + dst.getMinValue();
        List<CustomMidiEvent> input = forgeInput(src, List.of(src.getMinValue(), inBetweenValue, src.getMaxValue()));
        midiTransformer.addControllerMapping(new MidiControllerMapping(src, dst));
        List<Integer> inputValues = getValues(src, input);
        log.info("Input {} values: {}", inputValues.size(), inputValues.stream()
                .map(Object::toString)
                .collect(Collectors.joining(",")));
        // WHEN
        List<CustomMidiEvent> result = new ArrayList<>();
        for (CustomMidiEvent evt : input) {
            log.info("Transform MIDI event: {}", evt.toString());
            List<CustomMidiEvent> transformed = midiTransformer.transform(0, evt);
            log.info("\tGot {} events in return", transformed.size());
            result.addAll(transformed);
        }

        // THEN
        List<Integer> values = getValues(dst, result);
        log.info("Got {} values: {}", values.size(), values.stream()
                .map(Object::toString)
                .collect(Collectors.joining(",")));
        assertEquals(dst.getMinValue(), values.getFirst());
        assertEquals(dst.getMaxValue(), values.getLast());
    }

    public int[] toUnsignedInts(byte[] bytes) {
        if (bytes == null) {
            return new int[0];
        }
        int[] unsignedInts = new int[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            unsignedInts[i] = bytes[i] & 0xFF;
        }
        return unsignedInts;
    }

    private List<Integer> getValues(MidiDeviceController dst, List<CustomMidiEvent> events) {
        List<Integer> values = new ArrayList<>();
        int valueMSB = 0;
        for (CustomMidiEvent evt : events) {
            int[] payload = toUnsignedInts(evt.getMessage()
                    .getMessage());
            switch (dst.getType()) {
                case CC -> {
                    values.add(payload[2]);
                }
                case CC_MSB_LSB -> {
                    if (payload[1] == dst.getMSB()) {
                        valueMSB = payload[2];

                    } else if (payload[1] == dst.getLSB()) {
                        values.add(valueMSB << 7 | payload[2]);
                    }
                }
                case NRPN_MSB -> {
                    if (payload[1] == CustomMidiEvent.NRPN_MSB_VALUE) {
                        values.add(payload[2]);
                    }
                }
                case NRPN_LSB -> {
                    if (payload[1] == CustomMidiEvent.NRPN_LSB_VALUE) {
                        values.add(payload[2]);
                    }
                }
                case NRPN_MSB_LSB -> {
                    if (payload[1] == CustomMidiEvent.NRPN_MSB_VALUE) {
                        valueMSB = payload[2];
                    } else if (payload[1] == CustomMidiEvent.NRPN_LSB_VALUE) {
                        values.add(valueMSB << 7 | payload[2]);
                    }
                }
            }
        }
        return values;
    }

    private List<CustomMidiEvent> forgeInput(MidiDeviceController ctrl, List<Integer> values) {
        List<CustomMidiEvent> result = new ArrayList<>();
        values.forEach(i -> {
            result.addAll(forgeNRPN(ctrl, i == 0, i));
        });
        return result;
    }

    private List<CustomMidiEvent> forgeNRPN(MidiDeviceController ctrl, boolean sameNRPN, int value) {
        var v = MidiControllerValue.fromValue(value);
        return switch (ctrl.getType()) {
            case CC -> MidiControllerFactory.forge7BitsCC(0, ctrl.getId(), v);
            case CC_MSB_LSB -> MidiControllerFactory.forge14BitsCC(0, ctrl.getId(), v);
            case NRPN_MSB, NRPN_LSB ->
                    MidiControllerFactory.forge7bitsNRPN(0, sameNRPN, ctrl.getId(), v, ctrl.getType());
            case NRPN_MSB_LSB -> MidiControllerFactory.forge14bitsNRPN(0, sameNRPN, ctrl.getId(), v);
        };
    }
}