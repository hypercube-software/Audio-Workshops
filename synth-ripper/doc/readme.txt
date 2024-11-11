This tool allow you to capture various presets from hardware synth to sampler formats (DecentSampler or SFZ).
how to run:

Windows:
synth-ripper.exe -c config/<your config file>.yml

OSX:
synth-ripper -c config/<your config file>.yml

JVM:
java -jar synth-ripper.jar -c config/<your config file>.yml

NOTE for windows 11 users: if you can't record, you need to add this in your registry:

Windows Registry Editor Version 5.00

[HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Windows\CurrentVersion\CapabilityAccessManager\ConsentStore\microphone]
"Value"="Allow"

