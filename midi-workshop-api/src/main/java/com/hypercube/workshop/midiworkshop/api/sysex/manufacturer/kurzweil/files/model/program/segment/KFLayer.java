package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/*
typedef struct {
    lyrb lyr;
    asrb asr[2];
    fcnb fcn1[2];
    lfob lfo[2];
    fcnb fcn2[2];
    encb enc;
    encb atk;     // !! new to 2500, impact params
    envb env[3];
    csegb cseg;
    hsegb hseg[4];
} layerb;
*/
@Getter
@Setter
@NoArgsConstructor
@JsonPropertyOrder({"lyr", "asr", "fcn1", "lfo", "fcn2", "enc", "atk", "env", "cseg", "hseq"})
public class KFLayer {
    private KFLayerSegment lyr;
    private List<KFAsrSegment> asr;
    private List<KFFcnSegment> fcn1;
    private List<KFLfoSegment> lfo;
    private List<KFFcnSegment> fcn2;
    private KFEncSegment enc;
    private KFEncSegment atk;
    private List<KFEnvSegment> env;
    private KFCalvinSegment cseg; // provide pointer to the keymap
    private List<KFHobbesSegment> hseq;
}
