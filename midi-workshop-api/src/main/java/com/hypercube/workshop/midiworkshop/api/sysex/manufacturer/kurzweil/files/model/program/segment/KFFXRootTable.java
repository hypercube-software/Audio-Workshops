package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment;

import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.RawData;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class KFFXRootTable extends KFProgramSegment {
    private int vers;
    private int studio;
    private int[] rfu;
    private List<KFFXPartSegment> patch;/* 18 FMOD */
    private KFAsrSegment asr1, asr2;
    private KFFcnSegment fcn1, fcn2;
    private KFLfoSegment lfo1, lfo2;
    private KFFcnSegment fcn3, fcn4;

    public KFFXRootTable(RawData segmentContent, ProgramSegmentIdentifier id) {
        super(segmentContent, id);
    }
}
