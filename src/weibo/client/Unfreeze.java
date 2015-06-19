package weibo.client;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.binary.Hex;

public class Unfreeze {

	public static String MD5Checksum(byte[] image) {
		String md5 = null;
		try {
			md5 = new String(Hex.encodeHex(MessageDigest.getInstance("MD5")
					.digest(image)));
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return md5;
	}

}
