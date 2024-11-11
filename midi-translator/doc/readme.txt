This little tool allow you to convert any CC to SYSEX.
It is able to limit the bandwidth, so, no more MIDI buffer errors on synth like the Yamaha TX81z
If messages are coming too fast, they are simply droped. Nevertheless, the last one is retained.
how to run:

Windows:
midi-translator.exe -c config/<your config file>.yml

OSX:
midi-translator -c config/<your config file>.yml

JVM:
java -jar midi-translator.jar -c config/<your config file>.yml
