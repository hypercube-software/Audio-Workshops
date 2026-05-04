package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io.program.segment;

import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment.KFMasterSegment;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment.KFProgramSegment;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamReader;

public class KFMasterDeserializer implements KFSegmentDeserializer {

    @Override
    public void deserialize(KFProgramSegment segment) {
        KFMasterSegment masterSegment = (KFMasterSegment) segment;
        BitStreamReader in = masterSegment.getSegmentContent()
                .bitStreamReader();
        masterSegment.setBitfields1(in.readByte());
        masterSegment.setScsiID(in.readByte());
        masterSegment.setBchan(in.readByte());
        masterSegment.setRvmap(in.readShort());
        masterSegment.setRpmap(in.readShort());
        masterSegment.setIntTbl(in.readShort());
        masterSegment.setIntKey(in.readByte());
        masterSegment.setSysxID(in.readByte());
        masterSegment.setTrans(in.readByte());
        masterSegment.setDtune(in.readByte());
        masterSegment.setSampflags(in.readByte());
        masterSegment.setPlayflags(in.readByte());
        masterSegment.setRfu2(in.readByte());
        masterSegment.setSamptime(in.readByte());
        masterSegment.setCurSetup(in.readShort());
        masterSegment.setOldSetup(in.readShort());
        masterSegment.setCurBank(in.readShort());
        masterSegment.setCurEntry(in.readByte());
        masterSegment.setFxflags(in.readByte());
        masterSegment.setCurEffect(in.readByte());
        masterSegment.setLocalKbdChan(in.readByte());
        masterSegment.setFxMix(in.readByte());
        masterSegment.setEchan(in.readByte());
        masterSegment.setBitfields2(in.readByte());
        masterSegment.setCurSong(in.readByte());
        masterSegment.setTvmap(in.readShort());
        masterSegment.setTpmap(in.readShort());
        masterSegment.setCurDisk(in.readByte());
        masterSegment.setContrast(in.readByte());
        masterSegment.setView(in.readByte());
        masterSegment.setConfirm(in.readByte());
        masterSegment.setXflags(in.readByte());
        masterSegment.setKbdTrans(in.readByte());
        masterSegment.setXvmap(in.readShort());
        masterSegment.setXpmap(in.readShort());
        masterSegment.setDchan(in.readByte());
        masterSegment.setBitfields3(in.readByte());
        int[] markList = new int[10];
        for (int i = 0; i < 10; i++) markList[i] = in.readByte();
        masterSegment.setMarkList(markList);
        masterSegment.setBitfields4(in.readByte());
        masterSegment.setBitfields5(in.readByte());
        masterSegment.setBitfields6(in.readByte());
        masterSegment.setBitfields7(in.readByte());
        masterSegment.setSeqClickProg(in.readShort());
        masterSegment.setSeqQuantGrid(in.readShort());
        masterSegment.setSeqQuantAmt(in.readByte());
        masterSegment.setSeqQuantSwing(in.readByte());
        masterSegment.setListIndex(in.readByte());
        masterSegment.setListTop(in.readByte());
        masterSegment.setSeqTempo(in.readShort());
    }
}
