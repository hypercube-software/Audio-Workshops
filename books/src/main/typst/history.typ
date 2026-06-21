#import "settings.typ": *
#part("Archeology") 

#chapter("History", image: image("./assets/hd3-2481-500.svg"))
#index("Sectioning")
#codly(number-format: none)

== The official Kurzweil FTP

=== The bad news

This FTP server is now closed to anonymous access. Nevertheless its content is "hidden" in the official HTTP site. On old links, you may find various host names like `younchang.com`, `kurzweilmusicsystem.com` or `kurzweil.com`.

You may ask, "how can I browse the content ?" Well ... you can't ! Fortunately you can use *WayBack Machine* to the rescue:

```
https://web.archive.org/web/20210312202644/http://kurzweil.com/content/migration/downloads/pub/Kurzweil/Pro_Products/K2600/
```
Old links like:

```
http://www.youngchang.com/pub/Kurzweil/Pro_Products/K2000-K2vx-K2500/Samples/ElectricPianos
```
Need to be changed in :
```
http://kurzweil.com/content/migration/downloads/pub/Kurzweil/Pro_Products/K2000-K2vx-K2500/Samples/ElectricPianos/
```
=== Offical specification

The most important .H files on earth are still here:

```
http://kurzweil.com/content/migration/downloads/pub/Kurzweil/Pro_Products/K2000-K2vx-K2500/Information/K2KSYSEX.ZIP
http://kurzweil.com/content/migration/downloads/pub/Kurzweil/Pro_Products/K2000-K2vx-K2500/Information/K25SYSEX.ZIP
```
They are released by Kurzweil but *not officially supported by them*.

= The K2000 Programmer corner

== About
The Sysex Project was a joint effort of K2000 owners to develop a better understanding of their instruments. The owners pool what they have learned about K2000 sysex commands into a central location, which can be freely accessed by all. This eliminates the need for individuals to uncover all of the K2000 sysex commands independently and creates an information database that can be used for deeper explorations.

This site contains many insights about the format. Chris Martin (aka. Frank, from university of Central Florida) was a major contributor, providing a kind of "K2000 Sysex Manual"

#v(20pt)

#table(
  columns: 2,
  align: left + horizon,  
[Status],
[Dead],
[URL],
[#text(fill: rgb("#bbbebf"))[#underline[https://web.archive.org/web/20191229022006/http://k2000.creativebits.net/sysex-project/]]],
)

= Martin’s K2000 Sysex Manual

== KEYMAP DATA

```
Keymaps are an integral part of every layer of a program.  Each
keymap contains a set of parameters determining which sample(s)
the k2000 will play when you trigger a note.... Each layer has at
least one keymap, but it can have two keymaps when you're working
with stereo samples.... Each keymap consists of a set of key
ranges. The entire span of each keymap is 128 keys, from C0 to
G9.  Each range has a sample root assigned within the range.

Within each key range, the sample root can be transposed up and
down to play on each of the range's keys. You may assign as many
key ranges to a keymap as you like, even creating a key range for
each key. You may also create multi-velocity keymaps, which will
play different samples depending on how hard you strike the keys.
(Excerpts From K2000 User Manual)


The Keymap Parameters are as follows:
        
        KEY RANGE               Variable
        LOW KEY                 C0 to G9
        HIGH KEY                C0 to G9
        SAMPLE                  Sample Root List
        COARSE TUNE             -120 to 60 semitones
        FINE TUNE               -49 to 50 cents
        VOLUME ADJUST           +/- 48 db
        VELOCITY CROSSOVER      none, ppp to fff

BANKS- Keymaps may be stored in:
         ROM:     1-99
                100-199
         RAM:   200-299
                300-399
                   .
                   .
                900-999


Sysex Data for a Single Velocity KeyMap : 0-795
Sysex Data for a Multi- Velocity KeyMap : 0-1563
Data Starts Here---
LineNo. 
----------------------------------------
   0:   Hex constant (00h)  
   1:   Hex constant of (00h) only if there are multiple samples assigned
        within different ranges. Else, if there is only one sample, or 
        the same sample (same number in ROM *-may be a different timbre) 
        across multiple ranges, that sample number is placed here.
   2:   Hex constant (00h) 
   3:   Hex constant (17h) 
   4:   Hex constant (00h) 
   5:   Hex constant (00h) 
   6:   Hex constant (00h) 
   7:   Hex constant (64h) 
   8:   Hex constant (00h) 
   9:   Hex constant (7Fh) 
  10:   Hex constant (00h) 
  11:   Hex constant (06h) 
  12:   Hex constant (00h) 

  13:   Hex constant (10h)    \
  14:   (00h or 03h -see right)|  
  15:   Hex constant (0Eh)     |   A little hard to explain, but I'll do
  16:   (00h or 03h -see right)|   my best.... This section corresponds to
  17:   Hex constant (0Ch)     |   a Multi-Velocity KeyMap. If it is not a 
  18:   (00h or 03h -see right)|   Multi-Vel Keymap, each Line number (14,
  19:   Hex constant (0Ah)     |   16,18,20,22,26) would be 00h. If it is
  20:   (00h or 03h -see right) \  a Multi-Vel Keymap, the 'bitmaps' are
  21:   Hex constant (08h)      /  as follows according to its Vel 
  22:   (00h or 03h -see right)|   Crossover value (03h - on / 00h - off):
  23:   Hex constant (06h)     |   
  24:   (00h or 03h -see right)|         (ppp) 14: 03h  (pp) 14: 00h
  25:   Hex constant (04h)     |               16: 03h       16: 03h
  26:   (00h or 03h -see right)|               18: 03h       18: 03h
  27:   Hex constant (02h)     /               20: 03h       20: 03h
                                               22: 03h       22: 03h
                                               24: 03h       24: 03h
                                               26: 03h       26: 03h

  (p) 14: 00h  (mp) 14: 00h  (mf) 14: 00h  (f) 14: 00h  (ff) 14: 00h     
      16: 00h       16: 00h       16: 00h      16: 00h       16: 00h
      18: 03h       18: 00h       18: 00h      18: 00h       18: 00h
      20: 03h       20: 03h       20: 00h      20: 00h       20: 00h 
      22: 03h       22: 03h       22: 03h      22: 00h       22: 00h 
      24: 03h       24: 03h       24: 03h      24: 03h       24: 00h 
      26: 03h       26: 03h       26: 03h      26: 03h       26: 03h


  ----==RANGE VALUES==----
  Each set of 6 line numbers starting at Line# 28, correspond to a 
  key on the keyboard. Line #'s 28-33 are a sets of values that 
  correspond to C0.  Line #'s 34-39 correspond to the C#0 key. 
  And so on. Each set of six, starting from 28 and the last ending at 
  795, match up to each key starting at C0 all the way up to G9 (128 keys).

  *If the keymap is Multi-Velocity, Line #'s 28-795 correspond to the
   Lower Velocity Range Values, while the Higher Velocity's Range Values
   start at 796 and end at line number 1563... Six sets of values
   corresponding to the 128 keys as in the explanation above.. It's 
   just the additional layer that is added for a Multi-Velocity Keymap.


                TOTAL TUNE VALUE:= ((Coarse tune * 100) + Fine tune)
                *  *note* if TOTAL TUNE VALUE is Negative:  *
 /              NEW TOTAL TUNE VALUE:= (256 + NEGATIVE TOTAL TUNE VALUE)
| 28:   High Value =  TOTAL TUNE VALUE (mod) 256
| 29:   Lo Value =       - the remainder -
|
|               VOLUME VALUE:= (Volume / .5)
|               *  *note* if VOLUME VALUE is Negative:  *
|               NEW VOLUME VALUE:= (256 + NEGATIVE VOLUME VALUE)
| 30:   VOLUME VALUE 
< 31:   Hex constant (00h) 
| 32:   SAMPLE NUMBER          (example: Grand Piano G#1 would be 
| 33:   SAMPLE TIMBRE NUMBER    Sample #: 1   Sample Timbre #: 0
|                               but Grand Piano B1, which is the same 
|                               sample number in ROM, but the next timbre
|                               would be:
|                               Sample #: 1   Sample Timbre #: 1
|                               Get it?!? Good! :)
 \

  34-795: Lower Velocity Values, or the final values for a 
          Single Velocity Keymap

  796-1563: The Higher Velocity Values for a Multi-Velocity Keymap
            (not included for a Single Velocity Keymap)
```
== Effects

```
The effects processor in the K2000 allows you to create your own
effects, selecting from 27 different configurations of effects
types (list below).  These configurations are simply different
sets of familiar programmable effects generators. And each
configuration has a different set of parameters (up to 21).

BANKS- Effects may be stored in:
       ROM: 1-37
       RAM: 100-109
            200-209
               .
               .
            900-909


Sysex Data for a Effects Data : 0-43
Data Starts Here---
LineNo. 
----------------------------------------
   0:   Hex constant (00h)  
   1:   CONFIGURATION NUMBER
   2:   Hex constant (00h)  
   3:   PARAMETER VALUE # 1
   4:   Hex constant (00h)  
   5:   PARAMETER VALUE # 2
   6:   Hex constant (00h)  
   7:   PARAMETER VALUE # 3
   8:   Hex constant (00h)  
   9:   PARAMETER VALUE # 4
  10:   Hex constant (00h)  
  11:   PARAMETER VALUE # 5
  12:   Hex constant (00h)  
  13:   PARAMETER VALUE # 6
  14:   Hex constant (00h)  
  15:   PARAMETER VALUE # 7
  16:   Hex constant (00h)  
  17:   PARAMETER VALUE # 8
  18:   Hex constant (00h)  
  19:   PARAMETER VALUE # 9
  20:   Hex constant (00h)  
  21:   PARAMETER VALUE # 10
  22:   Hex constant (00h)  
  23:   PARAMETER VALUE # 11
  24:   Hex constant (00h)  
  25:   PARAMETER VALUE # 12
  26:   Hex constant (00h)  
  27:   PARAMETER VALUE # 13
  28:   Hex constant (00h)  
  29:   PARAMETER VALUE # 14
  30:   Hex constant (00h)  
  31:   PARAMETER VALUE # 15
  32:   Hex constant (00h)  
  33:   PARAMETER VALUE # 16
  34:   Hex constant (00h)  
  35:   PARAMETER VALUE # 17
  36:   Hex constant (00h)  
  37:   PARAMETER VALUE # 18
  38:   Hex constant (00h)  
  39:   PARAMETER VALUE # 19
  40:   Hex constant (00h)  
  41:   PARAMETER VALUE # 20
  42:   Hex constant (00h)  
  43:   PARAMETER VALUE # 21


CONFIGURATIONS:
#Value  Name
-----------------
1       Dry
2       Stereo Chorus
3       Stereo Flange
4       Stereo Delay
5       4-tap Delay
6       Ultimate Reverb
7       Room Simulator
8       Gated Reverb
9       Reverse Reverb
10      Parametric EQ
11      Graphic EQ
12      Parametric EQ+Delay+Mixer
13      Parametric EQ+Chorus+Mixer
14      Chorus+Room+Mixer
15      Delay+Room+Mixer
16      Chorus+Room+Mixer
17      Delay+Hall+Mixer
18      EQ+Gated Reverb+Mixer
19      EQ+Reverse Reverb+Mixer
20      Parametric EQ+Chorus+Delay+Mixer
21      Parametric EQ+Flange+Delay+Mixer
22      Chorus+Delay+Room+Mixer
23      Flange+Delay+Room+Mixer
24      Chorus+Delay+Hall+Mixer
25      Flange+Delay+Hall+Mixer
26      EQ+Chorus+4-tap Delay+Mixer
27      EQ+Flange+4-tap Delay+Mixer


-Parameters and their Value Ranges
============================================

CONFIG# 1 - Dry
Parameters#: None (parameter values in Sysex are ignored)


CONFIG# 2 - Stereo Chorus
#  Parameters         Sysex Values 
1 - Chorus Delay        0-60                    
2 - LFO Speed           0-65
3 - LFO Depth           0-99
4 - DRY Level           0-10
5 - RIGHT Level         0-10
6 - LEFT Level          0-10
(additional parameter values in Sysex are ignored)


CONFIG# 3 - Stereo Flange
#  Parameters         Sysex Values 
1 - Flange Delay        0-10                    
2 - LFO Speed           0-65
3 - LFO Depth           0-99
4 - Feedback            0-10  (real value: 0-99 by 10's)
5 - DRY Level           0-10
6 - RIGHT Level         0-10
7 - LEFT Level          0-10
(additional parameter values in Sysex are ignored)


CONFIG# 4 - Stereo Delay
#  Parameters         Sysex Values 
1 - Delay Time          0-147  (real value: 0-40 by 1's, 40-400 by 5's,                    
                                and 400-750 by 10's)
2 - Feedback            0-10  (real value: 0-99 by 10's)
3 - DRY Level           0-10
4 - RIGHT Level         0-10
5 - LEFT Level          0-10
(additional parameter values in Sysex are ignored)


CONFIG# 5 - 4-tap Delay
#  Parameters         Sysex Values 
1 - Tap 1               0-222  (real value: 0-40 by 1's, 40-400 by 5's,                    
                                and 400-1500 by 10's)
2 - Tap 2               0-222  (real value: 0-40 by 1's, 40-400 by 5's,                    
                                and 400-1500 by 10's)
3 - Tap 3               0-222  (real value: 0-40 by 1's, 40-400 by 5's,                    
                                and 400-1500 by 10's)
4 - Tap 4               0-222  (real value: 0-40 by 1's, 40-400 by 5's,                    
                                and 400-1500 by 10's)
5 - Feedback Delay      0-222  (real value: 0-40 by 1's, 40-400 by 5's,                    
                                and 400-1500 by 10's)
6 - Feedback            0-10  (real value: 0-99 by 10's)
7 - DRY Level           0-10
8 - Tap1 Level R        0-10
9 - Tap1 Level L        0-10
10- Tap2 Level R        0-10
11- Tap2 Level L        0-10
12- Tap3 Level R        0-10
13- Tap3 Level L        0-10
14- Tap4 Level R        0-10
15- Tap4 Level L        0-10
(additional parameter values in Sysex are ignored)


CONFIG# 6 - Ultimate Reverb
#  Parameters         Sysex Values 
1 - Dry Level           0-10
2 - Early Level         0-10
3 - Later Level         0-10
4 - Decay Time (1)      0-30  (real value: see Effects Parameter Appendix)
5 - Room Volume         0-9   (real value: 0.0-0.9 by .1's)
6 - High-Freq Damping   0-9   
7 - Envelopment         0-9
8 - Later Delay         0-70
9 - Later Diffusion     0-9
10- Early Delay         0-70
11- Early Diffusion     0-9
(additional parameter values in Sysex are ignored)


CONFIG# 7 - Room Simulator
#  Parameters         Sysex Values 
1 - Gross Size          0-4  (real value: 0-Studio 1-Chamber 2-Club                    
                                          3-Hall 4-Arena)
2 - Decay Time (2)      0-23 (real value: see Effects Parameter Appendix)
3 - Listening Position  0-2  (real value: 0-front 1-middle 2-back)
4 - Hi-Freq Damping     0-9
5 - DRY Level           0-10
6 - REVERB Level        0-10
(additional parameter values in Sysex are ignored)


CONFIG# 8 - Gated Reverb  
#  Parameters         Sysex Values 
1 - Pre Delay           0-80
2 - Envelope            0-1  (real value: 0-flat 1-decaying)
3 - Decay Time          0-11 (real value: 50-600 by 50's)
4 - Accent Delay        0-10 (real value: -50to50 by 10's)
5 - Accent Level        0-10
6 - DRY Level           0-10
7 - RIGHT Level         0-10
8 - LEFT Level          0-10
(additional parameter values in Sysex are ignored)


CONFIG# 9 - Reverse Reverb
#  Parameters         Sysex Values 
1 - Pre Delay           0-80
2 - Reverb Time         0-11 (real value: 50-600 by 50's)
3 - Accent Delay        0-10 (real value: -50to50 by 10's)
4 - Accent Level        0-10
5 - DRY Level           0-10
6 - RIGHT Level         0-10
7 - LEFT Level          0-10
(additional parameter values in Sysex are ignored)


CONFIG# 10 - Parametric EQ 
#  Parameters         Sysex Values 
1 - Band 1 Frequency    0-14 (real value: See Appendix)
2 - Band 1 Level        0-12 (real value: -12to12 by 2's)
3 - Band 2 Frequency    0-21 (real value: See Appendix)
4 - Band 2 Level        0-12 (real value: -12to12 by 2's)
5 - Band 3 Frequency    0-28 (real value: See Appendix)
6 - Band 3 Level        0-12 (real value: -12to12 by 2's)
7 - EQ Level            0-10
(additional parameter values in Sysex are ignored)


CONFIG# 11 - Graphic EQ 
#  Parameters         Sysex Values 
1 - FREQ 63             0-12 (real value: -12to12 by 2's)
2 - FREQ 125            0-12 (real value: -12to12 by 2's)
3 - FREQ 250            0-12 (real value: -12to12 by 2's)
4 - FREQ 500            0-12 (real value: -12to12 by 2's)
5 - FREQ 1k             0-12 (real value: -12to12 by 2's)
6 - FREQ 2k             0-12 (real value: -12to12 by 2's)
7 - FREQ 4k             0-12 (real value: -12to12 by 2's)
8 - FREQ 8k             0-12 (real value: -12to12 by 2's)
9 - FREQ 16k            0-12 (real value: -12to12 by 2's)
(additional parameter values in Sysex are ignored)


CONFIG# 12 - Parametric EQ, Chorus, and Mixer
#  Parameters         Sysex Values 
1 - Band 1 Frequency    0-14 (real value: See Appendix)
2 - Band 1 Level        0-12 (real value: -12to12 by 2's)
3 - Band 2 Frequency    0-21 (real value: See Appendix)
4 - Band 2 Level        0-12 (real value: -12to12 by 2's)
5 - Band 3 Frequency    0-28 (real value: See Appendix)
6 - Band 3 Level        0-12 (real value: -12to12 by 2's)
7 - Delay Source        0-1  (real value: 0-EQ 1-Dry)
8 - Delay In Level      0-10
9 - Delay Time          0-222  (real value: 0-40 by 1's, 40-400 by 5's,                    
                                and 400-1500 by 10's)
10- Feedback            0-10 (real value: 0-99 by 10's) 
11- DRY Level           0-10 
12- EQ Level            0-10
13- DELAY Level R       0-10
14- DELAY Level L       0-10
(additional parameter values in Sysex are ignored)


CONFIG# 13 - Parametric EQ, Chorus, and Mixer
#  Parameters         Sysex Values 
1 - Band 1 Frequency    0-14 (real value: See Appendix)
2 - Band 1 Level        0-12 (real value: -12to12 by 2's)
3 - Band 2 Frequency    0-21 (real value: See Appendix)
4 - Band 2 Level        0-12 (real value: -12to12 by 2's)
5 - Band 3 Frequency    0-28 (real value: See Appendix)
6 - Band 3 Level        0-12 (real value: -12to12 by 2's)
7 - Chorus Source       0-1  (real value: 0-EQ 1-Dry)
8 - Chorus Delay        0-60
9 - LFO Speed           0-65
10- LFO Depth           0-99
11- DRY Level           0-10
12- EQ Level            0-10
13- CHORUS Level R      0-10
14- CHORUS Level L      0-10
(additional parameter values in Sysex are ignored)


CONFIG# 14 - Chorus, Room Reverb, and Mixer
#  Parameters         Sysex Values 
1 - Chorus Delay        0-60
2 - LFO Speed           0-65
3 - LFO Depth           0-99
4 - Reverse In Dry      0-10
5 - Reverse In Chorus   0-10
6 - Reverse Pre-Delay   0-60
7 - Hi-Freq Damping     0-2  (real value: 0-warm 1-soft 2-bright)
8 - Reverb Decay        0-11 (real value: .1-1.2 by .1's)
9 - DRY Level           0-10
10- CHORUS Level R      0-10
11- CHORUS Level L      0-10
12- REVERB Level R      0-10
13- REVERB Level L      0-10
(additional parameter values in Sysex are ignored)


CONFIG# 15 - Delay, Room Reverb, and Mixer
#  Parameters         Sysex Values 
1 - Delay Time          0-147  (real value: 0-40 by 1's, 40-400 by 5's,                    
                                and 400-750 by 10's)
2 - Feedback            0-10  (real value: 0-99 by 10's)
3 - Reverb In Dry       0-10
4 - Reverb in Delay     0-10
5 - Reverb Pre-Delay    0-60
6 - Hi-Freq Damping     0-2   (real value: 0-warm 1-soft 2-bright)
7 - Reverb Decay        0-11  (real value: .1-1.2 by .1's)
8 - DRY Level           0-10
9 - DELAY Level R       0-10
10- DELAY Level L       0-10
11- REVERB Level R      0-10
12- REVERB Level L      0-10
(additional parameter values in Sysex are ignored)


CONFIG# 16 - Chorus, Hall Reverb, and Mixer 
#  Parameters         Sysex Values 
1 - Chorus Delay        0-60
2 - LFO Speed           0-65
3 - LFO Depth           0-99
4 - Reverb In Dry       0-10
5 - Reverb in Chorus    0-10
6 - Reverb Pre-Delay    0-60
7 - Hi-Freq Damping     0-2   (real value: 0-warm 1-soft 2-bright)
8 - Reverb Decay (2)    0-21  (real value: See Appendix)
9 - DRY Level           0-10
10- CHORUS Level R      0-10
11- CHORUS Level L      0-10
12- REVERB Level R      0-10
13- REVERB Level L      0-10
(additional parameter values in Sysex are ignored)


CONFIG# 17 - Delay, Hall Reverb, and Mixer
#  Parameters         Sysex Values 
1 - Delay Time          0-147  (real value: 0-40 by 1's, 40-400 by 5's,                    
                                and 400-750 by 10's)
2 - Feedback            0-10  (real value: 0-99 by 10's)
3 - Reverb In Dry       0-10
4 - Reverb in Delay     0-10
5 - Reverb Pre-Delay    0-60
6 - Hi-Freq Damping     0-2   (real value: 0-warm 1-soft 2-bright)
7 - Reverb Decay (2)    0-21  (real value: See Appendix)
8 - DRY Level           0-10
9 - DELAY Level R       0-10
10- DELAY Level L       0-10
11- REVERB Level R      0-10
12- REVERB Level L      0-10
(additional parameter values in Sysex are ignored)


CONFIG# 18 - Parametric EQ, Gated Reverb, and Mixer
#  Parameters         Sysex Values 
1 - LoPass Filt. Cutoff 0-22  (real value: See Appendix)
2 - Reverb Pre Delay    0-80 
3 - Gate Envelope       0-1   (real value: 0-flat 1-decaying)
4 - Gate Decay Time     0-11  (real value: 50-600 by 50's)        
5 - Accent Delay        0-10 (real value: -50to50 by 10's)
6 - EQ Level R          0-10
7 - EQ Level L          0-10
8 - ACCENT Level R      0-10
9 - ACCENT Level L      0-10
10- REVERB Level R      0-10
11- REVERB Level L      0-10
(additional parameter values in Sysex are ignored)


CONFIG# 19 - Parametric EQ, Reverse Reverb, and Mixer
#  Parameters         Sysex Values 
1 - LoPass Filt. Cutoff 0-22  (real value: See Appendix)
2 - Reverb Pre Delay    0-80 
4 - Reverse Time        0-11  (real value: 50-600 by 50's)        
5 - Accent Delay        0-10 (real value: -50to50 by 10's)
6 - EQ Level R          0-10
7 - EQ Level L          0-10
8 - ACCENT Level R      0-10
9 - ACCENT Level L      0-10
10- REVERSE Level R     0-10
11- REVERSE Level L     0-10
(additional parameter values in Sysex are ignored)


CONFIG# 20 - Parametric EQ, Chorus, Delay, and Mixer
#  Parameters         Sysex Values 
1 - Band 1 Frequency    0-14 (real value: See Appendix)
2 - Band 1 Level        0-12 (real value: -12to12 by 2's)
3 - Band 2 Frequency    0-21 (real value: See Appendix)
4 - Band 2 Level        0-12 (real value: -12to12 by 2's)
5 - Band 3 Frequency    0-28 (real value: See Appendix)
6 - Band 3 Level        0-12 (real value: -12to12 by 2's)
7 - Chorus Source       0-1  (real value: 0-EQ 1-Dry)
8 - Chorus Delay        0-60
9 - LFO Speed           0-65
10- LFO Depth           0-99
11- Delay EQ Source     0-1  (real value: 0-EQ 1-Dry)
12- Delay EQ In Level   0-10
13- Delay Chorus In     0-10
14- Delay Time          0-222  (real value: 0-40 by 1's, 40-400 by 5's,                    
                                and 400-1500 by 10's)
15- Feedback            0-10 (real value: 0-99 by 10's) 

16- DRY Level           0-10
17- EQ Level            0-10
18- DELAY Level R       0-10
19- DELAY Level L       0-10
20- CHORUS Level R      0-10
21- CHORUS Level L      0-10


CONFIG# 21 - Parametric EQ, Flange, Delay, and Mixer
#  Parameters         Sysex Values 
1 - Band 1 Frequency    0-14 (real value: See Appendix)
2 - Band 1 Level        0-12 (real value: -12to12 by 2's)
3 - Band 2 Frequency    0-21 (real value: See Appendix)
4 - Band 2 Level        0-12 (real value: -12to12 by 2's)
5 - Band 3 Frequency    0-28 (real value: See Appendix)
6 - Band 3 Level        0-12 (real value: -12to12 by 2's)
7 - Flange Source       0-1  (real value: 0-EQ 1-Dry)
8 - Flange Delay        0-10
9 - LFO Speed           0-65
10- LFO Depth           0-99
11- Flange Feedback     0-10 (real value: 0-99 by 10's) 
12- Delay EQ Source     0-1  (real value: 0-EQ 1-Dry)
13- Delay EQ In Level   0-10
14- Delay Flange In     0-10
15- Delay Time          0-222  (real value: 0-40 by 1's, 40-400 by 5's,                    
                                and 400-1500 by 10's)
16- Delay Feedback      0-10 (real value: 0-99 by 10's) 
17- EQ Level            0-10
18- DELAY Level R       0-10
19- DELAY Level L       0-10
20- CHORUS Level R      0-10
21- CHORUS Level L      0-10


CONFIG# 22 - Chorus, Delay, Room Reverb, and Mixer
#  Parameters         Sysex Values 
1 - Chorus Delay        0-60
2 - LFO Speed           0-65
3 - LFO Depth           0-99
4 - Delay Dry In        0-10
5 - Delay Chorus In     0-10
6 - Delay Time          0-222  (real value: 0-40 by 1's, 40-400 by 5's,                    
                                and 400-750 by 10's)
7 - Delay Feedback      0-10 (real value: 0-99 by 10's) 
8 - Reverse In Dry      0-10
9 - Reverse In Chorus   0-10
10- Reverse In Delay    0-10
11- Reverse Pre-Delay   0-60
12- Hi-Freq Damping     0-2  (real value: 0-warm 1-soft 2-bright)
13- Reverb Decay        0-11 (real value: .1-1.2 by .1's)
14- DRY Level           0-10
15- CHORUS Level R      0-10
16- CHORUS Level L      0-10
17- DELAY  Level R      0-10
18- DELAY  Level L      0-10
19- REVERB Level R      0-10
20- REVERB Level L      0-10
(additional parameter values in Sysex are ignored)

CONFIG# 23 - Flange, Delay, Room Reverb, and Mixer
#  Parameters         Sysex Values 
1 - Flange Delay        0-10
2 - LFO Speed           0-65
3 - LFO Depth           0-99
4 - Flange Feedback     0-10 (real value: 0-99 by 10's) 
5 - Delay Dry In        0-10  
6 - Delay Flange In     0-10
7 - Delay Time          0-147  (real value: 0-40 by 1's, 40-400 by 5's,                    
                                and 400-750 by 10's)
8 - Delay Feedback      0-10  (real value: 0-99 by 10's)
9 - Reverb In Dry       0-10
10- Reverb Flange In    0-10
11- Reverb in Delay     0-10
12- Reverb Pre-Delay    0-60
13- Hi-Freq Damping     0-2   (real value: 0-warm 1-soft 2-bright)
14- Reverb Decay        0-11  (real value: .1-1.2 by .1's)
15- DRY Level           0-10
16- FLANGE Level R      0-10
17- FLANGE Level L      0-10
18- DELAY Level R       0-10
19- DELAY Level L       0-10
20- REVERB Level R      0-10
21- REVERB Level L      0-10


CONFIG# 24 - Chorus, Delay, Hall Reverb, and Mixer
#  Parameters         Sysex Values 
1 - Chorus Delay        0-60
2 - LFO Speed           0-65
3 - LFO Depth           0-99
4 - Delay Dry In        0-10
5 - Delay Chorus In     0-10
6 - Delay Time          0-222  (real value: 0-40 by 1's, 40-400 by 5's,                    
                                and 400-750 by 10's)
7 - Delay Feedback      0-10 (real value: 0-99 by 10's) 
8 - Reverse In Dry      0-10
9 - Reverse In Chorus   0-10
10- Reverse In Delay    0-10
11- Reverse Pre-Delay   0-60
12- Hi-Freq Damping     0-2  (real value: 0-warm 1-soft 2-bright)
13- Reverb Decay (2)    0-21 (real value: See Appendix)
14- DRY Level           0-10
15- CHORUS Level R      0-10
16- CHORUS Level L      0-10
17- DELAY  Level R      0-10
18- DELAY  Level L      0-10
19- REVERB Level R      0-10
20- REVERB Level L      0-10
(additional parameter values in Sysex are ignored)


CONFIG# 25 - Flange, Delay, Hall Reverb, and Mixer
#  Parameters         Sysex Values 
1 - Flange Delay        0-10
2 - LFO Speed           0-65
3 - LFO Depth           0-99
4 - Flange Feedback     0-10 (real value: 0-99 by 10's) 
5 - Delay Dry In        0-10  
6 - Delay Flange In     0-10
7 - Delay Time          0-147  (real value: 0-40 by 1's, 40-400 by 5's,                    
                                and 400-750 by 10's)
8 - Delay Feedback      0-10  (real value: 0-99 by 10's)
9 - Reverb In Dry       0-10
10- Reverb Flange In    0-10
11- Reverb in Delay     0-10
12- Reverb Pre-Delay    0-60
13- Hi-Freq Damping     0-2   (real value: 0-warm 1-soft 2-bright)
14- Reverb Decay        0-11  (real value: .1-1.2 by .1's)
15- DRY Level           0-10
16- FLANGE Level R      0-10
17- FLANGE Level L      0-10
18- DELAY Level R       0-10
19- DELAY Level L       0-10
20- REVERB Level R      0-10
21- REVERB Level L      0-10


CONFIG# 26 - Parametric EQ, Chorus, 4-Tap Delay, and Mixer
#  Parameters         Sysex Values 
1 - LoPass Filt. Cutoff 0-22  (real value: See Appendix)
2 - Chorus Delay        0-60
3 - LFO Speed           0-65
4 - LFO Depth           0-99
5 - Delay EQ In         0-10
6 - Delay Chorus In     0-10
7 - Tap 1               0-222  (real value: 0-40 by 1's, 40-400 by 5's,                    
                                and 400-1500 by 10's)
8 - Tap 2               0-222  (real value: 0-40 by 1's, 40-400 by 5's,                    
                                and 400-1500 by 10's)
9 - Tap 3               0-222  (real value: 0-40 by 1's, 40-400 by 5's,                    
                                and 400-1500 by 10's)
10- Tap 4               0-222  (real value: 0-40 by 1's, 40-400 by 5's,                    
                                and 400-1500 by 10's)
11- Feedback Delay      0-222  (real value: 0-40 by 1's, 40-400 by 5's,                    
                                and 400-1500 by 10's)
12- 4-tap Feedback      0-10  (real value: 0-99 by 10's)
13- EQ Level            0-10
14- CHORUS Level        0-10
15- Tap1 Level R        0-10
16- Tap1 Level L        0-10
17- Tap2 Level          0-10
18- Tap3 Level          0-10
19- Tap4 Level R        0-10
20- Tap4 Level L        0-10
(additional parameter values in Sysex are ignored)


CONFIG# 27 - Parametric EQ, Flange, 4-Tap Delay, and Mixer
#  Parameters         Sysex Values 
1 - LoPass Filt. Cutoff 0-22  (real value: See Appendix)
2 - Flange Delay        0-60
3 - LFO Speed           0-65
4 - LFO Depth           0-99
5 - Flange Feedback     0-10  (real value: 0-99 by 10's)
6 - Delay EQ In         0-10
7 - Delay Flange In     0-10
8 - Tap 1               0-222  (real value: 0-40 by 1's, 40-400 by 5's,                    
                                and 400-1500 by 10's)
9 - Tap 2               0-222  (real value: 0-40 by 1's, 40-400 by 5's,                    
                                and 400-1500 by 10's)
10- Tap 3               0-222  (real value: 0-40 by 1's, 40-400 by 5's,                    
                                and 400-1500 by 10's)
11- Tap 4               0-222  (real value: 0-40 by 1's, 40-400 by 5's,                    
                                and 400-1500 by 10's)
12- Feedback Delay      0-222  (real value: 0-40 by 1's, 40-400 by 5's,                    
                                and 400-1500 by 10's)
13- 4-tap Feedback      0-10  (real value: 0-99 by 10's)
14- EQ Level            0-10
15- FLANGE Level        0-10
16- Tap1 Level R        0-10
17- Tap1 Level L        0-10
18- Tap2 Level          0-10
19- Tap3 Level          0-10
20- Tap4 Level R        0-10
21- Tap4 Level L        0-10


EFFECT PARAMETER REAL VALUES APPENDIX
+++++++++++++++++++++++++++++++++++++

-DECAY TIME (1)
0 - 1.0         16- 12.0
1 - 1.2         17- 14.0
2 - 1.4         18- 16.0
3 - 1.6         19- 19.0
4 - 1.9         20- 22.0
5 - 2.2         21- 25.0
6 - 2.5         22- 29.0
7 - 2.9         23- 34.0
8 - 3.4         24- 40.0
9 - 4.0         25- 46.0
10- 4.6         26- 54.0
11- 5.4         27- 63.0
12- 6.3         28- 74.0
13- 7.4         29- 86.0
14- 8.6         30- 99.0
15- 10.0

-DECAY TIME (2)
0 - .7          12- 4.4
1 - .8          13- 5.2
2 - 1.0         14- 6.0
3 - 1.1         15- 7.0
4 - 1.3         16- 8.4
5 - 1.5         17- 9.8
6 - 1.8         18- 11.2
7 - 2.0         19- 13.3
8 - 2.4         20- 15.4
9 - 2.8         21- 17.5
10- 3.2         22- 20.3
11- 3.8         23- 23.8

-BAND 1 FREQ
0 - .10         8 - 1.60  
1 - .14         9 - 2.26
2 - .20         10- 3.20
3 - .28         11- 4.53
4 - .40         12- 6.40
5 - .56         13- 9.05
6 - .80         14- 12.60
7 - 1.13

-BAND 2 FREQ
0 - .10         11- 1.25
1 - .12         12- 1.60
2 - .16         13- 2.00
3 - .20         14- 2.50
4 - .25         15- 3.20
5 - .31         16- 4.00
6 - .40         17- 5.00
7 - .50         18- 6.40
8 - .63         19- 8.00
9 - .80         20- 10.00
10- 1.00        21- 12.80

-BAND 3 FREQ
0 - .10         15- 1.35
1 - .12         16- 1.60
2 - .14         17- 1.90
3 - .17         18- 2.26
4 - .20         19- 2.69
5 - .24         20- 3.20
6 - .28         21- 3.80
7 - .34         22- 4.53
8 - .40         23- 5.40
9 - .48         24- 6.40
10- .56         25- 7.60
11- .67         26- 9.05
12- .80         27- 10.80
13- .95         28- 12.80
14- 1.13

-REVERB DECAY TIME  (3)
0 - 1.0          11- 4.80
1 - 1.15         12- 5.54
2 - 1.33         13- 6.39
3 - 1.53         14- 7.37
4 - 1.77         15- 8.50
5 - 2.00         16- 9.80
6 - 2.35         17- 11.30
7 - 2.71         18- 13.00
8 - 3.13         19- 15.00
9 - 3.61         20- 17.30
10- 4.16         21- 20.00

-LO PASS
0 - .10         12- 3.20
1 - .48         13- 3.80
2 - .57         14- 4.50
3 - .67         15- 5.30
4 - .80         16- 6.40
5 - .95         17- 7.60
6 - 1.10        18- 9.00
7 - 1.30        19- 10.00
8 - 1.60        20- 12.00
9 - 1.90        21- 15.00
10- 2.20        22- 18.00
11- 2.60
```

== Velocity and pressure maps

```
Both of these Maps hold eight values, which correspond to the
eight dynamic levels of standard musical notation.  These values
determine the MIDI attack velocity value or the Mono pressure
value that must be generated to achieve the dynamic level for
that parameter .


BANKS- Velocity/Pressure Maps may be stored in:
         ROM:     1-75
         RAM:   100-119
                200-219
                   .
                   .
                900-919


Sysex Data for Velocity/Pressure Map : 0-7
Data Starts Here---
LineNo. 
----------------------------------------
   0:   PPP VALUE (0-127) 
   1:   PP VALUE  (0-127)
   2:   P VALUE   (0-127)
   3:   MP VALUE  (0-127)
   4:   MF VALUE  (0-127)
   5:   F VALUE   (0-127)
   6:   FF VALUE  (0-127)
   7:   FFF VALUE (0-127) 
```

== Setups

```
Setups enable you to use three keyboard zones, each of which can
have its own program, MIDI channel, and control assignments. Its
Sysex data is pretty self-explanatory: A few Global values, then
split into 3 zones with the same variables in each...


The Setup Parameters are as follows:
        
        PROGRAM                 Program List                       
        CHANNEL                 Off, 1 to 16
        TRANSPOSE               +/- 60 semitones
        LOW KEY                 C-1 to G9
        HIGH KEY                C-1 to G9
        EFFECT                  Preset Effects list
        EFFECTS MIX             0-100% wet
        MODE                    Off, Both, MIDI, Local
        PITCH BEND              Off, On
        PROGRAM CHANGE          Off, On
        MOD WHEEL               MIDI Control Source list
        FOOT SWITCH1            MIDI Control Source list
        FOOT SWITCH2            MIDI Control Source list
        CONTROL PEDAL           MIDI Control Source list
        CONTROL SLIDER          MIDI Control Source list
        MONO PRESSURE           MIDI Control Source list
        MIDI VOLUME (Version 2) MIDI Volume (0-127)

BANKS- Setups may be stored in:
         ROM:     1-99
                100-199
         RAM:   200-299
                300-399
                   .
                   .
                900-999

Sysex Data for Setup : 0-55
Data Starts Here---
LineNo. 
----------------------------------------
   0:   Hex constant (0Fh)  
   1:   EFFECT (Hi Value) =  Effect (mod) 256
   2:   EFFECT (Lo Value) = - the remainder -
   3:   EFFECTS MIX
   4:   Hex constant (00h)  
   5:   Hex constant (00h)  
   6:   Hex constant (00h)  
   7:   Hex constant (00h)  
---8:   Hex constant (03h)  
   9:   Zone 1 - CHANNEL               
  10:   Zone 1 - PROGRAM (Hi Value) =  Program (mod) 256
  11:   Zone 1 - PROGRAM (Lo Value) =  - remainder-
  12:   Zone 1 - LOW KEY (0-127) 
  13:   Zone 1 - HI KEY (0-127)
  14:   Zone 1 - *BITMAP see below* (ProgramChange/PitchBend/Mode)
  15:   Zone 1 - TRANSPOSE (0-255)  (0-127 is Positive, 255-128 is Neg)
  16:   Zone 1 - MONO PRESSURE 
  17:   Zone 1 - MOD WHEEL
  18:   Zone 1 - CONTROL PEDAL
  19:   Zone 1 - CONTROL SLIDER
  20:   Zone 1 - FOOT SWITCH 1
  21:   Zone 1 - FOOT SWITCH 2
  22:   Hex constant (00h)
  23:   Zone 1 - MIDI VOLUME
--24:   Hex constant (04h)  
  25:   Zone 2 - CHANNEL               
  26:   Zone 2 - PROGRAM (Hi Value) =  Program (mod) 256
  27:   Zone 2 - PROGRAM (Lo Value) =  - remainder-
  28:   Zone 2 - LOW KEY (0-127) 
  29:   Zone 2 - HI KEY (0-127)
  30:   Zone 2 - *BITMAP see below* (ProgramChange/PitchBend/Mode)
  31:   Zone 2 - TRANSPOSE (0-255)  (0-127 is Positive, 255-128 is Neg)
  32:   Zone 2 - MONO PRESSURE 
  33:   Zone 2 - MOD WHEEL
  34:   Zone 2 - CONTROL PEDAL
  35:   Zone 2 - CONTROL SLIDER
  36:   Zone 2 - FOOT SWITCH 1
  37:   Zone 2 - FOOT SWITCH 2
  38:   Hex constant (00h)
  39:   Zone 2 - MIDI VOLUME
--40:   Hex constant (05h)  
  41:   Zone 3 - CHANNEL               
  42:   Zone 3 - PROGRAM (Hi Value) =  Program (mod) 256
  43:   Zone 3 - PROGRAM (Lo Value) =  - remainder-
  44:   Zone 3 - LOW KEY (0-127) 
  45:   Zone 3 - HI KEY (0-127)
  46:   Zone 3 - *BITMAP see below* (ProgramChange/PitchBend/Mode)
  47:   Zone 3 - TRANSPOSE (0-255)  (0-127 is Positive, 255-128 is Neg)
  48:   Zone 3 - MONO PRESSURE 
  49:   Zone 3 - MOD WHEEL
  50:   Zone 3 - CONTROL PEDAL
  51:   Zone 3 - CONTROL SLIDER
  52:   Zone 3 - FOOT SWITCH 1
  53:   Zone 3 - FOOT SWITCH 2
  54:   Hex constant (00h)
  55:   Zone 3 - MIDI VOLUME
    

BINARY BITMAP-(line #'s 14, 30 and 46)
========================================   
   PROGRAM CHANGE  
       1-on
       0-off
         |
   0000|XXXX
        |  \\________ MODE
PITCH BEND             11-off
   1-on                00-both
   0-off               01-MIDI
                       10-Local
```
== Quick Access Banks
```
In Quick Access Mode, the user can select PROGRAMS and SETUPS
with the single press of an alphanumeric button (or with the
other data entry methods).... Each bank contains ten momory
slots, or entries, where you can store programs or setups in any
combination.  Any program or setup in the currently selected bank
can be selected with the numeric buttons 0 through 9.....(Excerpt
from K2000 Users Manual)

   1 2 3
   4 5 6
   7 8 9
     0


Quick Access Banks May be Stored in:
	ROM:	  1-75
	RAM:	100-119
		200-219
		   .
		   .
		900-919



Sysex Data for Quick Access Bank : 0-25
Data Starts Here---
LineNo. 
----------------------------------------
   0:   Hex constant (00h)  
   1:   Hex constant (00h)  
   2:   Hex constant (00h)  
   3:   Hex constant (0Ah)  
   4:   Hex constant (00h)  
   5:   Hex constant (02h)  
   6:   '0' PROGRAM/SETUP NUMBER (high)  **see below**
   7:   '0' PROGRAM/SETUP NUMBER  (lo)   **see below**
   8:   '1' PROGRAM/SETUP NUMBER (high) 
   9:   '1' PROGRAM/SETUP NUMBER  (lo) 
  10:   '2' PROGRAM/SETUP NUMBER (high) 
  11:   '2' PROGRAM/SETUP NUMBER  (lo) 
  12:   '3' PROGRAM/SETUP NUMBER (high) 
  13:   '3' PROGRAM/SETUP NUMBER  (lo) 
  14:   '4' PROGRAM/SETUP NUMBER (high) 
  15:   '4' PROGRAM/SETUP NUMBER  (lo) 
  16:   '5' PROGRAM/SETUP NUMBER (high) 
  17:   '5' PROGRAM/SETUP NUMBER  (lo) 
  18:   '6' PROGRAM/SETUP NUMBER (high) 
  19:   '6' PROGRAM/SETUP NUMBER  (lo) 
  20:   '7' PROGRAM/SETUP NUMBER (high) 
  21:   '7' PROGRAM/SETUP NUMBER  (lo) 
  22:   '8' PROGRAM/SETUP NUMBER (high) 
  23:   '8' PROGRAM/SETUP NUMBER  (lo) 
  24:   '9' PROGRAM/SETUP NUMBER (high) 
  25:   '9' PROGRAM/SETUP NUMBER  (lo) 


***HIGH VALUE- If it is a PROGRAM, Value := 144 + (Program# MOD 256)
               If it is a SETUP,   Value := 156 + (Setup# MOD 256)

***LO VALUE - The remainder....
```

== Intonation tables

```
 Intonation Tables define the interval between the notes in each
octave. (Excerpt from the K2000 User Manual)


BANKS- Intonation Tables may be stored in:
         ROM:     1-75
         RAM:   100-119
                200-219
                   .
                   .
                900-919


*Note* On Version 2 machines, Intonation editor uses the labels:
Minor 2nd, Major 2nd, Minor 3rd, Major 3rd, and so on...
Version 3 machines have a graphic screen representing a C octave, 
where each value is the amount of detuning applied to each note.
The low C always represents the tonic (Set in MASTER by the IntonaKey
parameter). I, myself, only have version 3 installed...and am making 
an educated guess as to what the Version 2 labels correspond to.
If anyone can provide me the information (if it is wrong), please
email me!

Sysex Data for Intonation Table : 0-23
Data Starts Here---
LineNo. 
----------------------------------------
   0:   Tonic (C)  Hi Value *see below*   \__ (real value: 0-9999)
   1:   Tonic (C)  Lo Value *see below*   /
   2:   Minor 2nd (C#)  Hi Value \__ (real value: 0-9999)  
   3:   Minor 2nd (C#)  Lo Value /
   4:   Major 2nd (D)  Hi Value  \__ (real value: 0-9999) 
   5:   Major 2nd (D)  Lo Value  /
   6:   Minor 3rd (D#)  Hi Value \__ (real value: 0-9999)  
   7:   Minor 3rd (D#)  Lo Value /
   8:   Major 3rd (E)  Hi Value  \__ (real value: 0-9999) 
   9:   Major 3rd (E)  Lo Value  /
  10:   Perfect 4th (F)  Hi Value  \__ (real value: 0-9999) 
  11:   Perfect 4th (F)  Lo Value  /
  12:   Tritone (F#)  Hi Value \__ (real value: 0-9999)  
  13:   Tritone (F#)  Lo Value /
  14:   Perfect 5th (G)  Hi Value  \__ (real value: 0-9999) 
  15:   Perfect 5th (G)  Lo Value  /
  16:   Minor 6th (G#)  Hi Value \__ (real value: 0-9999)  
  17:   Minor 6th (G#)  Lo Value /
  18:   Major 6th (A)  Hi Value  \__ (real value: 0-9999) 
  19:   Major 6th (A)  Lo Value  /
  20:   Dominant 7th (A#)  Hi Value \__ (real value: 0-9999)  
  21:   Dominant 7th (A#)  Lo Value /
  22:   Major 7th (B)  Hi Value  \__ (real value: 0-9999) 
  23:   Major 7th (B)  Lo Value  /

Hi Value:= (RealValue MOD 256)
Lo Value:=   ***remainder***
```

= Robert Fries work (1993)

```
Reply-To:     K2000 user's group <K2000@JHUVM.HCF.JHU.EDU>
Sender:       K2000 user's group <K2000@JHUVM.HCF.JHU.EDU>
From:         jk <JKRIKAWA@CCIT.ARIZONA.EDU>
Subject:      SYSEX I: (long post, delete if no interest)
X-To:         K2000@jhuvm.hcf.jhu.edu
To:           Multiple recipients of list K2000

All right you dogs:

This first post was compiled through hacking by a guy who no
longer owns a K2000 (Robert Fries....that's it)  He gets all
credit for this listing.

------------------------------------------------------- 11 NOV 1993
There has been some interest in this mailing list for more info
on the K2000 program format. I have done some of the work to
figure this out.

Below is a partial analysis of the K2000 program format. It is
not complete, and I make no guarantee of its correctness. This
information was derived from a K2000 using v1.3 firmware.

Again: NO GUARANTEES! I'd welcome correspondence from anyone else
working on this. I'd also be glad to offer some assistance to
people interested in attempting this kind of thing, and/or those
having trouble just getting the low-level sysex stuff working.
```

== General Info

```
K2000 program objects are in the following format:

 - 48 bytes global stuff for entire program
 - 224 bytes layer information per layer

So, a one-layer program is 272 bytes long, and an 'n-layer'
program is 48 + (n * 224) bytes long.

In the following list, offsets marked UNCHANGED seemed to be
constants for all K2000 programs. In all of the approx. 300
programs I scanned, values at these offsets never changed. The
UPPERCASE word at the start of each line below is the edit screen
where this parameter is the name of the edit screen where the
parameter is found.

Offsets which have neither UNCHANGED nor any explanation have not
yet been decoded. This is the 'incomplete' part. Items with an
asterisk (*) are probably explained in the bitmap stuff at the
end of this 'document'.

There is no 'change parameter' sysex command in the K2000. To
edit these values from a computer, the sysex 'load' command is
used. The format for this command is found in the K2000 manual.

I hope this information helps someone, or serves as a basis for
further investigation.
```
== Offsets

```
OFFSET
------
0   -     UNCHANGED
1   -     UNCHANGED
2   -   # layers
3   - * COMMON  bitmap MONO,PORT,GLOBALS,ATT PORT,LEGATO
        (see bitmaps)
4   -   COMMON pitch bend range
5   -   COMMON portamento rate
6   -   EFFECT Wet/Dry SRC
7   -   EFFECT Wet/Dry Depth
8   -   EFFECT variable 1 adjust
9   -   EFFECT variable 1 src
10  -   EFFECT variable 1 depth
11  -   EFFECT variable 1
12  -   EFFECT variable 2 adjust
13  -   EFFECT variable 2 src
14  -   EFFECT variable 2 depth
15  -   EFFECT variable 2
16  -      UNCHANGED
17  -      UNCHANGED
18  -   ASR gasr2 trig
19  -   ASR gasr2 mode
20  -   ASR gasr2 dly
21  -   ASR gasr2 atk
22  -      UNCHANGED
23  -   ASR gasr2 rls
24  -      UNCHANGED
25  -
26  -
27  -
28  -      UNCHANGED
29  -      UNCHANGED
30  -   LFO glfo2 rate ctl
31  -   LFO glfo2 min rate
32  -   LFO glfo2 max rate
33  -   LFO glfo2 phase
34  -   LFO glfo2 shape
35  -      UNCHANGED
36  -      UNCHANGED
37  -
38  -
39  -
40  -      UNCHANGED
41  -      UNCHANGED
42  -   EFFECT preset #
43  -   EFFECT Wet/Dry Mix Adjust
44  -      UNCHANGED
45  -      UNCHANGED
46  -      UNCHANGED
47  -      UNCHANGED
48  -      UNCHANGED
49  -      UNCHANGED
50  -      UNCHANGED
51  -      UNCHANGED
52  -   LAYER lo key
53  -   LAYER hi key
54  -   LAYER lo level/hi level
55  -   LAYER enable on/off
56  - * LAYER sust,sost,freeze,ign rls,til dec, thr attak (see
        bitmaps)
57  - * LAYER pbend mode, EnableS, opaque, KEYMAP stereo,
        OUTPUT xfade sense
58  - * VTRIG (see bitmaps)
59  -      UNCHANGED
60  -  LAYER delay ctl
61  -  LAYER min delay
62  -  LAYER max delay
63  -  OUTPUT xfade
64  -      UNCHANGED
65  -      UNCHANGED
66  -  ASR asr1 trig
67  -  ASR asr1 mode
68  -  ASR asr1 dly
69  -  ASR asr1 attak
70  -      UNCHANGED
71  -  ASR asr1 rls
72  -      UNCHANGED
73  -      UNCHANGED
74  -
75  -
76  -
77  -
78  -      UNCHANGED
79  -
80  -      UNCHANGED
81  -  FUN fun1 function
82  -  FUN fun1 input A
83  -  FUN fun1 input B
84  -      UNCHANGED
85  -  FUN fun2 function
86  -  FUN fun2 input A
87  -  FUN fun2 input B
88  -      UNCHANGED
89  -      UNCHANGED
90  -  LFO lfo1 rate ctl
91  -  LFO lfo1 min rate
92  -  LFO lfo1 max rate
93  -  LFO lfo1 phase
94  -  LFO lfo1 shape
95  -       UNCHANGED
96  -       UNCHANGED
97  -       UNCHANGED
98  -
99  -
100 -
101 -
102 -
103 -        UNCHANGED
104 -        UNCHANGED
105 -   FUN fun3 function
106 -   FUN fun3 input A
107 -   FUN fun3 input B
108 -        UNCHANGED
109 -   FUN fun4 function
110 -   FUN fun4 input A
111 -   FUN fun4 input B
112 -        UNCHANGED
113 -
114 -   AMPENV user/natural
115 -   ENVCTL  att adjust
116 -   ENVCTL  att keytrak
117 -   ENVCTL  att vel trk
118 -   ENVCTL  att src
119 -   ENVCTL  att depth
120 -   ENVCTL  dec adjust
121 -   ENVCTL  dec keytrak
122 -   ENVCTL  dec src
123 -   ENVCTL  dec depth
124 -   ENVCTL  rel adjust
125 -   ENVCTL  rel keytrak
126 -   ENVCTL rel src
127 -   ENVCTL  rel depth
128 -       UNCHANGED
129 -   AMPENV  LOOP seg-X (2 parms)
130 -   AMPENV  att1 %
131 -   AMPENV  att1 time
132 -   AMPENV  att2 %
133 -   AMPENV att2 time
134 -   AMPENV att3 %
135 -   AMPENV att3 time
136 -   AMPENV dec1 %
137 -   AMPENV dec1 time
138 -   AMPENV rel1 %
139 -   AMPENV rel1 time
140 -   AMPENV rel2 %
141 -   AMPENV rel2 time
142 -       UNCHANGED
143 -   AMPENV rel3 time
144 -       UNCHANGED
145 -   ENV2    LOOP seg-X (2 parms)
146 -   ENV2    att1 %
147 -   ENV2    att1 time
148 -   ENV2    att2 %
149 -   ENV2   att2 time
150 -   ENV2   att3 %
151 -   ENV2   att3 time
152 -   ENV2   dec1 %
153 -   ENV2   dec1 time
154 -   ENV2   rel1 %
155 -   ENV2   rel1 time
156 -   ENV2   rel2 %
157 -   ENV2   rel2 time
158 -   ENV2   rel3 %
159 -   ENV2   rel 3 time
160 -       UNCHANGED
161 -   ENV3    LOOP seg-X (2 parms)
162 -   ENV3    att1 %
163 -   ENV3    att1 time
164 -   ENV3    att2 %
165 -   ENV3   att2 time
166 -   ENV3   att3 %
167 -   ENV3   att3 time
168 -   ENV3   dec1 %
169 -   ENV3   dec1 time
170 -   ENV3   rel1 %
171 -   ENV3   rel1 time
172 -   ENV3   rel2 %
173 -   ENV3   rel2 time
174 -   ENV3   rel3 %
175 -   ENV3   rel 3 time
176 -       UNCHANGED
177 -       UNCHANGED
178 -   KEYMAP  Transpose/Timbre shift (also 178, 192, 194)
179 -         UNCHANGED
180 -   KEYMAP key track
181 -   KEYMAP vel track
182 -       UNCHANGED
183 -       UNCHANGED
184 -       UNCHANGED
185 -
186 -       UNCHANGED
187 -       UNCHANGED
188 -       UNCHANGED
189 -   KEYMAP keymap #
190 -       UNCHANGED
191 -   KEYMAP Alt. Att. Ctl.
192 -   KEYMAP timber shift            (also 178, 192, 194)
193 -      UNCHANGED
194 -   PITCH                          (also 178, 192, 194)
195 -   PITCH
196 -   PITCH
197 -   PITCH
198 -   PITCH
199 -   PITCH
200 -   PITCH
201 -   PITCH
202 -   PITCH
203 -   PITCH
204 -      UNCHANGED
205 -   KEYMAP playback mode
206 -   Algorithm #
207 -   PITCH fine hz
208 -      UNCHANGED
209 -
210 -   F1 coarse
211 -   F1 fine
212 -   F1  key track
213 -   F1  vel track
214 -   F1  src1
215 -   F1  depth
216 -   F1  depth ctl
217 -   F1  min depth
218 -   F1  max depth
219 -   F1  src2
220 -   F1  pad
221 -      UNCHANGED
222 -      UNCHANGED
223 -   F1  fine hz
224 -      UNCHANGED
225 -
226 -   F2 coarse
227 -   F2 fine
228 -   F2  key track
229 -   F2  vel track
230 -   F2  src1
231 -   F2  depth
232 -   F2  depth ctl
233 -   F2  min depth
234 -   F2  max depth
235 -   F2  src2
236 -
237 -      UNCHANGED
238 -      UNCHANGED
239 -   F2  fine hz
240 -      UNCHANGED
241 -
242 -   F3 coarse
243 -   F3 fine
244 -   F3  key track
245 -   F3  vel track
246 -   F3  src1
247 -   F3  depth
248 -   F3  depth ctl
249 -   F3  min depth
250 -   F3  max depth
251 -   F3  src2
252 -   F4AMP Pad (also 252,268)
253 -      UNCHANGED
254 -
255 -
256 -      UNCHANGED
257 -
258 -  F4AMP Adjust
259 -
260 -   F4AMP  key track
261 -   F4AMP  vel track
262 -   F4AMP  src1
263 -   F4AMP  depth
264 -   F4AMP  depth ctl
265 -   F4AMP  min depth
266 -   F4AMP  max depth
267 -   F4AMP  src2
268 -   F4AMP Pad (also 252,268)
269 -    UNCHANGED
270 - * OUTPUT PAIR+GAIN
271 - * OUTPUT PAN+MODE
```
== Bitmaps
```
offset 3  COMMON  bitmap MONO,PORT,GLOBALS,ATT PORT,LEGATO
verified: offset 3 in progs 1 - 199 never > 31
MONO                0x01
PORTAMENTO          0x02    * not in menu AND ignored if MONO = 0
GLOBALS             0x04
ATTACK_PORTAMENTO   0x08    * not in menu AND ignored if MONO = 0
LEGATO              0x10    * not in menu AND ignored if MONO = 0

offset 56
verified: offset 56 in progs 1 - 199 never > 63

IgnRel      0x01
SusPdl      0x02
SosPdl      0x04
FrzPdl      0x08
ThrAtt      0x10
TilDec      0x20

offset 57
LAYER PitchBend_ALL 0x00   \
LAYER PitchBend_KEY 0x02    - BITS 0 and 1 - value 3 never happens
LAYER PitchBend_OFF 0x01   /
LAYER EnableS       0x04    1 = norm 0 = rvrs
LAYER Opaque        0x08    1 = ON
KEYMAP Stereo       0x20    1 = ON 0 = OFF
OUTPUT xfade sense  0x40    1 = rvrs 0 = norm

offset 58 VTRIG

X XXX X XXX
^ VTRIG1 level  (3 bits - see below)
^   VTRIG1 sense   0 = normal 1 = reverse
^       VTRIG2 level  (3 bits - see below)
^         VTRIG2 sense   0 = normal 1 = reverse

levels (bits)
ppp = 000
pp = 001
p = 010
mp = 011
mf = 100
f = 101
ff = 110
fff = 111

-----------------------------------------------------------
love,
_
-john       ___                                    __/ |
___        |   |     JKrikawa@CCIT.Arizona.Edu    |    |___
________ \______/     \__________ Tucson, AZ ___../\./\/
\____/        \____

```

= John Krikawa–Fries work (1994)

```
1994 10:41:34 -0500 Date:         Wed, 30 Nov 1994 08:41:28 -0700
Reply-To:     K2000 user's group <K2000@JHUVM.HCF.JHU.EDU>
Sender:       K2000 user's group <K2000@JHUVM.HCF.JHU.EDU>
From:         jk <JKRIKAWA@CCIT.ARIZONA.EDU>
Subject:      SYSEX II: verified and extended SYSEX I
X-To:         K2000@jhuvm.hcf.jhu.edu
To:           Multiple recipients of list K2000

So then I took Robert's work (God bless him) and took a few days
off and hacked and hacked and hacked.  I verified/and altered his
work and added a lot of detail.  There is enough stuff in the
manual to work with in terms of sending and receiving SYSEX.  I'm
a total weenie though because after all this damn work 1) I
haven't used any of it, 2) haven't tried to send SYSEX while the
K2K was playing, so I don't know if the output changes Michael
wants to make work in real time.

My hack session was a lot of work, I would appreciate discretion.
I don't really want to see this shit all over USENET tomorrow.
It's not that complete and still needs a lot of work.

If you do anything with it, let me know out of interest.
Thanks!


                                  love, _ -john       ___                                    __/ |
___        |   |     JKrikawa@CCIT.Arizona.Edu    |    |___
________ \______/     \__________ Tucson, AZ ___../\./\/ \____/
\____
-------------------------------------------------------------

1994 10:53:29 -0500 Date:         Wed, 30 Nov 1994 08:52:23 -0700
Reply-To:     K2000 user's group <K2000@JHUVM.HCF.JHU.EDU>
Sender:       K2000 user's group <K2000@JHUVM.HCF.JHU.EDU>
From:         jk <JKRIKAWA@CCIT.ARIZONA.EDU>
Subject:      SYSEX II:listing (long)
X-To:         K2000@jhuvm.hcf.jhu.edu
To:           Multiple recipients of list K2000

Sorry for the message break,  I had to convert it from WordPerfect.
```
== Offets
```
OBJECT: Program

0   8 (unknown/constant)
1   2 (unknown/constant)
2   # layers
3   * COMMON  bitmap MONO/PORT/GLOBALS/ATT/PORT LEGATO (see
bitmaps) 4   COMMON pitch bend range
5   COMMON portamento rate
6   EFFECT Wet/Dry SRC (ctl-list)
7   EFFECT Wet/Dry Depth
8   EFFECT variable 1 adjust
9   EFFECT variable 1 src
10   EFFECT variable 1 depth
11   EFFECT variable 1
12   EFFECT variable 2 adjust
13   EFFECT variable 2 src
14   EFFECT variable 2 depth
15   EFFECT variable 2
16   17 (unknown/constant)
17   0 (unknown/constant)
18   ASR gasr2 trig
19   ASR gasr2 mode
20   ASR gasr2 dly
21   ASR gasr2 atk
22   0 (unknown/constant)
23   ASR gasr2 rls
24   25 (unknown/constant)
25   Global ON 0000 0010
26   0
27   0
28   21 (unknown/constant)
29   0  (unknown/constant)
30   LFO glfo2 rate ctl
31   LFO glfo2 min rate
32   LFO glfo2 max rate
33   LFO glfo2 phase
34   LFO glfo2 shape
35   0 (unknown/constant)
36   27 (unknown/constant)
37   Global ON 0000 0100
38   0
39   0
40   15 (unknown/constant)
41   0 (unknown/constant)
42   EFFECT preset # (in memory order)
43   EFFECT Wet/Dry Mix Adjust %
44   0 (unknown/constant)
45   0 (unknown/constant)
46   0 (unknown/constant)
47   0 (unknown/constant)
48   9 (unknown/constant)
49   0 (unknown/constant)
50   0 (unknown/constant)
51   0 (unknown/constant)
52   LAYER lo key
53   LAYER hi key
54   LAYER lo vel/hi vel 000+000
55   LAYER enable (Ctl-list)
56   * LAYER sust/sost/freez/ign/rls/til/dec/thr attk (see bitmaps)
57   * LAYER pbend mode/EnableS/opaque/KEYMAP stereo/OUTPUT/xfade
sense 58   * VTRIG (see bitmaps)
59   0 (unknown/constant)
60   LAYER delay ctl
61   LAYER min delay
62   LAYER max delay
63   OUTPUT xfade (Ctl-list)
64   16 (unknown/constant)
65   0 (unknown/constant)
66   ASR asr1 trig (Ctl-list)
67   ASR asr1 mode (norm=0  hold=1  rpt=2)
68   ASR asr1 dly
69   ASR asr1 attak
70   0 (unknown/constant)
71   ASR asr1 rls
72   17 (unknown/constant)
73   0 (unknown/constant)
74   ASR asr2 trig (Ctl-list)
75   ASR asr2 mode (norm=0  hold=1  rpt=2)
76   ASR asr2 dly
77   ASR asr2 attak   78   0 (unknown/constant)
79   ASR asr2 rls
80   24 (unknown/constant)
81   FUN fun1 function
82   FUN fun1 input A
83   FUN fun1 input B
84   25 (unknown/constant)
85   FUN fun2 function
86   FUN fun2 input A
87   FUN fun2 input B
88   20 (unknown/constant)
89   0 (unknown/constant)
90   LFO lfo1 rate ctl
91   LFO lfo1 min rate
92   LFO lfo1 max rate
93   LFO lfo1 phase
94   LFO lfo1 shape
95   0 (unknown/constant)
96   21 (unknown/constant)
97   0 (unknown/constant)
98   LFO lfo2 rate ctl
99   LFO lfo2 min rate
100   LFO lfo2 max rate
101   LFO lfo2 phase
102   LFO lfo2 shape
103   0 (unknown/constant)
104   26 (unknown/constant)
105   FUN fun3 function
106   FUN fun3 input A
107   FUN fun3 input B
108   27 (unknown/constant)
109   FUN fun4 function
110   FUN fun4 input A
111   FUN fun4 input B
112   32 (unknown/constant)
113   0
114   AMPENV user/natural
115   ENVCTL att adjust
116   ENVCTL att keytrak
117   ENVCTL att vel trk
118   ENVCTL att src
119   ENVCTL att depth
120   ENVCTL dec adjust
121   ENVCTL dec keytrak
122   ENVCTL dec src
123   ENVCTL dec depth
124   ENVCTL rel adjust
125   ENVCTL rel keytrak
126   ENVCTL rel src
127   ENVCTL rel depth
128   33 (unknown/constant)
129   AMPENV LOOP seg-X (2 parms)
130   AMPENV att1 %
131   AMPENV att1 time
132   AMPENV att2 %
133   AMPENV att2 time
134   AMPENV att3 %
135   AMPENV att3 time
136   AMPENV dec1 %
137   AMPENV dec1 time
138   AMPENV rel1 %
139   AMPENV rel1 time
140   AMPENV rel2 %
141   AMPENV rel2 time
142   0 (unknown/constant)
143   AMPENV rel3 time
144   34 (unknown/constant)
145   ENV2 LOOP seg-X (2 parms)
146   ENV2 att1 %
147   ENV2 att1 time
148   ENV2 att2 %
149   ENV2 att2 time
150   ENV2 att3 %
151   ENV2 att3 time
152   ENV2 dec1 %
153   ENV2 dec1 time
154   ENV2 rel1 %
155   ENV2 rel1 time
156   ENV2 rel2 %
157   ENV2 rel2 time  158   ENV2 rel3 %
159   ENV2 rel3 time
160   35 (unknown/constant)
161   ENV3 LOOP seg-X (2 parms)
162   ENV3 att1 %
163   ENV3 att1 time
164   ENV3 att2 %
165   ENV3 att2 time
166   ENV3 att3 %
167   ENV3 att3 time
168   ENV3 dec1 %
169   ENV3 dec1 time
170   ENV3 rel1 %
171   ENV3 rel1 time
172   ENV3 rel2 %
173   ENV3 rel2 time
174   ENV3 rel3 %
175   ENV3 rel 3 time
176   64 (unknown/constant)
177   127 (unknown/constant)
178   KEYMAP Transpose/Timbre shift (also 178 192 194)
179   0 (unknown/constant)
180   KEYMAP key track
181   KEYMAP vel track
182   0 (unknown/constant)
183   0 (unknown/constant)
184   KEYMAP2 keymap # MSB
185   KEYMAP2 keymap # LSB
186   0 (unknown/constant)
187   0 (unknown/constant)
188   0 (unknown/constant)
189   KEYMAP1 keymap #
190   0 (unknown/constant)
191   KEYMAP AltSwitch Ctl (Ctl-list)
192   KEYMAP timber shift            (also 178 192 194)
193   0 (PITCH ALG BLOCK)
194   PITCH coarse                   (also 178 192 194)
195   PITCH fine
196   PITCH key trk
197   PITCH vel trk
198   PITCH src1 (Ctl-list)
199   PITCH depth
200   PITCH depth ctl (Ctl-list)
201   PITCH min depth
202   PITCH max depth
203   PITCH src2 (Ctl-list)
204   0 (unknown/constant)
205   KEYMAP playback mode  (0=normal  2=reverse  4=bidirect
6=noise) 206   Algorithm #
207   PITCH fine hz
208   80 (unknown/constant)
209   ALG BLOCK
210   F1 coarse
211   F1 fine
212   F1 key track
213   F1 vel track
214   F1 src1
215   F1 depth
216   F1 depth ctl
217   F1 min depth
218   F1 max depth
219   F1 src2
220   F1 pad
221   0 (unknown/constant)
222   0 (unknown/constant)
223   F1 fine hz
224   81 (unknown/constant)
225   F2 ALG BLOCK
226   F2 coarse
227   F2 fine
228   F2 key track
229   F2 vel track
230   F2 src1
231   F2 depth
232   F2 depth ctl
233   F2 min depth
234   F2 max depth
235   F2 src2
236   F2 pad
237   0 (unknown/constant)  238   0 (unknown/constant)
239   F2  fine hz
240   82 (unknown/constant)
241   F3 ALG BLOCK
242   F3 coarse
243   F3 fine
244   F3 key track
245   F3 vel track
246   F3 src1
247   F3 depth
248   F3 depth ctl
249   F3 min depth
250   F3 max depth
251   F3 src2
252   F3 pad
253   0 (unknown/constant)
254   4
255   0
256   83 (unknown/constant)
257   F4 ALG BLOCK
258   F4AMP Adjust
259   KEYMAP2 OUTPUT PAN
260   F4AMP key track
261   F4AMP vel track
262   F4AMP src1
263   F4AMP depth
264   F4AMP depth ctl
265   F4AMP min depth
266   F4AMP max depth
267   F4AMP src2
268   F4AMP Pad
269   0 (unknown/constant)
270   * OUTPUT PAIR+GAIN
271   * KEYMAP1 OUTPUT PAN+MODE
272   9
```
== Bitmaps
```

offset 3PORT                           GLOBALATLEGATO
MONO                0x01    1 = ON   0 = OFF
PORTAMENTO          0x02    1 = ON   0 = OFF
GLOBALS             0x04    1 = ON   0 = OFF
ATTACK_PORTAMENTO   0x08    1 = OFF  0 = ON
LEGATO              0x10    1 = OFF  0 = ON

offset 56
IgnRel      0x01
SusPdl      0x02  1 = OFF  0 = ON
SosPdl      0x04  1 = OFF  0 = ON
FrzPdl      0x08  1 = OFF  0 = ON
ThrAtt      0x10
TilDec      0x20

offset 57
LAYER PitchBend_ALL  0x00   \
LAYER PitchBend_KEY  0x02    - BITS 0 and 1 - value 3 never happens
LAYER PitchBend_OFF  0x01   /
LAYER EnableS        0x04    1 = norm    0 = rvrs
LAYER Opaque         0x08    1 = ON      0 = OFF
OUTPUT xfade sense2  0x10    1 = reverse 0 = norm  (stereo keymaps)
KEYMAP Stereo        0x20    1 = ON      0 = OFF
OUTPUT xfade sense1  0x40    1 = rvrs    0 = norm

offset 58 VTRIG
X XXX X XXX
^ VTRIG1 level  (3 bits - see below)
^   VTRIG1 sense   0 = normal 1 = reverse
^       VTRIG2 level  (3 bits - see below)
^         VTRIG2 sense   0 = normal 1 = reverse

levels (bits)
ppp = 000
pp = 001
p = 010
mp = 011
mf = 100
f = 101
ff = 110
fff = 111EFFECTS VARIABLES
------------------------------------------------------
0   None                  48   Graphic8.0K
1   AccentDelay           49
2   AccentLevel           50
3   Band1Freq             51
4   Band1Level            52   Chorus L
5   Band2Freq             53
6   Band2Level            54   Chorus R
7   Band3Freq             55   Delay L
8   Band3Level            56   Delay R
9                         57   DryLevel
10   ChorusDelay           58
11   ChorusEQSource        59
12   ChrLFODepth           60
13   ChrLFOSpeed           61
14   HiFreqDamping         62   Gate R
15                         63   Reverb L
16   DlyEQSource           64   Reverb R
17   DlyFeedback           65   Reverse L
18   DelayIn:Chr           66   Reverse R
19   DelayIn:Dry           67   Tap1 L
20   DelayIn:EQ            68   Tap1 R
21   DelayIn:Fla           69   Tap2 L
22                         70
23                         71   Tap2 R
24                         72   Tap3 L
25                         73
26                         74   Tap3 R
27                         75   Tap4 L
28   DryLevel              76   Tap4 R
29   EarlyDelay            77   RevDecayTime
30   EarlyDiff             78   RoomVolume
31   EarlyLevel            79
32   Envelopment           80   RevPreDelay
33   FlangeDelay           81
34                         82
35   FlaFeedback           83   HiFrqDamping
36   FlaLFODepth           84   RevIn:Chr
37   FlaLFOSpeed           85   RevIn:Delay
38                         86   RevIn:Dry
39   GateEnvelope          87   RevIn:Fl
40   Graphic16K            88
41   Graphic63Hz           89
42   Graphic1.0K           90
43   Graphic125Hz          91   LaterDelay
44   Graphic2.0K           92   LaterLevel
45   Graphic250Hz          93   LaterDiff
46   Graphic4.0K           94   Gate L
47   Graphic500Hz
```
== Algorithm blocks
```
0   PITCH                 39   BAL    AMP
1   AMP                   40   PANNER
2   2POLE LOPASS          41   X GAIN
3   BANDPASS FILT         42   + GAIN
4   NOTCH FILTER          43   XFADE
5   2POLE ALLPASS         44   AMPMOD
6   unknown               45   unknown
7   unknown               46   unknown
8   PARA BASS             47   unknown
9   PARA TREBLE           48   X AMP
10   PARA BASS             49   + AMP
11   PARA TREBLE           50   4POLE LOPASS W/SEP
12   HIFREQ STIMULATOR     51   PARA MID
13   PARAMETRIC EQ         52   HIPAS2
14   STEEP RESONANT BASS   53   SW+DIST
15   LOPASS                54   4POLE HIPASS W/SEP
16   HIPASS                55   TWIN PEAKS BANDPASS
17   ALPASS                56   DOUBLE NOTCH W/SEP
18   GAIN                  57   LPGATE
19   SHAPER                58   unknown
20   DIST                  59   unknown
21   unknown               60   NONE (1 BLOCK)
22   PWM                   61   NONE (2 BLOCK)
23   SINE                  62   NONE (3 BLOCK)
24   LF SIN                63   unknown
25   SW+SHP                64   2PARAM SHAPER
26   SAW+                  65   unknown
27   SAW                   66   X SHAPEMOD OSC
28   LF SAW                67   + SHAPEMOD OSC
29   SQUARE                68   SHAPE MOD OSC
30   LF SQR                69   unknown
31   WRAP                  70   LPCLIP
32   unknown               71   SINE +
33   SYNC M                72   AMP MOD OSC
34   SYNC S                73   LP2RES
35   BAND2                 74   unused
36   NOTCH2                75   ! AMP
37   LOPAS2                76   NOISE +
38   AMP U  AMP L
```
== FUNS
```
0   None                  35   warp1(a,b)
1   a+b                   36   warp2(a,b)
2   a-b                   37   warp3(a,b)
3   (a+b)/2               38   warp4(a,b)
4   a/2+b                 39   warp8(a,b)
5   a/4+b/2               40   Not Found
6   (a+2b)/3              41   a AND b
7   Not Found             42   a OR b
8   Not Found             43   b>a
9   a*b                   44   Not Found
10   -a*b                  45   Not Found
11   Not Found             46   Not Found
12   Not Found             47   ramp(f=a+b)
13   a*10^b                48   ramp(f=a-b)
14   Not Found             49   ramp(f=(a+b)/2)
15   Not Found             50   ramp(f=a*b)
16   |a+b|                 51   ramp(f=-a*b)
17   |a-b|                 52   ramp(f=a*10^b)
18   min(a,b)              53   ramp(f=(a+b)/4)
19   max(a,b)              54   a(y+b)
20   Quantize B To A       55   ay+b
21   Not Found             56   (a+1)y+b
22   lowpass(f=a,b)        57   y+a(y+b)
23   hipass(f=a,b)         58   a|y|+b
24   Not Found             59   Not Found
25   b/(1-a)               60   Not Found
26   Not Found             61   Sample B On A
27   a(b-y)                62   Sample B On ~A
28   Not Found             63   Track B While A
29   (a+b)^2               64   diode(a-b)
30   Not Found             65   diode(a-b+.5)
31   sin(a+b)              66   diode(a-b-.5)
32   cos(a+b)              67   diode(a-b+.25
33   tri(a+b)              68   diode(a-b-.25)
34   Not Found             69   Track B While ~A
```
== Master Parameters
```
1,*MIDI RCV
2,MIDI RCV SCSI ID
3,MIDI RCV BasicChannel
4,MIDI RCV Velocity Map MSB
5,MIDI RCV VelocityMap LSB
6,MIDI RCV PressureMap MSB
7,MIDI RCV PressureMap LSB
8,
9,MASTER Intonation (affects 35 also)
10,MASTER IntonaKey [C=0:B=11]
11,MIDI RCV SysEx ID
12,MASTER Transpose
13,MASTER Tune
14,*MASTER bitmap
15,*MASTER bitmap
16,
17,MASTER Sample Time [scaled]
18,
19,
20,
21,
22,
23,
24,
25,*MASTER bitmap
26,
27,MIDI RCV LocalKbdCh
28,
29,
30,
31,
32,MASTER VelTouch MSB
33,MASTER VelTouch LSB
34,MASTER PressTouch MSB
35,MASTER PressTouch LSB
36,
37,MASTER Contrast
38,
39,MASTER Confirm: 0=Off 1=On
40,*MIDI RCV/XMT bitmap
41,
42,MIDI XMT VelocMap MSB
43,MIDI XMT VelocMap LSB
44,MIDI XMT PressMap MSB
45,MIDI XMT PressMap LSB
46,MASTER DrumChan (affects others)

78,*MIDI XMT bitmap
79,
80,MIDI XMT Press
81,MIDI XMT ModWhl
82,MIDI XMT CPedal
83,MIDI XMT Slider
84,MIDI XMT FtSw1
85,MIDI XMT FtSw2

91,CH_01 bitmap:  0x01 Enable: 0=On 1=Off  0x02 PrgLock: 0=Off 1=On
92,
93,
94,CH_01 Volume: 0x01-0x40 volume  0x80 VolLock: 0=Off 1=On
95,CH_01 Pan:    0x01-0x40 pan     0x80 PanLock: 0=Off 1=On
96,
97,
98,
99,
100,
101,CH_01 bitmap
102,
103,
104,
105,
106,

107 CH_02 bitmap

123 CH_03 bitmap

etc, etc.
```
== Bitmaps
```
1
0x01 \ MIDI Mode: 00       10=poly
0x02 /            01=omni  11=multi
0x04
0x08
0x10 AllNotesOff: 0=Normal 1=Ignore
0x20
0x40
0x80

14
0x01
0x02
0x04 Input: 0=Digital 1=Analog
0x08
0x10 Cable: 1=Optical 2=Coaxial
0x20 \ Rate: 00=29.4KHz  10=44.1KHz
0x40 /       01=32.0KHz  11=48.0KHz
0x80 Format: 0=SPDIF 1=AES/EBU

15
0x01
0x02
0x04
0x08
0x10 MIDI RCV BendSmooth: 0=On 1=Off
0x20 SAMPLE Mon: 0=Off 1=On
0x40
0x80

25
0x01
0x02
0x04 OutA->Mix: 0=Stereo 1=Mono
0x08 OutB->Mix: 0=Stereo 1=Mono
0x10 OutA->FX:  0=L+R 1=L Only
0x20
0x40 Sample Src: 0=Ext 1=Int
0x80 MIDI XMT V: 0=Off 1=On

40
0x01
0x02
0x04
0x08
0x10
0x20 \  ProgChgType: 000=Extended  011=QA Ext
0x40  >              001=Kurzweil  100=QA Kurz
0x80 /               010=0-127     101=QA 0-127

78
0x01 \  MIDI XMT Control: 00=Both 10=Local (affects 92, 93)
0x02 /                    01=MIDI
0x04 MIDI XMT PChng: 0=Off 1=On
0x08 MIDI XMT PBend: 0=Off 1=On
0x10
0x20
0x40
0x80

101
0x01 \   OutPair:  000=A(FX)   011=D(DRY)
0x02  >            001=B(DRY)  100=Prog
0x04 /             010=C(DRY)
0x08 \   OutGain:  0000=30dB   0011=12dB   0110=-6dB
0x10  \            0001=24dB   0100=6dB    0111=-12dB
0x20  /            0010=18dB   0101=0dB    1000=Prog
0x40 /
0x80

good luck
_
-john       ___                                    __/ |
___        |   |     JKrikawa@CCIT.Arizona.Edu    |    |___
________ \______/     \__________ Tucson, AZ ___../\./\/
\____/        \____
```

= Bill Simpson work (1995)
== Archive

Download his work from https://web.archive.org/web/19990422025731/http://ourworld.compuserve.com/homepages/k2000/sxtbl.zip

== Internal Codes and Parameter Values

```                                                                    
    Table Name=AB                                                   
                                                                    
    Internal    Parameter                                           
      Code        Value                                             
                                                                    
       0            A                                               
       1            B                                               
                                                                    
                                                                    
    Table Name=AMPENV                                               
                                                                    
    Internal    Parameter                                           
      Code        Value                                             
                                                                    
       0         User                                               
       1         Natural                                            
                                                                    
                                                                    
    Table Name=ASRMODE                                              
                                                                    
    Internal    Parameter                                           
      Code        Value                                             
                                                                    
       0         Normal                                             
       1         Hold                                               
       2         Repeat                                             
                                                                    
                                                                    
      Table Name=KEYMAP                                               
                                                                    
    Internal                                                        
      Code     Parameter Value
                                                                    
       0       None
       1       Grand Piano
       2       Dual Elec Piano
       3       Hard Elec Piano
       4       Soft Elec Piano
       5       Voices
       6       Ensemble Strings
       7       Elec Jazz Guitar
       8       Acoustic Guitar
       9       5 String Guitar
      10       Dual E Bass
      11       Elec Pick Bass
      12       Elec Slap Bass
      13       Finger Atk Bass
      14       Flute
      15       Tenor Saxophone
      16       Sax no Altissimo
      17       Trumpet
      18       Trombone
      19       Trombone/Trumpet
      20       Preview Kit
      21       5 Octave Dry Kit 1
      22       5 Octave Dry Kit 2
      23       5 Octave Dry Kit 3
      24       5 Octave Dry Kit 4
      25       5 Octave Amb Kit 1
      26       5 Octave Amb Kit 2
      27       5 Octave Amb Kit 3
      28       5 Octave Amb Kit 4
      29       5 Octave Amb Kit 5
      30       5 Octave Amb Kit 6
      31       5 Octave Amb Kit 7
      32       5 Octave Amb Kit 8
      33       5 Octave Amb Kit 9
      34       5 Octave Amb Kit 10
      35       5 Octave Amb Kit 11
      36       5 Octave Amb Kit 12
      37       2 Octave Dry Kit 1
      38       General MIDI Kit
      39       Ride Rim Cymbal             

       ...                              
```

= Tools

== KurzFiler
One of the best. Open source. Java compiled with ...make.

#figure(
  image("assets/KurzFiler.png", width: 50%),
  caption: [KurzFiler],
) <figure>

#table(
  columns: 2,
  align: left + horizon,  
[Status],
[Archived],
[URL],
[#text(fill: rgb("#bbbebf"))[#underline[https://kurzfiler.sourceforge.io/]]],
)

== VAST programmer

VP is a Java program developed by Michael Warpenburg that allows users to interactively develop Setups, Programs & other types of objects on Kurzweil VAST machines. Users interact with the machine(s) in real-time using VP, and as such can also use it as a general control user interface in a performance setting.


#figure(
  image("assets/VASTprogrammer.png", width: 50%),
  caption: [VAST programmer],
) <figure>

#table(
  columns: 2,
  align: left + horizon,  
[Status],
[Dead],
[URL],
[#text(fill: rgb("#bbbebf"))[#underline[https://web.archive.org/web/20160506220003if_/http://www.tangentcats.com/]]],
)
== Analysis
This software was written in Vijeo Designer. A Schneider Electric IDE based on Java which is still available. Since it relies only on LCD Screenshots via MIDI, it was not really insightfull. It is also closed source.

== wav2krz
Python cli to collect WAV files into Kurzweil soundfile format (.krz, .k25, .k26, and .for) with automatic keymap and program creation.

#v(20pt)

#table(
  columns: 2,
  align: left + horizon,  
[Status],
[Alive],
[URL],
[#text(fill: rgb("#bbbebf"))[#underline[https://github.com/xrisw-aeae/wav2krz]]],
)

== K2x00 Remote
K2x00 Remote is a remote control application and plugin to replicate the screen of Kurzweil K-series instruments, including K2000, K2500, K2600, K2661 and KSP8. This software is under active development by Sjoerd W. Bijleveld and Godlike Productions

#figure(
  image("assets/K2000Remote.png", width: 50%),
  caption: [K2x00 Remote],
) <figure>

#block(breakable: false)[
#table(
  columns: 2,
  align: left + horizon,  
[Status],
[Alive],
[URL],
[#text(fill: rgb("#bbbebf"))[#underline[https://godlike.com.au/index.php?id=358]]],
)
]

== KCDRead
The Kurzweil samplers of the K2x00 series are on the market for a long time now, proving that quality persists.

As time has gone by, the hardware environment changed. Hard disks became larger and CD Roms faster. Unfortunately, the K doesn't really know how to work with new drives.

This Project is intended for solving this gap. The Kurzweil's filesystem is quite near to the old MSDOS FAT16 standard. It's not too hard to decode it.
#v(20pt)
#table(
  columns: 2,
  align: left + horizon,  
[Status],
[Archived],
[URL],
[#text(fill: rgb("#bbbebf"))[#underline[https://kcdread.sourceforge.net/]]],
)

== Kurzweil Creator
Kurzweil Creator™ is a powerful Program, Setup, Keymap, and Sample creator/editor for Kurzweil workstations, including the K2000, K2500, K2600, K2661, PC3K, and Forte.
#figure(
  image("assets/KurzweilCreator.png", width: 50%),
  caption: [Kurzweil Creator],
) <figure>
#table(
  columns: 2,
  align: left + horizon,  
[Status],
[Alive],
[URL],
[#text(fill: rgb("#bbbebf"))[#underline[https://www.chickensys.com/support/software/kurzweilcreator/documentation/]]],
)
= Sites

== K1000 K1200 Users Group

#table(
  columns: 2,
  align: left + horizon,  
[Status],
[Archived],
[URL],
[#text(fill: rgb("#bbbebf"))[#underline[https://www.k1000.net/]]],
)

== Espace Kurzweil (fr)

#table(
  columns: 2,
  align: left + horizon,  
[Status],
[Archived],
[URL],
[#text(fill: rgb("#bbbebf"))[#underline[http://espacekurzweil.free.fr/utilitaires/index.html]]],
)

== K2500 OS hack

Peter Sobot is that kind of guy who try to run a K2500 OS inside... MAME.

#table(
  columns: 2,
  align: left + horizon,  
[Status],
[Archived],
[URL],
[#text(fill: rgb("#bbbebf"))[#underline[https://petersobot.com/blog/patching-the-k2500/]]],
)
