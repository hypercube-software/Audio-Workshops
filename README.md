# Audio/MIDI Workshops

Topics covered:

- How to parse various audio file formats.
- How to build a simple synthesizer in Java.
- How to make a native tool to convert an entire MIDI synthesizer into SFZ presets
- How to parse proprietary SysEx MIDI messages, especially the Roland ones
- How to build a MIDI Editor for any Synthesizer using [Quasar](https://quasar.dev/)/[VueJS](https://vuejs.org/) and [SpringBoot 3](https://spring.io/projects/spring-boot)

| ![build](https://github.com/hypercube-software/Audio-Workshops/workflows/Documentation%20build/badge.svg) | ![build](https://github.com/hypercube-software/Audio-Workshops/workflows/Maven%20build/badge.svg) |
|-----------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------|

⚠️ **This project is in constant "work in progress"**

# Status

## MIDI

- Midi Sequencer with various clock implementations
- Various Memory maps are provided: **Boss DS-330** (= Sound Canvas), **Roland D-70**
- SysEx are not finished yet: various devices need to be analyzed, especially **AKAI MPK-261**

## AUDIO

Record and Play audio from/to WAV files.

Support audio channel assignation

Supported PCM convertions

|                             | From [0,1] float | To [0,1] float |
| --------------------------- | ---------------- | -------------- |
| Signed 32 bit Little Endian |                  | ✔️ |
| Signed 24 bit Little Endian | ✔️ | ✔️ |
| Signed 16 bit Little Endian | ✔️ | ✔️ |
| Signed 8 bit Little Endian  | ✔️ | ✔️ |
| Signed 32 bit Big Endian    |                  | ✔️ |
| Signed 32 bit Big Endian    |                  | ✔️ |
| Signed 24 bit Big Endian    |                  | ✔️ |
| Signed 16 bit Big Endian    |                  | ✔️ |
| Signed 8 bit Big Endian     |                  | ✔️ |
| Unsigned 32 bit Little Endian |                  | ✔️ |
| Unsigned 24 bit Little Endian |                  | ✔️ |
| Unsigned 16 bit Little Endian |                  | ✔️ |
| Unsigned 8 bit Little Endian  |                  | ✔️ |
| Unsigned 32 bit Big Endian    |                  | ✔️ |
| Unsigned 32 bit Big Endian    |                  | ✔️ |
| Unsigned 24 bit Big Endian    |                  | ✔️ |
| Unsigned 16 bit Big Endian    |                  | ✔️ |
| Unsigned 8 bit Big Endian     |                  | ✔️ |

Supported file formats:

| Format     | PARSER | READ | WRITE | Comment                                           |
| ---------- | ------ | ---- | ----- | ------------------------------------------------- |
| WAV        | ✔️      | ✔️    | ✔️     | Parse many proprietary chunks                     |
| ACID       | ✔️      | ✔️    |       |                                                   |
| AIFF       | ✔️      | ✔️    |       |                                                   |
| AIFC       | ✔️      |      |       | Compressed AIFF                                   |
| FLAC       | ✔️      |      |       | With metadata like Vorbis comment and ID3 picture |
| ID3        | ✔️      |      |       | In RIFF, Text frames only                         |
| Gigastudio | ✔️      | ✔️    |       | Is an extension of DSL2                           |
| DSL2       | ✔️      | ✔️    |       |                                                   |



## Audio synth

- Filters are not implemented yet
- ADSR envelopes are still in early stages.
- The documentation does not cover all the code yet.

Anyway, the VCOs are working and they respond to MIDI. The java synth is working.

## Synth Editor

This tool allow you to edit your synthesizer (typically Roland Sound Canvas) using SysEx MIDI messages

![image-20240407173158050](assets/image-20240407173158050.png)

- It is in early stage
- The frontend ([Quasar](https://quasar.dev/)+[VueJs](https://vuejs.org/)) communicate with backend with **WebSocket** and **REST**. 
- Components can update the hardware memory in real-time

## Synth Ripper

This little CLI will help you to convert any MIDI synthesizer to SFZ presets which can be played in any sampler supporting this [open format](https://sfzformat.com/). For instance, it works nicely with [TX16Wx](https://www.tx16wx.com/).

- Everything is automatised: program changes, note changes, velocities
- See YAML configuration files as example
- ⚠️It is not finished yet, need loop detection and trim start and end samples

This tool requires 3 device interfaces:

- It send MIDI notes to a MIDI OUT device, towards your synthetizer
- It record samples from an AUDIO IN device, from your synthetizer
- it monitor the record to an AUDIO OUT device, in order to ear what's going on

You can compile it into **native executable** using [GraalVM](https://www.graalvm.org/) and [Visual studio build tools 2022](https://learn.microsoft.com/en-us/cpp/build/building-on-the-command-line?view=msvc-170#download-and-install-the-tools)

- `build-analysis.cmd`: run the tool in order to prepare the native compilation
- `build-native.cmd`: run the compilation of `synth-ripper.exe`

Usage:

```
>synth-ripper.exe info
 _______ __   __ __   _ _______ _     _       ______ _____  _____   _____  _______  ______
 |______   \_/   | \  |    |    |_____|      |_____/   |   |_____] |_____] |______ |_____/
 ______|    |    |  \_|    |    |     |      |    \_ __|__ |       |       |______ |    \_

synth-ripper 1.0.0
Powered by Spring Boot 3.2.4
Available devices:
MIDI OUT : Microsoft MIDI Mapper
MIDI OUT : Microsoft GS Wavetable Synth
MIDI OUT : ToDAW
MIDI OUT : FromDAW
MIDI OUT : MidiClock
MIDI OUT : 4- MIDISPORT Uno Out
AUDIO IN : Pilote de capture audio principal
AUDIO IN : Microphone (Realtek High Definition Audio)
AUDIO IN : VoiceMeeter Output (VB-Audio VoiceMeeter VAIO)
AUDIO IN : VoiceMeeter Aux Output (VB-Audio VoiceMeeter AUX VAIO)
AUDIO IN : Mixage stéréo (Realtek High Definition Audio)
AUDIO IN : VoiceMeeter VAIO3 Output (VB-Audio VoiceMeeter VAIO3)
AUDIO OUT: Périphérique audio principal
AUDIO OUT: Haut-parleurs (Realtek High Definition Audio)
AUDIO OUT: VoiceMeeter Input (VB-Audio VoiceMeeter VAIO)
AUDIO OUT: VoiceMeeter VAIO3 Input (VB-Audio VoiceMeeter VAIO3)
AUDIO OUT: VoiceMeeter Aux Input (VB-Audio VoiceMeeter AUX VAIO)
AUDIO OUT: Realtek Digital Output (Realtek High Definition Audio)
```

Then edit your YAML configuration file to use the right devices: `config/config.yml`

```yaml
devices:
  inputAudioDevice: "Microphone (Realtek High Definition Audio)"
  outputAudioDevice: "Périphérique audio principal"
  outputMidiDevice: "4- MIDISPORT Uno Out"
```

Then start the rip:

```
>synth-ripper.exe rip -c config/config.yml
```

SFZ presets will be generated in `output`folder

# Audience

Most workshops are made for Java developers knowing zero about MIDI and Audio, except the **Synth Editor** and **Synth Ripper** which is more advanced.

We are targeting Windows OS but things should works on OSX in the same way.

# Documentation

Go to the [website](https://hypercube-software.github.io/Audio-Workshops).

