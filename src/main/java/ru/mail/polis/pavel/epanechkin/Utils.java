package ru.mail.polis.pavel.epanechkin;

import javax.xml.bind.DatatypeConverter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Utils {


    public static String getSHA256(byte[] data) {
        String result = "";

        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(data);
            byte[] digest = messageDigest.digest();
            result = DatatypeConverter.printBase64Binary(digest);
        }
        catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return result;
    }

    public static int getSimpleStringsDistance(String a, String b) {
        int minStringSize = Math.min(a.length(), b.length());
        int maxStringSize = Math.max(a.length(), b.length());
        int distance = maxStringSize - minStringSize;

        for (int i = 0; i < minStringSize; i++) {
            distance += a.charAt(i) == b.charAt(i) ? 0 : 1;
        }
        return distance;
    }
}
