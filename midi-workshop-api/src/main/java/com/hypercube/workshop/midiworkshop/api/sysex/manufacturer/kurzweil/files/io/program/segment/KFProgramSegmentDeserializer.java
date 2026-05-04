package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io.program.segment;

import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.RawData;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.KFProgram;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment.KFLayer;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment.KFProgramSegment;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment.ProgramSegmentIdentifier;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamReader;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
public class KFProgramSegmentDeserializer {
    final private BitStreamReader in;
    final private KFProgram program;
    private KFProgramSegment currentSegment;

    public KFProgramSegmentDeserializer(KFProgram program) {
        this.program = program;
        this.in = program.getData()
                .bitStreamReader();
    }

    public void deserialize(String depth, Object parent) {
        try {
            log.info("{}Deserialize {}", depth, parent.getClass()
                    .getSimpleName());

            for (Field field : parent.getClass()
                    .getDeclaredFields()) {
                field.setAccessible(true);
                Class<?> fieldType = field.getType();
                if (!KFProgramSegment.class
                        .isAssignableFrom(fieldType) && !fieldType.equals(KFLayer.class) && !List.class.isAssignableFrom(fieldType)) {
                    continue;
                }
                log.info("{}Next field: {}.{} {}", depth, parent.getClass()
                        .getSimpleName(), fieldType
                        .getSimpleName(), field.getName());
                if (KFProgramSegment.class
                        .isAssignableFrom(fieldType) || List.class.isAssignableFrom(fieldType)) {
                    for (; ; ) {
                        KFProgramSegment nextSegment = readNextSegment(depth);
                        if (nextSegment == null) {
                            break;
                        }

                        Class<? extends KFProgramSegment> childSegmentClass = nextSegment.getClass();
                        if (fieldType
                                .isAssignableFrom(childSegmentClass)) {
                            log.info("{}ASSIGN current segment to {}", depth, field.getName());
                            field.set(parent, nextSegment);
                            break;
                        } else if (List.class.isAssignableFrom(fieldType)) {
                            ParameterizedType parameterizedType = (ParameterizedType) field.getGenericType();
                            Class<?> listType = (Class<?>) parameterizedType.getActualTypeArguments()[0];
                            List<KFProgramSegment> list = (List<KFProgramSegment>) field.get(parent);
                            if (list == null) {
                                list = new ArrayList<>();
                                field.set(parent, list);
                            }
                            if (listType.isAssignableFrom(childSegmentClass)) {
                                list.add(nextSegment);
                                log.info("{}ADD current segment to list {}", depth, field.getName());
                            } else {
                                currentSegment = nextSegment;
                                break;
                            }
                        } else {
                            currentSegment = nextSegment;
                            field.set(parent, null);
                            break;
                        }
                    }
                } else {
                    var instance = fieldType
                            .getDeclaredConstructor()
                            .newInstance();
                    field.set(parent, instance);
                    deserialize(depth, instance);
                }
            }
        } catch (IllegalAccessException | InvocationTargetException | InstantiationException |
                 NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public KFProgramSegment deserializeSegment(String depth, RawData data, BitStreamReader in) {
        long segmentPos = data.position() + in.getBytePos();
        int segmentTag = in.readByte();
        if (segmentTag == 0) {
            return null;
        }
        ProgramSegmentIdentifier id = ProgramSegmentIdentifier.fromTag(segmentTag)
                .orElse(null);
        if (id == null) {
            log.error("{}Unknown segment tag {} ${} at position ${}", depth, segmentTag, Integer.toHexString(segmentTag), Long.toHexString(segmentPos));
            return null;
        }
        RawData segmentContent = data.readChildBlock(id.type()
                .getSize() - 1);
        log.info("{}Read segment tag {} ${} of size {} bytes at position ${}",
                depth,
                id.type(),
                Integer.toHexString(id.rawValue()),
                segmentContent.size(),
                Long.toHexString(segmentPos));
        try {
            var c = id.type()
                    .getClazz()
                    .getDeclaredConstructor(RawData.class, ProgramSegmentIdentifier.class);
            var instance = c.newInstance(segmentContent, id);
            Optional.ofNullable(id.type()
                            .getDeserializer())
                    .ifPresent(d ->
                            d.deserialize(instance));
            return instance;
        } catch (InstantiationException | InvocationTargetException | IllegalAccessException |
                 NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private KFProgramSegment readNextSegment(String depth) {
        final KFProgramSegment result;
        if (currentSegment != null) {
            result = currentSegment;
            currentSegment = null;
        } else {
            result = deserializeSegment(depth, program.getData(), in);
            if (result != null) {
                deserialize(depth + "\t", result);
            }
        }
        return result;
    }
}
