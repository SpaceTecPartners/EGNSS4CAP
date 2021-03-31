package eu.foxcom.gtphotos.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Digest {

    private static final char[] HEX = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
    private static final int BUFFER_SIZE = 4000;
    private static final String HASH = "SHA-256";

    private static String hashBytesToHexString(byte[] bytes) {
        final int nBytes = bytes.length;
        char[] result = new char[2 * nBytes];
        int j = 0;
        for (byte aByte : bytes) {
            result[j++] = HEX[(0xF0 & aByte) >>> 4];
            result[j++] = HEX[(0x0F & aByte)];
        }
        return new String(result);
    }

    public static String hashStringToHexString(String string) throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance(HASH);
        byte[] hash = messageDigest.digest(string.getBytes(StandardCharsets.UTF_8));
        return hashBytesToHexString(hash);
    }

    public static String hashFileStream(File file) throws NoSuchAlgorithmException, FileNotFoundException, IOException {
        MessageDigest messageDigest = MessageDigest.getInstance(HASH);
        FileInputStream inputStream = new FileInputStream(file);
        DigestInputStream digestInputStream = new DigestInputStream(inputStream, messageDigest);
        byte[] buffer = new byte[BUFFER_SIZE];
        while (digestInputStream.read(buffer) > -1) {
        }
        MessageDigest digest = digestInputStream.getMessageDigest();
        digestInputStream.close();
        return hashBytesToHexString(digest.digest());
    }
}

/**
 * Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
 */
