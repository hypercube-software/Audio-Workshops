package com.hypercube.midi.translator;

import com.fasterxml.jackson.core.JacksonException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Slf4j
public class MidiBackupTranslatorApplication {

    public static void main(String[] args) {
        if (args.length == 0) {
            args = new String[]{"help"};
        }
        try {
            SpringApplication.run(MidiBackupTranslatorApplication.class, args);
        } catch (Exception e) {
            Throwable ourException = resolveException(e);
            if (ourException != null && ourException.getClass()
                    .getPackageName()
                    .startsWith("com.hypercube")) {
                log.error("Unexpected error: " + ourException.getMessage());
                if (e.getCause() instanceof JacksonException) {
                    log.error("You made a mistake in your YAML: " + e.getCause()
                            .getMessage());
                }
            } else {
                log.error("A technical error occurred, see logs for details: " + e.getMessage());
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
