package server;



import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import jakarta.xml.bind.DatatypeConverter;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Arrays;

public class CryptoService {

    private static CryptoService service;

    private final Key key;
    private final IvParameterSpec ivSpec;
    private final String transformation = "AES/CBC/PKCS5Padding";

    private CryptoService() {
        SecureRandom  random = null;
        KeyGenerator keygen = null;
        try {
            random = SecureRandom.getInstanceStrong();
            keygen = KeyGenerator.getInstance("AES");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        byte[] sec_bytes = new byte[16];
        random.nextBytes(sec_bytes);

        keygen.init(256);
        key = keygen.generateKey();
        ivSpec = new IvParameterSpec(sec_bytes);
    }

    public static CryptoService getService(){
        if (service==null) service = new CryptoService();

        return service;
    }


    /**
     * зашифровывает строку
     * @param inString
     * @return
     * @throws InvalidKeyException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     */
    public String encryptString(String inString) throws ServerCloudException{
        byte[] inText = inString.getBytes(StandardCharsets.UTF_8);

        try {
            Cipher cipher = Cipher.getInstance(transformation);

            cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);

            return DatatypeConverter.printHexBinary(cipher.doFinal(inText));
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | IllegalBlockSizeException |
                 BadPaddingException | InvalidAlgorithmParameterException e) {
            throw new ServerCloudException(e.getMessage());
        }

    }

    /**
     * расшифровывает строку
     * @param inString
     * @return
     * @throws InvalidKeyException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     */
    public String decryptString(String inString) throws ServerCloudException {

        byte[] inText = DatatypeConverter.parseHexBinary(inString);

        try {
            Cipher cipher = Cipher.getInstance(transformation);

            cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);

            return new String(cipher.doFinal(inText), StandardCharsets.UTF_8);
        } catch (InvalidAlgorithmParameterException | NoSuchPaddingException | NoSuchAlgorithmException |
                 InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
            throw new ServerCloudException(e.getMessage());
        }

    }
}
