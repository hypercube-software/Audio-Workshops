package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment;

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
public class KFLayer {
    private KFLayerSegment lyr;
    private List<KFAsrSegment> asr;
    private List<KFFcnSegment> fcn1;
    private List<KFLfoSegment> lfo;
    private List<KFFcnSegment> fcn2;
    private KFEncSegment enc;
    private KFEncSegment atk;
    private List<KFEnvSegment> env;
    private KFCalvinSegment cseg;
    private List<KFHobbesSegment> hseq;
}
