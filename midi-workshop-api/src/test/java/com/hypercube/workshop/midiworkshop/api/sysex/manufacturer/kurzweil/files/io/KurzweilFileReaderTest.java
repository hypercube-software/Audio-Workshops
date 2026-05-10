package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.io;

import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.KurzweilFile;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

class KurzweilFileReaderTest {

    @Test
    void readSoundBlock() {
        File file = new File("./src/test/resources/Kurzweil/SIMPLE.K26");
        try (KurzweilFileReader kurzweilFileReader = new KurzweilFileReader(file)) {
            KurzweilFile kurzweilFile = kurzweilFileReader.read();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @Disabled
    void read() {
        String pathname = "./src/test/resources/Kurzweil/anapads.krz";
        //String pathname = "D:\\github-checkout\\Audio-Workshops\\Manuals\\Kurzweil\\Solina Phase 2.K26";
        File file = new File(pathname);
        try (KurzweilFileReader kurzweilFileReader = new KurzweilFileReader(file)) {
            KurzweilFile kurzweilFile = kurzweilFileReader.read();
            KurzweilFileConverter kurzweilFileConverter = new KurzweilFileConverter();
            kurzweilFileConverter.toSysEx(kurzweilFile, new File("./target/output"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
