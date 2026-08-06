package com.hypercube.workshop.midiworkshop.presets;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

@Slf4j
public abstract class AbstractPredefinedPatchesTest {
    protected void saveText(List<String> lines, String path) throws IOException {
        var p = Path.of("./target/patches/" + path);
        File file = p.toFile();
        file
                .getParentFile()
                .mkdirs();
        if (file.exists()) {
            file.delete();
        }
        Files.write(p, lines, StandardOpenOption.CREATE);
    }


}
