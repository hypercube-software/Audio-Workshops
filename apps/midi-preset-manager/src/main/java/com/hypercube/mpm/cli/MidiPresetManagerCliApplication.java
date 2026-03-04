package com.hypercube.mpm.cli;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import java.util.Locale;

@SpringBootApplication
@ComponentScan(basePackages = {"com.hypercube.mpm.cli", "com.hypercube.mpm.udp", "com.hypercube.mpm.config", "com.hypercube.workshop.midiworkshop.api"})
public class MidiPresetManagerCliApplication {

    public static void main(String[] args) {
        Locale.setDefault(new Locale("en"));
        try {
            SpringApplication.run(MidiPresetManagerCliApplication.class, args);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

}
