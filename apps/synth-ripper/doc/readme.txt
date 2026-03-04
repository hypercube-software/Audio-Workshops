This tool allow you to capture various presets from hardware synth to sampler formats (DecentSampler or SFZ).
how to run:

Windows:
synth-ripper.exe rip -c config/<your config file>.yml

OSX:
synth-ripper rip -c config/<your config file>.yml

JVM:
java -jar synth-ripper.jar rip -c config/<your config file>.yml

you can also list I/O devices with:

synth-ripper.exe info

NOTE for windows 11 users:
--------------------------

if you can't record, you need to add this in .REG to your registry:

Windows Registry Editor Version 5.00

[HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Windows\CurrentVersion\CapabilityAccessManager\ConsentStore\microphone]
"Value"="Allow"

