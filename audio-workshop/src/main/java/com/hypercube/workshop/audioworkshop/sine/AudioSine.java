package com.hypercube.workshop.audioworkshop.sine;

import com.hypercube.workshop.audioworkshop.common.AudioOutputDevice;
import com.hypercube.workshop.audioworkshop.common.AudioOutputLine;
import com.hypercube.workshop.audioworkshop.common.vca.AdsrVCA;
import com.hypercube.workshop.audioworkshop.common.vca.SimpleVCA;
import com.hypercube.workshop.audioworkshop.common.vca.VCA;
import com.hypercube.workshop.audioworkshop.common.vco.NaiveVCO;
import com.hypercube.workshop.audioworkshop.common.vco.VCO;
import com.hypercube.workshop.audioworkshop.common.vco.WavetableType;
import com.hypercube.workshop.audioworkshop.common.vco.WavetableVCO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteOrder;
import java.util.List;


@Slf4j
@Component
public class AudioSine {
    static final int SAMPLE_RATE = 44100;


    void playSine(AudioOutputDevice audioOutputDevice) {
        final AudioFormat af = new AudioFormat(SAMPLE_RATE, 16, 1, true, true);
        try {
            VCA vca = new SimpleVCA(SAMPLE_RATE, 10);
            VCO vco = new NaiveVCO(100, SAMPLE_RATE, 16, ByteOrder.BIG_ENDIAN, vca);
            //VCO vco = new CorrectVCO(100, SAMPLE_RATE, 16, ByteOrder.BIG_ENDIAN, vca);
            //VCO vco = new WavetableVCO(WavetableType.SINE, 100, SAMPLE_RATE, 16, ByteOrder.BIG_ENDIAN, vca);
            final double freq = VCO.midiNoteToFrequency(48);
            byte[] data = vco.generateSignal(freq); // Play C3
            log.info(String.format("Play note at %f Hz", freq));
            audioOutputDevice.play(af, data, 2000);
            log.info("Done");
        } catch (LineUnavailableException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    void playFile(AudioOutputDevice audioOutputDevice, File file, int loops) {
        try {
            audioOutputDevice.play(file, loops);
            log.info("Done");
        } catch (LineUnavailableException | InterruptedException | UnsupportedAudioFileException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    void vco(AudioOutputDevice audioOutputDevice) {
        try {
            Thread.currentThread()
                    .setPriority(Thread.MAX_PRIORITY);
            try (AudioOutputLine line = new AudioOutputLine(audioOutputDevice, SAMPLE_RATE, 16, 100)) {
                line.start();

                // use our broken VCO, then try width CorrectVCO
                VCA vca = new SimpleVCA(SAMPLE_RATE, 10);
                VCO vco = new NaiveVCO(100, SAMPLE_RATE, 16, ByteOrder.BIG_ENDIAN, vca);

                // You should ear something wrong especially for the last note
                List.of(52, 54, 56, 57, 74)
                        .stream()
                        .map(VCO::midiNoteToFrequency)
                        .forEach(freq -> {
                            log.info("Play frequency " + freq + " Hz");
                            long start = System.currentTimeMillis();
                            while (System.currentTimeMillis() - start < 4000) {
                                byte[] data = vco.generateSignal(freq);
                                line.sendBuffer(data);
                            }
                        });
            }
            log.info("Done");
        } catch (LineUnavailableException e) {
            throw new RuntimeException(e);
        }
    }

    void generateFile(int midiNote, File file) {
        try (OutputStream out = new FileOutputStream(file)) {
            VCA vca = new AdsrVCA(SAMPLE_RATE, 500, 250, 500);
            VCO vco = new WavetableVCO(WavetableType.SINE, 100, SAMPLE_RATE, 16, ByteOrder.BIG_ENDIAN, vca);
            double freq = VCO.midiNoteToFrequency(midiNote);
            log.info("Play frequency " + freq + " Hz");
            for (int i = 0; i < 10; i++) {
                byte[] data = vco.generateSignal(freq);
                out.write(data);
            }
            vca.onNoteOn(1);
            for (int i = 0; i < 100; i++) {
                byte[] data = vco.generateSignal(freq);
                out.write(data);
            }
            vca.onNoteOff();
            for (int i = 0; i < 2000; i++) {
                byte[] data = vco.generateSignal(freq);
                out.write(data);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
