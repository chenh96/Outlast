package tech.chenh.outlast.util;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class Encryption {

    public static String encrypt(byte[] content, String encryptionKey) {
        try {
            SecretKeySpec sk = new SecretKeySpec(encryptionKey.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, sk);
            byte[] bytes = cipher.doFinal(content);
            return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            return new String(content, StandardCharsets.UTF_8);
        }
    }

    public static byte[] decrypt(String content, String decryptionKey) {
        try {
            SecretKeySpec sk = new SecretKeySpec(decryptionKey.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, sk);
            return cipher.doFinal(Base64.getDecoder().decode(content));
        } catch (Exception e) {
            return content.getBytes(StandardCharsets.UTF_8);
        }
    }

}