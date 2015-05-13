package weibo.client;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.apache.commons.codec.binary.Hex;

public class BigIntegerRSA {

	public String rsaCrypt(String modeHex, String exponentHex, String messageg) {

		BigInteger m = new BigInteger(modeHex, 16); /* public exponent */
		BigInteger e = new BigInteger(exponentHex, 16); /* modulus */
		RSAPublicKeySpec spec = new RSAPublicKeySpec(m, e);

		RSAPublicKey pub;
		byte[] encryptedContentKey = null;
		try {
			KeyFactory factory = KeyFactory.getInstance("RSA");
			pub = (RSAPublicKey) factory.generatePublic(spec);
			Cipher enc = Cipher.getInstance("RSA");
			enc.init(Cipher.ENCRYPT_MODE, pub);
			encryptedContentKey = enc.doFinal(messageg.getBytes("GB2312"));
		} catch (UnsupportedEncodingException | NoSuchAlgorithmException
				| InvalidKeySpecException | NoSuchPaddingException
				| InvalidKeyException | IllegalBlockSizeException
				| BadPaddingException e1) {
			e1.printStackTrace();
		}

		return new String(Hex.encodeHex(encryptedContentKey));
	}
}
