package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io;

import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.RawData;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.KFProgram;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.program.segment.*;
import com.hypercube.workshop.midiworkshop.api.sysex.util.BitStreamReader;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

@Slf4j
public class KFProgramDeserializer extends KFDeserializer {

    public List<KFProgramSegment> deserializeProgramSegments(RawData data, BitStreamReader in) {
        List<KFProgramSegment> segments = new ArrayList<>();
        HashSet<Integer> ids = new HashSet<>();
        for (; ; ) {
            long position = data.position() + in.getBytePos();
            int segmentTag = in.readByte();
            if (segmentTag == 0) {
                break;
            }
            ProgramSegmentIdentifier id = ProgramSegmentIdentifier.fromTag(segmentTag)
                    .orElse(null);
            if (id == null) {
                log.error("Unknown segment tag %d $%X at position $%X".formatted(segmentTag, segmentTag, position));
                break;
            }
            KFProgramSegment segment = deserializeKFProgramSegment(data, in, id);
            // sanity check
            if (ids.contains(segment.getRawTag())) {
                log.error("Duplicate tags: %s %d:".formatted(segment.getType(), segment.getRawTag()));
            } else {
                ids.add(segment.getRawTag());
            }
            segments.add(segment);
        }
        var sortedSegments = segments.stream()
                .sorted(Comparator.comparingInt(KFProgramSegment::getRawTag))
                .toList();
        sortedSegments.forEach(this::dumpSegment);
        return segments;
    }

    public KFProgram deserialize(RawData data, int objectId) {
        BitStreamReader in = data.getBitStream();
        String name = readName(in);
        int segmentsStart = in.getBytePos();
        List<KFProgramSegment> segments = deserializeProgramSegments(data, in);
        return new KFProgram(data, objectId, name, segmentsStart, segments);
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
        log.info("{} => RAW TAG {} (tag id {} size {} instanceId {})",
                type,
                instStr,
                typeStr,
                type.getSize(),
                instanceId);
    }

    private KFProgramSegment deserializeKFProgramSegment(RawData data, BitStreamReader in, ProgramSegmentIdentifier id) {

        int contentSize = id.type()
                .getSize() - 1; // tag already read


        byte[] content = new byte[contentSize];
        RawData segmentContent = new RawData(content, in.getBitPos() / 8);
        for (int i = 0; i < contentSize; i++) {
            content[i] = (byte) in.readByte();
        }
        return switch (id.type()) {
            case MASTSEGTAG -> deserializeKFMasterSegment(segmentContent, id);
            case CHANSEGTAG -> deserializeKFChannelSegment(segmentContent, id);
            case ZONESEGTAG -> deserializeKFZoneSegment(segmentContent, id);
            case CLOCKSEGTAG -> deserializeKFArpSegment(segmentContent, id);
            case PGMSEGTAG -> deserializeKFProgramCommon(segmentContent, id);
            case LYRSEGTAG -> deserializeKFLayerSegment(segmentContent, id);
            case EFXSEGTAG -> deserializeKFEfxSegment(segmentContent, id);
            case ASRSEGTAG -> deserializeKFAsrSegment(segmentContent, id);
            case LFOSEGTAG -> deserializeKFLfoSegment(segmentContent, id);
            case FCNSEGTAG -> deserializeKFFcnSegment(segmentContent, id);
            case ENCSEGTAG -> deserializeKFEncSegment(segmentContent, id);
            case ENVSEGTAG -> deserializeKFEnvSegment(segmentContent, id);
            case CALSEGTAG -> deserializeKFCalvinSegment(segmentContent, id);
            case HOBSEGTAG -> deserializeKFHobbesSegment(segmentContent, id);
            default -> new KFProgramSegment(segmentContent, id);
        };
    }

    private KFMasterSegment deserializeKFMasterSegment(RawData segmentContent, ProgramSegmentIdentifier id) {
        KFMasterSegment seg = new KFMasterSegment(segmentContent, id);
        BitStreamReader in = new BitStreamReader(segmentContent.content());
        seg.setTag(seg.getType()
                .getTag());
        seg.setBitfields1(in.readByte());
        seg.setScsiID(in.readByte());
        seg.setBchan(in.readByte());
        seg.setRvmap(in.readShort());
        seg.setRpmap(in.readShort());
        seg.setIntTbl(in.readShort());
        seg.setIntKey(in.readByte());
        seg.setSysxID(in.readByte());
        seg.setTrans(in.readByte());
        seg.setDtune(in.readByte());
        seg.setSampflags(in.readByte());
        seg.setPlayflags(in.readByte());
        seg.setRfu2(in.readByte());
        seg.setSamptime(in.readByte());
        seg.setCurSetup(in.readShort());
        seg.setOldSetup(in.readShort());
        seg.setCurBank(in.readShort());
        seg.setCurEntry(in.readByte());
        seg.setFxflags(in.readByte());
        seg.setCurEffect(in.readByte());
        seg.setLocalKbdChan(in.readByte());
        seg.setFxMix(in.readByte());
        seg.setEchan(in.readByte());
        seg.setBitfields2(in.readByte());
        seg.setCurSong(in.readByte());
        seg.setTvmap(in.readShort());
        seg.setTpmap(in.readShort());
        seg.setCurDisk(in.readByte());
        seg.setContrast(in.readByte());
        seg.setView(in.readByte());
        seg.setConfirm(in.readByte());
        seg.setXflags(in.readByte());
        seg.setKbdTrans(in.readByte());
        seg.setXvmap(in.readShort());
        seg.setXpmap(in.readShort());
        seg.setDchan(in.readByte());
        seg.setBitfields3(in.readByte());
        int[] markList = new int[10];
        for (int i = 0; i < 10; i++) markList[i] = in.readByte();
        seg.setMarkList(markList);
        seg.setBitfields4(in.readByte());
        seg.setBitfields5(in.readByte());
        seg.setBitfields6(in.readByte());
        seg.setBitfields7(in.readByte());
        seg.setSeqClickProg(in.readShort());
        seg.setSeqQuantGrid(in.readShort());
        seg.setSeqQuantAmt(in.readByte());
        seg.setSeqQuantSwing(in.readByte());
        seg.setListIndex(in.readByte());
        seg.setListTop(in.readByte());
        seg.setSeqTempo(in.readShort());
        return seg;
    }

    private KFChannelSegment deserializeKFChannelSegment(RawData segmentContent, ProgramSegmentIdentifier id) {
        KFChannelSegment seg = new KFChannelSegment(segmentContent, id);
        BitStreamReader in = new BitStreamReader(segmentContent.content());
        seg.setTag(seg.getType()
                .getTag());
        seg.setChan(in.readByte());
        seg.setNlyrs(in.readByte());
        seg.setFlags(in.readByte());
        seg.setProg(in.readShort());
        seg.setVolume(in.readByte());
        seg.setPan(in.readByte());
        seg.setTrans(in.readByte());
        seg.setDtune(in.readByte());
        seg.setBrange(in.readByte());
        seg.setPlayflags(in.readByte());
        seg.setPortRate(in.readByte());
        seg.setOutflags(in.readByte());
        seg.setRfu1(in.readByte());
        seg.setRfu2(in.readByte());
        return seg;
    }

    private KFZoneSegment deserializeKFZoneSegment(RawData segmentContent, ProgramSegmentIdentifier id) {
        KFZoneSegment seg = new KFZoneSegment(segmentContent, id);
        BitStreamReader in = new BitStreamReader(segmentContent.content());
        seg.setTag(seg.getType()
                .getTag());
        seg.setChan(in.readByte());
        seg.setProg(in.readShort());
        seg.setLokey(in.readByte());
        seg.setHikey(in.readByte());
        seg.setFlags(in.readByte());
        seg.setTrans(in.readByte());
        int[] ctls = new int[8];
        for (int i = 0; i < 8; i++) ctls[i] = in.readByte();
        seg.setCtls(ctls);
        return seg;
    }

    private KFArpSegment deserializeKFArpSegment(RawData segmentContent, ProgramSegmentIdentifier id) {
        KFArpSegment seg = new KFArpSegment(segmentContent, id);
        BitStreamReader in = new BitStreamReader(segmentContent.content());
        seg.setTag(in.readByte()); // First byte of arpb is loKey, but we set it as tag if we follow the pattern
        seg.setHiKey(in.readByte());
        seg.setInitialState(in.readByte());
        seg.setLatchMode(in.readByte());
        seg.setPlayOrder(in.readByte());
        seg.setGlissando(in.readByte());
        seg.setTempoSource(in.readByte());
        seg.setOnOffControl(in.readByte());
        seg.setClocksPerBeat(in.readShort());
        seg.setDurationPerBeat(in.readShort());
        seg.setInitialTempo(in.readShort());
        seg.setVelocityMode(in.readByte());
        seg.setVelocityFixed(in.readByte());
        seg.setVelocityCtrl(in.readByte());
        seg.setNoteShift(in.readByte());
        seg.setShiftLimit(in.readByte());
        seg.setLimitOption(in.readByte());
        seg.setArpSyncFlags(in.readByte());
        seg.setRfu1(in.readByte());
        seg.setRfu2(in.readShort());
        seg.setRfu3(in.readShort());
        return seg;
    }

    private KFLayerSegment deserializeKFLayerSegment(RawData segmentContent, ProgramSegmentIdentifier id) {
        KFLayerSegment seg = new KFLayerSegment(segmentContent, id);
        BitStreamReader in = new BitStreamReader(segmentContent.content());
        seg.setTag(seg.getType()
                .getTag());
        seg.setLoEnable(in.readByte());
        seg.setTrans(in.readByte());
        seg.setTune(in.readByte());
        seg.setLoKey(in.readByte());
        seg.setHiKey(in.readByte());
        seg.setVRange(in.readByte());
        seg.setESwitch(in.readByte());
        seg.setFlags(in.readByte());
        seg.setMoreFlags(in.readByte());
        seg.setVTrig(in.readByte());
        seg.setHiEnable(in.readByte());
        seg.setDlyCtl(in.readByte());
        seg.setDlyMin(in.readByte());
        seg.setDlyMax(in.readByte());
        seg.setXfade(in.readByte());
        return seg;
    }

    private KFEfxSegment deserializeKFEfxSegment(RawData segmentContent, ProgramSegmentIdentifier id) {
        KFEfxSegment seg = new KFEfxSegment(segmentContent, id);
        BitStreamReader in = new BitStreamReader(segmentContent.content());
        seg.setTag(seg.getType()
                .getTag());
        seg.setChan(in.readByte());
        seg.setProg(in.readByte());
        seg.setMix(in.readByte());
        seg.setCtl1(in.readByte());
        seg.setOut1(in.readByte());
        seg.setCtl2(in.readByte());
        seg.setOut2(in.readByte());
        return seg;
    }

    private KFAsrSegment deserializeKFAsrSegment(RawData segmentContent, ProgramSegmentIdentifier id) {
        KFAsrSegment seg = new KFAsrSegment(segmentContent, id);
        BitStreamReader in = new BitStreamReader(segmentContent.content());
        seg.setTag(seg.getType()
                .getTag());
        seg.setRfu1(in.readByte());
        seg.setTrigger(in.readByte());
        seg.setFlags(in.readByte());
        seg.setDtime(in.readByte());
        seg.setAtime(in.readByte());
        seg.setRfu2(in.readByte());
        seg.setRtime(in.readByte());
        return seg;
    }

    private KFLfoSegment deserializeKFLfoSegment(RawData segmentContent, ProgramSegmentIdentifier id) {
        KFLfoSegment seg = new KFLfoSegment(segmentContent, id);
        BitStreamReader in = new BitStreamReader(segmentContent.content());
        seg.setTag(seg.getType()
                .getTag());
        seg.setRfu1(in.readByte());
        seg.setRateCtl(in.readByte());
        seg.setMinRate(in.readByte());
        seg.setMaxRate(in.readByte());
        seg.setPhase(in.readByte());
        seg.setShape(in.readByte());
        seg.setRfu2(in.readByte());
        return seg;
    }

    private KFFcnSegment deserializeKFFcnSegment(RawData segmentContent, ProgramSegmentIdentifier id) {
        KFFcnSegment seg = new KFFcnSegment(segmentContent, id);
        BitStreamReader in = new BitStreamReader(segmentContent.content());
        seg.setTag(seg.getType()
                .getTag());
        seg.setOp(in.readByte());
        seg.setArg1(in.readByte());
        seg.setArg2(in.readByte());
        return seg;
    }

    private KFEncSegment deserializeKFEncSegment(RawData segmentContent, ProgramSegmentIdentifier id) {
        KFEncSegment seg = new KFEncSegment(segmentContent, id);
        BitStreamReader in = new BitStreamReader(segmentContent.content());
        seg.setTag(seg.getType()
                .getTag());
        seg.setRfu1(in.readByte());
        seg.setFlags(in.readByte());
        seg.setAtTime(in.readByte());
        seg.setAtKScale(in.readByte());
        seg.setAtVScale(in.readByte());
        seg.setAtCtl(in.readByte());
        seg.setAtRange(in.readByte());
        seg.setDtTime(in.readByte());
        seg.setDtKScale(in.readByte());
        seg.setDtCtl(in.readByte());
        seg.setDtRange(in.readByte());
        seg.setRtTime(in.readByte());
        seg.setRtKScale(in.readByte());
        seg.setRtCtl(in.readByte());
        seg.setRtRange(in.readByte());
        return seg;
    }

    private KFEnvSegment deserializeKFEnvSegment(RawData segmentContent, ProgramSegmentIdentifier id) {
        KFEnvSegment seg = new KFEnvSegment(segmentContent, id);
        BitStreamReader in = new BitStreamReader(segmentContent.content());
        seg.setTag(seg.getType()
                .getTag());
        seg.setFlags(in.readByte());
        int[][] segs = new int[7][2];
        for (int i = 0; i < 7; i++) {
            segs[i][0] = in.readByte();
            segs[i][1] = in.readByte();
        }
        seg.setSegs(segs);
        return seg;
    }

    private KFCalvinSegment deserializeKFCalvinSegment(RawData segmentContent, ProgramSegmentIdentifier id) {
        KFCalvinSegment seg = new KFCalvinSegment(segmentContent, id);
        BitStreamReader in = new BitStreamReader(segmentContent.content());
        seg.setTag(seg.getType()
                .getTag());
        seg.setSubTag(in.readByte());
        seg.setTrans(in.readByte());
        seg.setDtune(in.readByte());
        seg.setTkScale(in.readByte());
        seg.setTvScale(in.readByte());
        seg.setTcontrol(in.readByte());
        seg.setTrange(in.readByte());
        seg.setSkeymap(in.readShort());
        seg.setSroot(in.readByte());
        seg.setSlegato(in.readByte());
        seg.setKeymap(in.readShort());
        seg.setRoot(in.readByte());
        seg.setLegato(in.readByte());
        seg.setTshift(in.readByte());
        seg.setRfu2(in.readByte());
        seg.setCpitch(in.readByte());
        seg.setFpitch(in.readByte());
        seg.setCkScale(in.readByte());
        seg.setCvScale(in.readByte());
        seg.setPcontrol(in.readByte());
        seg.setPrange(in.readByte());
        seg.setPdepth(in.readByte());
        seg.setPmin(in.readByte());
        seg.setPmax(in.readByte());
        seg.setPsource(in.readByte());
        seg.setCcr(in.readShort());
        seg.setBitfields(in.readByte());
        seg.setAlg(in.readByte());
        seg.setFineHz(in.readByte());
        log.info("Using keymap {} and skeyMap {}", seg.getKeymap(), seg.getSkeymap());
        return seg;
    }

    private KFHobbesSegment deserializeKFHobbesSegment(RawData segmentContent, ProgramSegmentIdentifier id) {
        KFHobbesSegment seg = new KFHobbesSegment(segmentContent, id);
        BitStreamReader in = new BitStreamReader(segmentContent.content());
        seg.setTag(seg.getType()
                .getTag());
        seg.setSubTag(in.readByte());
        seg.setCoarse(in.readByte());
        seg.setFine(in.readByte());
        seg.setKScale(in.readByte());
        seg.setVScale(in.readByte());
        seg.setControl(in.readByte());
        seg.setRange(in.readByte());
        seg.setDepth(in.readByte());
        seg.setMinDepth(in.readByte());
        seg.setMaxDepth(in.readByte());
        seg.setSource(in.readByte());
        seg.setBitfields1(in.readByte());
        seg.setMoreTscr(in.readByte());
        seg.setBitfields2(in.readByte());
        seg.setBitfields3(in.readByte());
        return seg;
    }

    private KFProgramCommon deserializeKFProgramCommon(RawData segmentContent, ProgramSegmentIdentifier id) {
        KFProgramCommon common = new KFProgramCommon(segmentContent, id);
        BitStreamReader in = new BitStreamReader(segmentContent.content());
        common.setTag(common.getType()
                .getTag());
        common.setFmt(in.readByte());
        common.setNumLayers(in.readByte());
        common.setModeFlags(in.readByte());
        common.setBendRange(in.readByte());
        common.setPortSlope(in.readByte());
        common.setMixControl(in.readByte());
        common.setMixRange(in.readByte());
        common.setCoarse1(in.readByte());
        common.setControl1(in.readByte());
        common.setRange1(in.readByte());
        common.setDest1(in.readByte());
        common.setCoarse2(in.readByte());
        common.setControl2(in.readByte());
        common.setRange2(in.readByte());
        common.setDest2(in.readByte());
        return common;
    }
}
