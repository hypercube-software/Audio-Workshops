package com.hypercube.workshop.syntheditor;

import com.hypercube.workshop.syntheditor.model.error.SynthEditorException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.util.stream.Stream;

@SpringBootApplication
@Slf4j
@RequiredArgsConstructor
public class SynthEditorApplication {
    private final ServletWebServerApplicationContext webServerApplicationContext;
    private final Environment environment;

    private Process execute(String... params) {
        try {
            return Runtime.getRuntime()
                    .exec(params);
        } catch (IOException e) {
            throw new SynthEditorException(e);
        }
    }

    public void openBrowser(String url) {
        String osName = System.getProperty("os.name");
        try {
            if (osName.startsWith("Mac OS")) {
                execute("open", "-a", "Safari", url);
            } else if (osName.startsWith("Windows"))
                execute("rundll32", "url.dll,FileProtocolHandler", url);
            else { //assume Unix or Linux
                Stream.of("google-chrome", "firefox", "opera", "konqueror", "epiphany", "mozilla", "netscape")
                        .filter(this::browserExists)
                        .findFirst()
                        .ifPresentOrElse(browser -> execute(browser, url), () -> {
                            throw new SynthEditorException("Could not find web browser");
                        });
            }
        } catch (Exception e) {
            log.error("Unable to Open browser on {}", url, e);
        }
    }

    private boolean browserExists(String browser) {
        try {
            return execute("which", browser)
                    .waitFor() == 0;
        } catch (InterruptedException e) {
            throw new SynthEditorException(e);
        }
    }

    @EventListener({ApplicationReadyEvent.class})
    void applicationReadyEvent() {
        if (environment.getActiveProfiles().length == 0) {
            openBrowser("http://localhost:%d/".formatted(webServerApplicationContext.getWebServer()
                    .getPort()));
        }
    }

    public static void main(String[] args) {

        SpringApplication.run(SynthEditorApplication.class, args);
    }

}
