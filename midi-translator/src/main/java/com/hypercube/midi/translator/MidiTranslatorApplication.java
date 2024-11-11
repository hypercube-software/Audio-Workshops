package com.hypercube.midi.translator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MidiTranslatorApplication {

    public static void main(String[] args) {
        if (args.length == 0) {
            args = new String[]{"help"};
        }
        SpringApplication.run(MidiTranslatorApplication.class, args);
    }

}
