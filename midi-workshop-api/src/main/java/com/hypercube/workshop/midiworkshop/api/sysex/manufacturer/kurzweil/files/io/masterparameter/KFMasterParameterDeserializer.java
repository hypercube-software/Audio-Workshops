package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io.masterparameter;

import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io.KFDeserializer;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.RawData;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.masterparameter.CDB;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.masterparameter.KFMasterParameter;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.masterparameter.MDB;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.masterparameter.ZDB;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamReader;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamWriter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class KFMasterParameterDeserializer extends KFDeserializer {

    public KFMasterParameter deserialize(RawData data, int objectId, String name) {
        BitStreamReader in = data.bitStreamReader();
        if (name == null) {
            name = readName(in);
        }

        KFMasterParameter masterParameter = new KFMasterParameter(data, name, objectId);

        // Deserialize MDB
        MDB mdb = deserializeMDB(in);
        masterParameter.setM(mdb);

        // Deserialize ZDB
        ZDB zdb = deserializeZDB(in);
        masterParameter.setZ(zdb);

        // Deserialize CDB array (16 channels)
        CDB[] cdbs = new CDB[16];
        for (int i = 0; i < 16; i++) {
            cdbs[i] = deserializeCDB(in);
        }
        masterParameter.setC(cdbs);

        // Deserialize sb (search buffers, 10x18 ubytes = 180 bytes)
        int[][] sb = new int[10][18];
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 18; j++) {
                sb[i][j] = in.readByte() & 0xFF;
            }
        }
        masterParameter.setSb(sb);

        try {
            Files.write(Path.of("./target/master_parameter.dat"), data.getContent());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return masterParameter;
    }

    public byte[] serialize(KFMasterParameter masterParameter) {
        BitStreamWriter out = new BitStreamWriter();
        writeName(masterParameter.getName(), out);

        // Serialize MDB
        serializeMDB(out, masterParameter.getM());

        // Serialize ZDB
        serializeZDB(out, masterParameter.getZ());

        // Serialize CDB array (16 channels)
        for (CDB cdb : masterParameter.getC()) {
            serializeCDB(out, cdb);
        }

        // Serialize sb (search buffers, 10x18 ubytes = 180 bytes)
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 18; j++) {
                out.writeByte((byte) masterParameter.getSb()[i][j]);
            }
        }

        return out.toByteArray();
    }

    // Helper methods for deserializing nested structs

    private MDB deserializeMDB(BitStreamReader in) {
        MDB mdb = new MDB();
        mdb.setTag(in.readByte() & 0xFF);
        mdb.setRmode_rpchg_rANO_displayLink_seqTempoLock_packed(in.readByte() & 0xFF);
        mdb.setScsiID(in.readByte() & 0xFF);
        mdb.setBchan(in.readByte() & 0xFF);
        mdb.setRvmap(in.readShort() & 0xFFFF);
        mdb.setRpmap(in.readShort() & 0xFFFF);
        mdb.setIntTbl(in.readShort() & 0xFFFF);
        mdb.setIntKey(in.readByte() & 0xFF);
        mdb.setSysxID(in.readByte() & 0xFF);
        mdb.setTrans(in.readByte() & 0xFF);
        mdb.setDtune(in.readByte() & 0xFF);
        mdb.setSampflags_playflags_packed(in.readByte() & 0xFF); // Assuming this is 1 byte for now. C struct combines two bytes (sampflags and playflags)
        mdb.setRfu2(in.readByte() & 0xFF);
        mdb.setSamptime(in.readByte() & 0xFF);
        mdb.setCurSetup(in.readShort() & 0xFFFF);
        mdb.setOldSetup(in.readShort() & 0xFFFF);
        mdb.setCurBank(in.readShort() & 0xFFFF);
        mdb.setCurEntry(in.readByte() & 0xFF);
        mdb.setFxflags_outA_outB_fxSwitch_secsamp_rackMix_V_packed(in.readByte() & 0xFF);
        mdb.setCurEffect(in.readByte() & 0xFF);
        mdb.setLocalKbdChan(in.readByte() & 0xFF);
        mdb.setFxMix(in.readByte() & 0xFF);
        mdb.setEchan(in.readByte() & 0xFF);
        mdb.setMacroState_rfu3_packed(in.readByte() & 0xFF);
        mdb.setCurSong(in.readByte() & 0xFF);
        mdb.setTvmap(in.readShort() & 0xFFFF);
        mdb.setTpmap(in.readShort() & 0xFFFF);
        mdb.setCurDisk(in.readByte() & 0xFF);
        mdb.setContrast(in.readByte() & 0xFF);
        mdb.setView(in.readByte() & 0xFF);
        mdb.setConfirm(in.readByte() & 0xFF);
        mdb.setXflags_kbdTrans_packed(in.readByte() & 0xFF); // Assuming this is 1 byte from C struct interpretation
        mdb.setXvmap(in.readShort() & 0xFFFF);
        mdb.setXpmap(in.readShort() & 0xFFFF);
        mdb.setDchan(in.readByte() & 0xFF);
        mdb.setSeqRecDub_seqRecMode_seqLoop_seqSync_packed(in.readByte() & 0xFF);
        int[] markList = new int[10];
        for (int i = 0; i < 10; i++) {
            markList[i] = in.readByte() & 0xFF;
        }
        mdb.setMarkList(markList);
        mdb.setSeqCountOff_seqClickMode_seqClickChan_packed(in.readByte() & 0xFF);
        mdb.setSeqClock_seqClickKey_packed(in.readByte() & 0xFF);
        mdb.setSeqKeyWait_seqClickVel_packed(in.readByte() & 0xFF);
        mdb.setBootDisk_defaultDisk_packed(in.readByte() & 0xFF);
        mdb.setSeqClickProg(in.readShort() & 0xFFFF);
        mdb.setSeqQuantGrid(in.readShort() & 0xFFFF);
        mdb.setSeqQuantAmt(in.readByte() & 0xFF);
        mdb.setSeqQuantSwing(in.readByte() & 0xFF);
        mdb.setListIndex(in.readByte() & 0xFF);
        mdb.setListTop(in.readByte() & 0xFF);
        mdb.setSeqTempo(in.readShort() & 0xFFFF);
        return mdb;
    }

    private void serializeMDB(BitStreamWriter out, MDB mdb) {
        out.writeByte((byte) mdb.getTag());
        out.writeByte((byte) mdb.getRmode_rpchg_rANO_displayLink_seqTempoLock_packed());
        out.writeByte((byte) mdb.getScsiID());
        out.writeByte((byte) mdb.getBchan());
        out.writeShort((short) mdb.getRvmap());
        out.writeShort((short) mdb.getRpmap());
        out.writeShort((short) mdb.getIntTbl());
        out.writeByte((byte) mdb.getIntKey());
        out.writeByte((byte) mdb.getSysxID());
        out.writeByte((byte) mdb.getTrans());
        out.writeByte((byte) mdb.getDtune());
        out.writeByte((byte) mdb.getSampflags_playflags_packed());
        out.writeByte((byte) mdb.getRfu2());
        out.writeByte((byte) mdb.getSamptime());
        out.writeShort((short) mdb.getCurSetup());
        out.writeShort((short) mdb.getOldSetup());
        out.writeShort((short) mdb.getCurBank());
        out.writeByte((byte) mdb.getCurEntry());
        out.writeByte((byte) mdb.getFxflags_outA_outB_fxSwitch_secsamp_rackMix_V_packed());
        out.writeByte((byte) mdb.getCurEffect());
        out.writeByte((byte) mdb.getLocalKbdChan());
        out.writeByte((byte) mdb.getFxMix());
        out.writeByte((byte) mdb.getEchan());
        out.writeByte((byte) mdb.getMacroState_rfu3_packed());
        out.writeByte((byte) mdb.getCurSong());
        out.writeShort((short) mdb.getTvmap());
        out.writeShort((short) mdb.getTpmap());
        out.writeByte((byte) mdb.getCurDisk());
        out.writeByte((byte) mdb.getContrast());
        out.writeByte((byte) mdb.getView());
        out.writeByte((byte) mdb.getConfirm());
        out.writeByte((byte) mdb.getXflags_kbdTrans_packed());
        out.writeShort((short) mdb.getXvmap());
        out.writeShort((short) mdb.getXpmap());
        out.writeByte((byte) mdb.getDchan());
        out.writeByte((byte) mdb.getSeqRecDub_seqRecMode_seqLoop_seqSync_packed());
        for (int mark : mdb.getMarkList()) {
            out.writeByte((byte) mark);
        }
        out.writeByte((byte) mdb.getSeqCountOff_seqClickMode_seqClickChan_packed());
        out.writeByte((byte) mdb.getSeqClock_seqClickKey_packed());
        out.writeByte((byte) mdb.getSeqKeyWait_seqClickVel_packed());
        out.writeByte((byte) mdb.getBootDisk_defaultDisk_packed());
        out.writeShort((short) mdb.getSeqClickProg());
        out.writeShort((short) mdb.getSeqQuantGrid());
        out.writeByte((byte) mdb.getSeqQuantAmt());
        out.writeByte((byte) mdb.getSeqQuantSwing());
        out.writeByte((byte) mdb.getListIndex());
        out.writeByte((byte) mdb.getListTop());
        out.writeShort((short) mdb.getSeqTempo());
    }

    private ZDB deserializeZDB(BitStreamReader in) {
        ZDB zdb = new ZDB();
        zdb.setTag(in.readByte() & 0xFF);
        zdb.setChan(in.readByte() & 0xFF);
        zdb.setProg(in.readShort() & 0xFFFF);
        zdb.setLokey(in.readByte() & 0xFF);
        zdb.setHikey(in.readByte() & 0xFF);
        zdb.setFlags(in.readByte() & 0xFF);
        zdb.setTrans(in.readByte() & 0xFF);
        int[] ctls = new int[8];
        for (int i = 0; i < 8; i++) {
            ctls[i] = in.readByte() & 0xFF;
        }
        zdb.setCtls(ctls);
        return zdb;
    }

    private void serializeZDB(BitStreamWriter out, ZDB zdb) {
        out.writeByte((byte) zdb.getTag());
        out.writeByte((byte) zdb.getChan());
        out.writeShort((short) zdb.getProg());
        out.writeByte((byte) zdb.getLokey());
        out.writeByte((byte) zdb.getHikey());
        out.writeByte((byte) zdb.getFlags());
        out.writeByte((byte) zdb.getTrans());
        for (int ctl : zdb.getCtls()) {
            out.writeByte((byte) ctl);
        }
    }

    private CDB deserializeCDB(BitStreamReader in) {
        CDB cdb = new CDB();
        cdb.setTag(in.readByte() & 0xFF);
        cdb.setChan(in.readByte() & 0xFF);
        cdb.setNlyrs(in.readByte() & 0xFF);
        cdb.setFlags(in.readByte() & 0xFF);
        cdb.setProg(in.readShort() & 0xFFFF);
        cdb.setVolume(in.readByte() & 0xFF);
        cdb.setPan(in.readByte() & 0xFF);
        cdb.setTrans(in.readByte() & 0xFF);
        cdb.setDtune(in.readByte() & 0xFF);
        cdb.setBrange(in.readByte() & 0xFF);
        cdb.setPlayflags(in.readByte() & 0xFF);
        cdb.setPortRate(in.readByte() & 0xFF);
        cdb.setOutflags(in.readByte() & 0xFF);
        int[] rfu = new int[2];
        for (int i = 0; i < 2; i++) {
            rfu[i] = in.readByte() & 0xFF;
        }
        cdb.setRfu(rfu);
        return cdb;
    }

    private void serializeCDB(BitStreamWriter out, CDB cdb) {
        out.writeByte((byte) cdb.getTag());
        out.writeByte((byte) cdb.getChan());
        out.writeByte((byte) cdb.getNlyrs());
        out.writeByte((byte) cdb.getFlags());
        out.writeShort((short) cdb.getProg());
        out.writeByte((byte) cdb.getVolume());
        out.writeByte((byte) cdb.getPan());
        out.writeByte((byte) cdb.getTrans());
        out.writeByte((byte) cdb.getDtune());
        out.writeByte((byte) cdb.getBrange());
        out.writeByte((byte) cdb.getPlayflags());
        out.writeByte((byte) cdb.getPortRate());
        out.writeByte((byte) cdb.getOutflags());
        for (int rfuByte : cdb.getRfu()) {
            out.writeByte((byte) rfuByte);
        }
    }
}
