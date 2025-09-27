package tech.chenh.outlast;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class Encryption {

    private static final Logger LOG = LoggerFactory.getLogger(Encryption.class.getName());

    public static @NonNull String encrypt(byte @NonNull [] content, @NonNull String encryptionKey) {
        try {
            SecretKeySpec sk = new SecretKeySpec(encryptionKey.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, sk);
            byte[] bytes = cipher.doFinal(content);
            return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return new String(content, StandardCharsets.UTF_8);
        }
    }

    public static byte @NonNull [] decrypt(@NonNull String content, @NonNull String decryptionKey) {
        try {
            SecretKeySpec sk = new SecretKeySpec(decryptionKey.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, sk);
            return cipher.doFinal(Base64.getDecoder().decode(content));
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return content.getBytes(StandardCharsets.UTF_8);
        }
    }

}