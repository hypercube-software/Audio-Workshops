package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.RawData;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonPropertyOrder({"id", "segmentContent", "rfu1", "flags", "atTime", "atKScale", "atVScale", "atCtl", "atRange", "dtTime", "dtKScale", "dtCtl", "dtRange", "rtTime", "rtKScale", "rtCtl", "rtRange"})
public class KFEncSegment extends KFProgramSegment {
    private int rfu1;
    private int flags; // natural
    private int atTime;
    private int atKScale;
    private int atVScale;
    private int atCtl;
    private int atRange;
    private int dtTime;
    private int dtKScale;
    private int dtCtl;
    private int dtRange;
    private int rtTime;
    private int rtKScale;
    private int rtCtl;
    private int rtRange;

    public KFEncSegment(RawData segmentContent, ProgramSegmentIdentifier id) {
        super(segmentContent, id);
    }
}
