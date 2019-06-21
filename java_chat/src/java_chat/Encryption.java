package java_chat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;

import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 * RSA example with OAEP Padding and random key generation.
 */
public class Encryption {

    private KeyPair keyPair;
    private Key secKey;
    private Cipher xCipher;
    private Cipher sCipher;
    private byte[] keyBlock;
    private IvParameterSpec sIvSpec;
    private Key privKey;
    private Key pubKey;
    private Key encPubKey;
   // private SecureRandom random = Utils.createFixedRandom();

    public Encryption() {
        Security.addProvider(new BouncyCastleProvider());
    }

    private static byte[] packKeyAndIv(Key key, IvParameterSpec ivSpec) throws IOException {
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();

        bOut.write(ivSpec.getIV());
        bOut.write(key.getEncoded());

        return bOut.toByteArray();
    }

    private static Object[] unpackKeyAndIV(byte[] data) {
        byte[] keyD = new byte[16];
        byte[] iv = new byte[data.length - 16];

        return new Object[]{
            new SecretKeySpec(data, keyD.length, iv.length, "AES"),
            new IvParameterSpec(data, 0, 16)
        };
    }

    public void setKeyPair() {
        try {
            //    byte[]           input = new byte[] { 0x00, (byte)0xbe, (byte)0xef };
            SecureRandom random = Utils.createFixedRandom();
            // create the RSA Key
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", "BC");
            generator.initialize(1024, random);
            keyPair = generator.generateKeyPair();
            pubKey = keyPair.getPublic();
            privKey = keyPair.getPrivate();
        } catch (NoSuchAlgorithmException | NoSuchProviderException ex) {
            Logger.getLogger(Encryption.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public byte[] getPubKey() {
       return pubKey.getEncoded();
        //return pubKey;
    }

    public void setEncPubKey(byte[] key) {
        try {
            X509EncodedKeySpec spec = new X509EncodedKeySpec(key);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            System.out.println("spec " + spec);
            encPubKey = kf.generatePublic(spec);
            //   encPubKey = key;
        } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
            Logger.getLogger(Encryption.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public byte[] setKeyBlock() throws Exception {
        SecureRandom random = Utils.createFixedRandom();
        //System.out.println("input            : " + Utils.toHex(input));
        // create the symmetric key and iv
        secKey = Utils.createKeyForAES(256, random);
        sIvSpec = Utils.createCtrIvForAES(0, random);
        // symmetric key/iv wrapping step
        xCipher = Cipher.getInstance("RSA/NONE/OAEPWithSHA1AndMGF1Padding", "BC");

        xCipher.init(Cipher.ENCRYPT_MODE, encPubKey, random);

        byte[] kb = xCipher.doFinal(packKeyAndIv(secKey, sIvSpec));
        keyBlock = kb;
        return kb;
    }

    public void setKB(byte[] kb) {
        keyBlock = kb;
    }

    public byte[] encrypt(byte[] input) {
        byte[] cipherText = null;
        try {
          //  System.out.println(keyBlock);
         //   System.out.println(privKey);
            xCipher = Cipher.getInstance("RSA/NONE/OAEPWithSHA1AndMGF1Padding", "BC");
            xCipher.init(Cipher.DECRYPT_MODE, privKey);

            Object[] keyIv = unpackKeyAndIV(xCipher.doFinal(keyBlock));
            // encryption step
            sCipher = Cipher.getInstance("AES/CTR/PKCS7Padding", "BC");

            sCipher.init(Cipher.ENCRYPT_MODE, (Key) keyIv[0], (IvParameterSpec) keyIv[1]);

            cipherText = sCipher.doFinal(input);

          //  System.out.println(
          //          "keyBlock length  : " + keyBlock.length);
          //  System.out.println(
           //         "cipherText length: " + cipherText.length);

        } catch (NoSuchAlgorithmException | NoSuchProviderException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException | InvalidKeyException | InvalidAlgorithmParameterException ex) {
            Logger.getLogger(Encryption.class.getName()).log(Level.SEVERE, null, ex);
        }
        return cipherText;

    }

    public byte[] decrypt(byte[] cipherText) throws NoSuchPaddingException {
        byte[] plainText = null;
        try {
            // symmetric key/iv unwrapping step
            xCipher = Cipher.getInstance("RSA/NONE/OAEPWithSHA1AndMGF1Padding", "BC");
          //  System.out.println(privKey);
            xCipher.init(Cipher.DECRYPT_MODE, privKey);
          //  System.out.println(keyBlock);
            Object[] keyIv = unpackKeyAndIV(xCipher.doFinal(keyBlock));

            // decryption step
            
             sCipher = Cipher.getInstance("AES/CTR/PKCS7Padding", "BC");
            sCipher.init(Cipher.DECRYPT_MODE, (Key) keyIv[0], (IvParameterSpec) keyIv[1]);

            plainText = sCipher.doFinal(cipherText);

           // System.out.println(
          //          "plain            : " + Utils.toHex(plainText));
        } catch (InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException | NoSuchAlgorithmException | NoSuchProviderException ex) {
            Logger.getLogger(Encryption.class.getName()).log(Level.SEVERE, null, ex);
        }
        return plainText;
    }

    public static void main(String[] args) {
        try {
            Encryption e = new Encryption();
            e.setKeyPair();
           
            Encryption e2 = new Encryption();
             e2.setKeyPair();
            String s = "hello world";

            byte[] key = e.getPubKey();
            e2.setEncPubKey(key);
            byte[] b = e2.setKeyBlock();
            e.setKB(b);

            b = Utils.toByteArray(s);

            b = e.encrypt(b);
            //System.out.println(Utils.toString(b));
//System.out.println(b.toString());
            System.out.println(Utils.toString(e2.decrypt(b)));
        } catch (Exception ex) {
            Logger.getLogger(Encryption.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
}
