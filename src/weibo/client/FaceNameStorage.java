package weibo.client;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Hex;

public interface FaceNameStorage {

	public Map<String, String> getPairs();

	public boolean storeImages(List<ImageBytes> faces, List<ImageBytes> names);
	
	public void storePairs(Map<String,String> pairs);

	public static class ImageBytes {
		private final byte[] img;
		private final String md5;

		public ImageBytes(byte[] img) {
			this.img = img;
			md5 = MD5Checksum(img);
		}

		public byte[] getBytes() {
			return img;
		}
		
		public String getMD5Checksum(){
			return md5;
		}

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

}
