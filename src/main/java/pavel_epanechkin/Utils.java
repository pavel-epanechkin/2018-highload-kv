package pavel_epanechkin;

import javax.xml.bind.DatatypeConverter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Utils {

    public static String getMD5(byte[] data) {
        String result = "";

        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.update(data);
            byte[] digest = messageDigest.digest();
            result = DatatypeConverter.printBase64Binary(digest);
        }
        catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return result;
    }
}
