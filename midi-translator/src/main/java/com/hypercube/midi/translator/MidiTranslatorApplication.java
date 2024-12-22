package com.hypercube.midi.translator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Optional;

@SpringBootApplication
@Slf4j
public class MidiTranslatorApplication {

    public static void main(String[] args) {
        if (args.length == 0) {
            args = new String[]{"help"};
        }
        try {
            SpringApplication.run(MidiTranslatorApplication.class, args);
        } catch (Exception e) {
            Throwable ourException = resolveException(e);
            if (ourException != null && ourException.getClass()
                    .getPackageName()
                    .startsWith("com.hypercube")) {
                log.error("Unexpected error", Optional.ofNullable(ourException)
                        .orElse(e));
            } else {
                throw e; // Spring AOP internally catches exceptions to obtain the application context
            }
        }
    }

    /**
     * Try to get our exception instead of throwing a giant stack to the user
     */
    private static Throwable resolveException(Exception e) {
        Throwable ourException = null;
        Throwable initalException = e;
        while (initalException != null) {
            if (initalException.getClass()
                    .getPackageName()
                    .startsWith("com.hypercube")) {
                ourException = initalException;
            }
            initalException = initalException.getCause();
        }
        return ourException;
    }

}
