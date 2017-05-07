package br.com.willianantunes.test.util;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import org.apache.commons.codec.binary.Base64;

/**
 * It's necessary to set the static attribute char[] {@link #PASSWORD}. It will be use to encrypt and decrypt
 * a text. This basically means initialing a {@link Cipher} with algorithm "PBEWithMD5AndDES" and getting a key 
 * from {@link SecretKeyFactory} with the same algorithm.
 * @author Willian Antunes
 * @see <a href="http://stackoverflow.com/questions/1132567/encrypt-password-in-configuration-files-java">Encrypt Password in Configuration Files</a>
 */
public class ProtectedConfigFile {
	private static final char[] PASSWORD = "Tun3S!Budeg@.Kmel".toCharArray();
	private static final byte[] SALT = { (byte) 0xde, (byte) 0x33, (byte) 0x10, (byte) 0x12, (byte) 0xde, (byte) 0x33,
			(byte) 0x10, (byte) 0x12, };

	public static String encrypt(String property) {
		try {
			SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
			SecretKey key = keyFactory.generateSecret(new PBEKeySpec(PASSWORD));
			Cipher pbeCipher = Cipher.getInstance("PBEWithMD5AndDES");
			pbeCipher.init(Cipher.ENCRYPT_MODE, key, new PBEParameterSpec(SALT, 20));
			return base64Encode(pbeCipher.doFinal(property.getBytes("UTF-8")));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	public static String decrypt(String property) {
		try {
			SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
			SecretKey key = keyFactory.generateSecret(new PBEKeySpec(PASSWORD));
			Cipher pbeCipher = Cipher.getInstance("PBEWithMD5AndDES");
			pbeCipher.init(Cipher.DECRYPT_MODE, key, new PBEParameterSpec(SALT, 20));
			return new String(pbeCipher.doFinal(base64Decode(property)), "UTF-8");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static String base64Encode(byte[] bytes) {
		return Base64.encodeBase64String(bytes);
	}

	private static byte[] base64Decode(String property) {
		return Base64.decodeBase64(property);
	}
}