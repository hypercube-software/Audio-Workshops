package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.masterparameter;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents a Kurzweil Master Parameter Block (mdb).
 * The structure is based on the `mdb` definition found in `mdb.h`.
 *
 * <pre><code>
 * typedef struct {
 * 	ubyte tag;
 * 	ubyte rmode_rpchg_rANO_displayLink_seqTempoLock_packed; // Combination of bitfields
 * 	ubyte scsiID;
 * 	ubyte bchan;
 * 	word rvmap;
 * 	word rpmap;
 * 	word intTbl;
 * 	ubyte intKey;
 * 	ubyte sysxID;
 * 	ubyte trans;
 * 	ubyte dtune;
 * 	ubyte sampflags_playflags_packed; // Combination of bitfields and flags
 * 	ubyte rfu2;
 * 	ubyte samptime;
 * 	word curSetup;
 * 	word oldSetup;
 * 	word curBank;
 * 	ubyte curEntry;
 * 	ubyte fxflags_outA_outB_fxSwitch_secsamp_rackMix_V_packed; // Combination of bitfields and flags
 * 	ubyte curEffect;
 * 	ubyte localKbdChan;
 * 	ubyte fxMix;
 * 	ubyte echan;
 * 	ubyte macroState_rfu3_packed; // Combination of bitfields
 * 	ubyte curSong;
 * 	word tvmap;
 * 	word tpmap;
 * 	ubyte curDisk;
 * 	ubyte contrast;
 * 	ubyte view;
 * 	ubyte confirm;
 * 	ubyte xflags_kbdTrans_packed; // Combination of bitfields and kbdTrans
 * 	word xvmap;
 * 	word xpmap;
 * 	ubyte dchan;
 * 	ubyte seqRecDub_seqRecMode_seqLoop_seqSync_packed; // Combination of bitfields
 * 	ubyte markList[10];
 * 	ubyte seqCountOff_seqClickMode_seqClickChan_packed; // Combination of bitfields
 * 	ubyte seqClock_seqClickKey_packed; // Combination of bitfields
 * 	ubyte seqKeyWait_seqClickVel_packed; // Combination of bitfields
 * 	ubyte bootDisk_defaultDisk_packed; // Combination of bitfields
 * 	word seqClickProg;
 * 	word seqQuantGrid;
 * 	ubyte seqQuantAmt;
 * 	ubyte seqQuantSwing;
 * 	ubyte listIndex;
 * 	ubyte listTop;
 * 	word seqTempo;
 * } mdb;
 * </code></pre>
 */
@Getter
@Setter
@JsonPropertyOrder({
        "tag", "rmode_rpchg_rANO_displayLink_seqTempoLock_packed", "scsiID", "bchan", "rvmap", "rpmap", "intTbl", "intKey", "sysxID", "trans", "dtune",
        "sampflags_playflags_packed", "rfu2", "samptime", "curSetup", "oldSetup", "curBank", "curEntry",
        "fxflags_outA_outB_fxSwitch_secsamp_rackMix_V_packed", "curEffect", "localKbdChan", "fxMix", "echan",
        "macroState_rfu3_packed", "curSong", "tvmap", "tpmap", "curDisk", "contrast", "view", "confirm",
        "xflags_kbdTrans_packed", "xvmap", "xpmap", "dchan", "seqRecDub_seqRecMode_seqLoop_seqSync_packed",
        "markList", "seqCountOff_seqClickMode_seqClickChan_packed", "seqClock_seqClickKey_packed",
        "seqKeyWait_seqClickVel_packed", "bootDisk_defaultDisk_packed", "seqClickProg", "seqQuantGrid",
        "seqQuantAmt", "seqQuantSwing", "listIndex", "listTop", "seqTempo"
})
public class MDB {
    private int tag;
    // Packed bitfields for rmode, rpchg, rANO, displayLink, seqTempoLock (total 1 byte)
    private int rmode_rpchg_rANO_displayLink_seqTempoLock_packed;
    /**
     * SCSI identifier
     */
    private int scsiID;
    /**
     * base channel number
     */
    private int bchan;
    /**
     * recv velocity map
     */
    private int rvmap;
    /**
     * receive pressure map
     */
    private int rpmap;
    /**
     * intonation table
     */
    private int intTbl;
    /**
     * intonation key
     */
    private int intKey;
    /**
     * system exclusive id number
     */
    private int sysxID;
    /**
     * master receive transpose semitones
     */
    private int trans;
    /**
     * master tune cents
     */
    private int dtune;
    // Packed bitfields for sampgain, sampanalog, spare, sampoptical, samprate, sampAES, intLock, mono, port, portType, smoothSw, monitor, sampstereo (total 2 bytes)
    // Combined sampflags and playflags in the C struct.
    private int sampflags_playflags_packed; // This will hold the byte value for these flags
    /**
     * reserved for future use
     */
    private int rfu2;
    /**
     * time to sample for
     */
    private int samptime;
    /**
     * actual current setup, 0 means use master zone
     */
    private int curSetup;
    /**
     * for setup mode, remember last current setup
     */
    private int oldSetup;
    /**
     * current quick access bank
     */
    private int curBank;
    /**
     * current quick access entry
     */
    private int curEntry;
    // Packed bitfields for fxMode, outA, outB, fxSwitch, secsamp, rackMix, V (total 1 byte)
    private int fxflags_outA_outB_fxSwitch_secsamp_rackMix_V_packed;
    /**
     * current effect, for use with fx override
     */
    private int curEffect;
    /**
     * Local Keyboard channel for rack
     */
    private int localKbdChan;
    /**
     * effects wet/dry mix, for use with fx override
     */
    private int fxMix;
    /**
     * effects channel #; kbdchan/1/2/..16 (used when override is off)
     */
    private int echan;
    // Packed bitfields for rfu3, macroState (total 1 byte)
    private int macroState_rfu3_packed;
    private int curSong;
    /**
     * master touch maps, velocity and pressure
     */
    private int tvmap;
    private int tpmap;
    private int curDisk;
    /**
     * contrast adjust for LCD display
     */
    private int contrast;
    /**
     * display mode; big name or list
     */
    private int view;
    /**
     * confirmations on/off
     */
    private int confirm;
    // Packed bitfields for rfumode, xmode, bmode, xbuttons, xpchg and kbdTrans (total 2 bytes)
    private int xflags_kbdTrans_packed;
    /**
     * xmit maps, velocity and pressure
     */
    private int xvmap;
    private int xpmap;
    /**
     * drum channel
     */
    private int dchan;
    // Packed bitfields for seqSync, seqLoop, seqRecMode, seqRecDub (total 1 byte)
    private int seqRecDub_seqRecMode_seqLoop_seqSync_packed;
    private int[] markList = new int[10];
    // Packed bitfields for seqClickChan, seqClickMode, seqCountOff (total 1 byte)
    private int seqCountOff_seqClickMode_seqClickChan_packed;
    // Packed bitfields for seqClock, seqClickKey (total 1 byte)
    private int seqClock_seqClickKey_packed;
    // Packed bitfields for seqKeyWait, seqClickVel (total 1 byte)
    private int seqKeyWait_seqClickVel_packed;
    // Packed bitfields for bootDisk, defaultDisk (total 1 byte)
    private int bootDisk_defaultDisk_packed;
    private int seqClickProg;
    private int seqQuantGrid;
    private int seqQuantAmt;
    private int seqQuantSwing;
    private int listIndex;
    private int listTop;
    private int seqTempo;
}
