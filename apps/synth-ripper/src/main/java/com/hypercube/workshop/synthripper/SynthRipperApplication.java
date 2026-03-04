package com.hypercube.workshop.synthripper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SynthRipperApplication {

    public static void main(String[] args) {
        if (args.length == 0) {
            args = new String[]{"help"};
        }
        SpringApplication.run(SynthRipperApplication.class, args);
    }

}
