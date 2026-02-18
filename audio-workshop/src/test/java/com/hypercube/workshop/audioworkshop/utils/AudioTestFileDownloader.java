package com.hypercube.workshop.audioworkshop.utils;

import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.*;
import java.io.*;
import java.net.URI;
import java.net.URLConnection;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
public class AudioTestFileDownloader {
    public void downloadSound(String url, File outputfolder) {

        try {
            outputfolder.mkdirs();
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
            destfile.toFile()
                    .delete();
            if (!destfile.toFile()
                    .exists()) {
                log.info("Download test file %s ...".formatted(url));
                download(url, destfile.toString());
                if (destfile.toString()
                        .endsWith(".zip")) {
                    unzip(destfile.toFile());
                }
            }


        } catch (IOException e) {
            log.error("Unable to download {}", url, e);
        }

    }

    private void trustAnyCertificate() {
        // 1. BYPASS SSL: Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
        };


        try {// Install the all-trusting trust manager
            SSLContext sc = SSLContext.getInstance("SSL");

            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            // Bypass hostname verification (e.g., if the cert is for a different domain)
            HostnameVerifier allHostsValid = (hostname, session) -> true;
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException(e);
        }
    }

    private void download(String url, String file) throws IOException {
        trustAnyCertificate();
        URLConnection connection = URI.create(url)
                .toURL()
                .openConnection();

        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/145.0.0.0 Safari/537.36");
        connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");

        try (InputStream inputStream = connection.getInputStream();
             FileOutputStream outputStream = new FileOutputStream(file)) {

            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
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
