package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io.program;

import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io.KFDeserializer;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io.program.segment.KFProgramSegmentDeserializer;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.RawData;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.KFProgram;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment.KFProgramCommon;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment.KFProgramSegment;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment.ProgramSegmentType;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamReader;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KFProgramDeserializer extends KFDeserializer {

    public KFProgram deserialize(RawData data, int objectId) {
        BitStreamReader in = data.bitStreamReader();
        String name = readName(in);
        int segmentsStart = in.getBytePos();
        var program = new KFProgram(data, objectId, name, segmentsStart);
        KFProgramSegmentDeserializer d = new KFProgramSegmentDeserializer(program);
        d.deserialize("", program);
        return program;
    }

    private void dumpSegment(KFProgramSegment segment) {
        var id = segment.getId();
        int rawTag = id.rawValue();
        ProgramSegmentType type = id.type();
        int instanceId = id.instanceId();
        String instStr = "%d/$%X".formatted(rawTag, rawTag);
        String typeStr = "%d/$%X".formatted(type
                .getTag(), type
                .getTag());
        String position = Long.toHexString(segment.getSegmentContent()
                .position() - 1); // position start just after the segment tag, so we do -1
        final String format;
        if (segment instanceof KFProgramCommon common) {
            format = " format: " + common.getFmt();
        } else {
            format = "";
        }
        log.info("{} {} at 0x{} => RAW TAG {} (tag id {} size {} instanceId {}) {}",
                type.name(),
                segment.getType()
                        .getOrgName(),
                position,
                instStr,
                typeStr,
                type.getSize(),
                instanceId,
                format);
    }
}
