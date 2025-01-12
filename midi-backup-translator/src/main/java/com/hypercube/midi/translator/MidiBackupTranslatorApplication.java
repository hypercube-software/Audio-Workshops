package com.hypercube.midi.translator;

import com.hypercube.workshop.midiworkshop.common.errors.ErrorHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("com.hypercube")
@Slf4j
public class MidiBackupTranslatorApplication {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            args = new String[]{"help"};
        }
        try {
            SpringApplication.run(MidiBackupTranslatorApplication.class, args);
        } catch (Exception e) {
            ErrorHandler.catchException(e);
        }
    }


}
