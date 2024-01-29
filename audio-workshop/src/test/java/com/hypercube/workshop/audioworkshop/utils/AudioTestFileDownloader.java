package com.hypercube.workshop.audioworkshop.utils;

import com.hypercube.workshop.midiworkshop.common.errors.MidiError;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
public class AudioTestFileDownloader {
    public void downloadSound(String url, File outputfolder) {

        InputStream in = null;
        try {
            outputfolder.mkdirs();
            in = new URL(url).openStream();
            String filename;
            int idx = url.lastIndexOf('/');
            if (idx != -1) {
                filename = url.substring(idx + 1);
            } else {
                return;
            }
            filename = cheapUrlDecode(filename);
            filename = filename.replace("/", "-");
            Path destfile = Paths.get("%s/%s".formatted(outputfolder, filename));
            if (!destfile.toFile()
                    .exists()) {
                log.info("Download test file %s ...".formatted(url));
                Files.copy(in, destfile, StandardCopyOption.REPLACE_EXISTING);
                if (destfile.toString()
                        .endsWith(".zip")) {
                    unzip(destfile.toFile());
                }
            }


        } catch (IOException e) {
            throw new MidiError(e);
        }

    }

    private String cheapUrlDecode(String str) {
        for (int i = 32; i < 127; i++) {
            str = str.replace("%%%2X".formatted(i), "%c".formatted((char) i));
        }
        return str;
    }

    private void unzip(File destfile) throws IOException {
        byte[] buffer = new byte[1024];
        File outputFolder = destfile.getParentFile();
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(destfile))) {
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                unzipEntry(outputFolder, buffer, zis, zipEntry);
                zipEntry = zis.getNextEntry();
            }

            zis.closeEntry();
        }
    }

    private void unzipEntry(File outputFolder, byte[] buffer, ZipInputStream zis, ZipEntry zipEntry) throws IOException {
        File newFile = newFile(outputFolder, zipEntry);
        if (zipEntry.isDirectory()) {
            if (!newFile.isDirectory() && !newFile.mkdirs()) {
                throw new IOException("Failed to create directory " + newFile);
            }
        } else {
            // fix for Windows-created archives
            File parent = newFile.getParentFile();
            if (!parent.isDirectory() && !parent.mkdirs()) {
                throw new IOException("Failed to create directory " + parent);
            }

            // write file content
            FileOutputStream fos = new FileOutputStream(newFile);
            int len;
            while ((len = zis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
            fos.close();
        }
    }

    private File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;
    }
}
