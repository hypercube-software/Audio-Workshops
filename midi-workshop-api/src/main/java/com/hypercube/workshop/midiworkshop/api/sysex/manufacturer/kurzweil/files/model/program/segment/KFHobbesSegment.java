package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.RawData;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonPropertyOrder({"id", "segmentContent", "subTag", "coarse", "fine", "kScale", "vScale", "control", "range", "depth",
        "minDepth", "maxDepth", "source", "bitfields1", "moreTscr", "bitfields2", "bitfields3"})
public class KFHobbesSegment extends KFProgramSegment {
    private int subTag;
    private int coarse;
    private int fine;
    private int kScale;
    private int vScale;
    private int control;
    private int range;
    private int depth;
    private int minDepth;
    private int maxDepth;
    private int source;
    private int bitfields1; // downShift, filtAlg
    private int moreTscr;
    private int bitfields2; // headRoom, pair, rfu2
    private int bitfields3; // rfu, panMode, pan

    public KFHobbesSegment(RawData segmentContent, ProgramSegmentIdentifier id) {
        super(segmentContent, id);
    }
}
