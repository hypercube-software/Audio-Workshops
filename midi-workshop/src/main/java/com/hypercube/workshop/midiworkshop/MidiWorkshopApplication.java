package com.hypercube.workshop.midiworkshop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MidiWorkshopApplication {
    public static void main(String[] args) {
        if (args.length == 0) {
            args = new String[]{"help"};
        }
        SpringApplication.run(MidiWorkshopApplication.class, args);
    }
}
