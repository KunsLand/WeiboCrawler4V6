package weibo;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import common.FileUtils;
import common.Out;
import weibo.client.FaceNameStorage;

public class FaceNamePairs implements FaceNameStorage {

	private String faces_path;
	private String names_path;
	private String pairs_file;

	public FaceNamePairs(String path) {
		faces_path = path + (path.endsWith("/") ? "faces" : "/faces/");
		names_path = path + (path.endsWith("/") ? "names" : "/names/");
		pairs_file = path + (path.endsWith("/") ? "pairs.txt" : "/pairs.txt");
	}

	@Override
	public Map<String, String> getPairs() {
		Map<String, String> pairs = new HashMap<String, String>();
		File file = new File(pairs_file);
		if (!file.exists()) {
			return pairs;
		}
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line = null;
			while ((line = br.readLine()) != null) {
				String[] strs = line.split("\t");
				if (strs.length == 2) {
					pairs.put(strs[0], strs[1]);
				}
			}
			br.close();
		} catch (IOException e) {
			Out.println(e.getMessage());
		}
		return pairs;
	}

	@Override
	public boolean storeImages(List<ImageBytes> faces, List<ImageBytes> names) {
		boolean flag = false;
		for (ImageBytes img : faces) {
			File file = new File(faces_path + img.getMD5Checksum() + ".png");
			if (!file.exists()) {
				try {
					BufferedOutputStream out = new BufferedOutputStream(
							new FileOutputStream(file));
					out.write(img.getBytes());
					out.close();
					flag = true;
				} catch (IOException e) {
					Out.println(e.getMessage());
				}
			}
		}
		for (ImageBytes img : names) {
			File file = new File(names_path + img.getMD5Checksum() + ".png");
			if (!file.exists()) {
				try {
					BufferedOutputStream out = new BufferedOutputStream(
							new FileOutputStream(file));
					out.write(img.getBytes());
					out.close();
					flag = true;
				} catch (IOException e) {
					Out.println(e.getMessage());
				}
			}
		}
		return flag;
	}

	@Override
	public void storePairs(Map<String, String> pairs) {
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, String> entry : pairs.entrySet()) {
			sb.append(entry.getKey()).append("\t").append(entry.getValue())
					.append("\n");
		}
		FileUtils.WriteToFile(pairs_file, sb.toString(), false);
	}

}
