package com.hypercube.workshop.midiworkshop.common.config;

import com.hypercube.workshop.midiworkshop.common.errors.MidiConfigError;
import lombok.experimental.UtilityClass;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

@UtilityClass
public class ConfigHelper {
    /**
     * This method give us the location of the CLI no matter what is the current directory
     *
     * @return
     */
    public File getApplicationFolder(Class<?> applicationClass) {
        try {
            URI uri = applicationClass
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI();
            String scheme = uri.getScheme();
            if (scheme
                    .equals("file")) {
                String path = uri.getPath();
                File f = new File(path);
                if (path.endsWith("/target/classes/")) {
                    // The application run in inside an IDE
                    f = f.getParentFile();
                } else {
                    // The application run as native EXE
                }
                return f.getParentFile();
            } else if (scheme.equals("jar")) {
                // The application run in command line as an executable JAR
                String path = uri.getRawSchemeSpecificPart()
                        .replace("nested:", "")
                        .replaceAll("\\.jar.*", "");
                File f = new File(path);
                if (path.contains("/target/")) {
                    // The application run in debug inside an IDE
                    return f.getParentFile()
                            .getParentFile();
                } else {
                    return f.getParentFile();
                }
            }
            throw new MidiConfigError("Unexpected location: " + uri.toString());
        } catch (URISyntaxException e) {
            throw new MidiConfigError(e);
        }
    }
}
