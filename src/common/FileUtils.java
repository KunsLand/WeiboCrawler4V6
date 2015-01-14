package common;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

public class FileUtils {

	public static void downloadImage(String url, String filePath)
			throws MalformedURLException, IOException {
		String name = url.substring(url.lastIndexOf("/") + 1);
		String format = name.substring(name.lastIndexOf(".") + 1);
		String file = filePath.endsWith("/") ? filePath + name : filePath + "/"
				+ name;
		BufferedImage image = ImageIO.read(new URL(url));
		ImageIO.write(image, format, new File(file));
	}

	public static void checkSubString(String file, String substr) {
		BufferedReader br;
		File f = new File(file);
		System.out.println("Finding a string '" + substr + "' from file:"
				+ file);
		if (!f.exists())
			return;
		try {
			br = new BufferedReader(new FileReader(file));
			String line;
			int lineNum = 0;
			boolean flag = false;
			while ((line = br.readLine()) != null) {
				if (line.contains(substr)) {
					System.out.println("line: " + lineNum + "\t" + line);
					flag = true;
					break;
				}
			}
			br.close();
			if (!flag)
				System.out.println("not exist");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static String ReadJson(String file, int maxline) {
		StringBuilder sb = new StringBuilder("");
		BufferedReader br;
		File f = new File(file);
		if (!f.exists())
			return sb.toString();
		try {
			br = new BufferedReader(new FileReader(file));
			String line;
			while ((line = br.readLine()) != null && maxline-- > 0) {
				sb.append(line + ",\n");
			}
			sb.replace(sb.lastIndexOf(","), sb.length(), "\n");
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return sb.toString();
	}

	public static String ReadFile(String file) {
		StringBuilder sb = new StringBuilder("");
		BufferedReader br;
		File f = new File(file);
		if (!f.exists())
			return sb.toString();
		try {
			br = new BufferedReader(new FileReader(file));
			String line;
			while ((line = br.readLine()) != null) {
				sb.append(line);
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return sb.toString();
	}

	public static List<String> ReadFileByLine(String file) {
		List<String> result = new ArrayList<String>();
		BufferedReader br;
		try {
			File f = new File(file);
			if (!f.exists())
				return result;
			br = new BufferedReader(new FileReader(file));
			String line;
			while ((line = br.readLine()) != null) {
				result.add(line);
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}

	public static void WriteToFile(String str, String file, boolean append) {
		if (file == null || file.isEmpty() || str == null || str.isEmpty())
			return;
		try {
			File f = new File(file);
			if (!f.exists()) {
				if (!f.getParentFile().exists())
					f.getParentFile().mkdirs();
				f.createNewFile();
			}
			PrintWriter out = new PrintWriter(
					new FileOutputStream(file, append));
			// time region
			out.println(str);
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
