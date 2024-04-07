# Audio/MIDI Workshops

Topics covered:

- How to parse various audio file formats.
- How to build a simple synthesizer in Java.
- How to parse proprietary SysEx MIDI messages, especially the Roland ones
- How to build a MIDI Editor for any Synthesizer using [Quasar](https://quasar.dev/)/[VueJS](https://vuejs.org/) and [SpringBoot 3](https://spring.io/projects/spring-boot)

| ![build](https://github.com/hypercube-software/Audio-Workshops/workflows/Documentation%20build/badge.svg) | ![build](https://github.com/hypercube-software/Audio-Workshops/workflows/Maven%20build/badge.svg) |
|-----------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------|

⚠️ **This project is in constant "work in progress"**:

# Status

## MIDI

- Midi Sequencer with various clock implementations
- Various Memory maps are provided: **Boss DS-330** (= Sound Canvas), **Roland D-70**
- SysEx are not finished yet: various devices need to be analyzed, especially **AKAI MPK-261**

## Audio synth

- Filters are not implemented yet
- ADSR envelopes are still in early stages.
- The documentation does not cover all the code yet.

Anyway, the VCOs are working and they respond to MIDI. The java synth is working.

## Synth Editor

![image-20240407173158050](assets/image-20240407173158050.png)

- It is in early stage
- The frontend ([Quasar](https://quasar.dev/)+[VueJs](https://vuejs.org/)) communicate with backend with **WebSocket** and **REST**. 
- Components can update the hardware memory in real-time

# Audience

Most workshops are made for Java developers knowing zero about MIDI and Audio, except the Synth Editor which is more advanced.

We are targeting Windows OS but things should works on OSX in the same way.

# Documentation

Go to the [website](https://hypercube-software.github.io/Audio-Workshops).

