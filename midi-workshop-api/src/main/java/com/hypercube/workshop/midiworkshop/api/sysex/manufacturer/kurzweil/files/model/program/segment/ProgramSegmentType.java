package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment;

import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io.program.segment.*;
import lombok.Getter;

/**
 * A Program is a tree of multiple segments called "blocks".
 * <ul>
 * <li>They are identified with a "tag".</li>
 * <li>"tags" are defined in the official spec in "segtag.h"</li>
 * <li>The raw value of each tag can be decoded in a crazy way to get 2 identifiers</li>
 *     <ul>
 *         <li>A base tag id</li>
 *         <li>An index when the tag appear many times</li>
 *     </ul>
 *     This part is not properly explain in the official .H
 *     <pre>
 *        #define  calSegTag  64
 *        #define  hobSegTag  80
 *     </pre>
 * </ul>
 *
 * <p>
 * When you have "holes" like this, it means multiple calSeg can be found with tags 64,65,66,..
 * So a tag, no only gives you a segment type, but also a segment instance id !
 * <p>If you read a raw tag value 66</li>:
 * <ul>
 *     <li>64 is the base tag calSegTag</li>
 *     <li>2 is the instance id. An index to differentiate this one from the others</li>
 * </ul>
 */
@Getter
public enum ProgramSegmentType {

    /**
     * mdb.h (Master parameters).
     */
    MASTSEGTAG(0x01, 72, "mdb", KFMasterSegment.class, new KFMasterDeserializer()),

    /**
     * cdb.h (MIDI Channel parameters).
     */
    CHANSEGTAG(0x02, 16, "cdb", KFChannelSegment.class, new KFChannelDeserializer()),

    /**
     * zdb.h (Zone parameters).
     */
    ZONESEGTAG(0x03, 16, "zdb", KFZoneSegment.class, new KFZoneDeserializer()),

    /**
     * arpb.h (Arpeggiator/Clock).
     */
    ARPSEGTAG(0x04, 26, "arpb", KFArpSegment.class, new KFArpDeserializer()),

    /**
     * pgmb.h (Program Common).
     */
    PGMSEGTAG(0x08, 16, "pgmb", KFProgramCommon.class, new KFProgramCommonDeserializer()),

    /**
     * lyrb.h (Layer Common).
     */
    LYRSEGTAG(0x09, 16, "lyrb", KFLayerSegment.class, new KFLayerDeserializer()),

    /**
     * efxb.h (Effect Control without KDFX).
     */
    EFXSEGTAG(0x0F, 8, "efxb", KFEfxSegment.class, new KFEfxDeserializer()),

    /**
     * asrb.h (ASR envelopes).
     */
    ASRSEGTAG(0x10, 8, "asrb", KFAsrSegment.class, new KFAsrDeserializer()),

    /**
     * lfob.h (LFOs).
     */
    LFOSEGTAG(0x14, 8, "lfob", KFLfoSegment.class, new KFLfoDeserializer()),

    /**
     * fcnb.h (FUNs / Functions).
     */
    FCNSEGTAG(0x18, 4, "fcnb", KFFcnSegment.class, new KFFcnDeserializer()),

    /**
     * encb.h (Envelope Control).
     */
    ENCSEGTAG(0x20, 16, "encb", KFEncSegment.class, new KFEncDeserializer()),

    /**
     * envb.h (Envelope Segments).
     */
    ENVSEGTAG(0x21, 16, "envb", KFEnvSegment.class, new KFEnvDeserializer()),

    /**
     * encb.h (Envelope Control).
     */
    ATKSEGTAG(0x27, 16, "encb", KFEncSegment.class, new KFEncDeserializer()),

    /**
     * csegb.h (Calvin / Pitch / Sample).
     */
    CALSEGTAG(0x40, 32, "csegb", KFCalvinSegment.class, new KFCalvinDeserializer()),

    /**
     * hsegb.h (Hobbes / DSP / Amp).
     */
    HOBSEGTAG(0x50, 16, "hsegb", KFHobbesSegment.class, new KFHobbesDeserializer()),

    /**
     * fxrtb.h (KDFX real-time effects parameters).
     */
    FXRTSEGTAG(0x68, 8, "fxrtb", KFFXRootTableSegment.class, new KFFXRootTableDeserializer()),

    /**
     * fxrtb.h FXMOD.
     */
    FXPTSEGTAG(0x69, 8, "fxptb", KFFXPartSegment.class, new KFFXPartDeserializer()),

    /**
     * hammsegb.h (hammond organ simulation parameter segment).
     */
    HAMSEG1TAG(0x78, 32, "hammsegb", KFHammSegment1.class, new KFHamm1Deserializer()),

    /**
     * hammsegb.h (hammond organ simulation parameter segment).
     */
    HAMSEG2TAG(0x79, 32, "hammsegb", KFHammSegment2.class, new KFHamm2Deserializer()),

    /**
     * hammsegb.h (hammond organ simulation parameter segment).
     */
    HAMSEG3TAG(0x7A, 32, "hammsegb", KFHammSegment3.class, new KFHamm3Deserializer()),

    /**
     * Unknown block type.
     */
    UNKNOWN(0x98, 0, null, null, null);

    private final int tag;
    private final int size;
    private final String orgName;
    private final Class<? extends KFProgramSegment> clazz;
    private final KFSegmentDeserializer deserializer;

    ProgramSegmentType(int tag, int size, String orgName, Class<? extends KFProgramSegment> clazz, KFSegmentDeserializer deserializer) {
        this.tag = tag;
        this.size = size;
        this.orgName = orgName;
        this.clazz = clazz;
        this.deserializer = deserializer;
    }
}
