package weibo.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.json.JSONException;
import org.jsoup.Jsoup;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import weibo.client.FaceNameStorage.ImageBytes;
import common.Out;
import common.TimeUtils;

public class Unfreeze {

	private static Scanner wait = new Scanner(System.in);

	public static byte[] getImage(String url, Map<String, String> cookie) {
		try {
			return Jsoup.connect(url).cookies(cookie).followRedirects(true)
					.ignoreContentType(true)
					.timeout(RequestConfig.TIME_REQUEST_OUT).execute()
					.bodyAsBytes();
		} catch (IOException e) {
			Out.println("EXCEPTION:" + url + ", " + e.getMessage());
		}
		return null;
	}

	public static List<ImageBytes> getFaces(Document doc,
			Map<String, String> cookie) {
		List<ImageBytes> faces_bytes = new ArrayList<ImageBytes>();
		Elements faces = doc.select("div.f_face > img.f_img");
		if (faces != null) {
			for (Element face : faces) {
				byte[] img = null;
				String url = face.attr("src");
				while (img == null) {
					img = getImage(url, cookie);
					// TimeUtils.Pause(RequestConfig.TIME_REQUEST_GAP);
				}
				faces_bytes.add(new ImageBytes(img));
				// TimeUtils.Pause(RequestConfig.TIME_REQUEST_GAP);
			}
		}
		return faces_bytes;
	}

	public static List<ImageBytes> getNames(Document doc,
			Map<String, String> cookie) {
		List<ImageBytes> faces_bytes = new ArrayList<ImageBytes>();
		Elements names = doc.select("div.names_list div[node-type=name]");
		if (names != null) {
			for (Element name : names) {
				byte[] img = null;
				String url = name.attr("style");
				url = "http://sass.weibo.com"
						+ url.substring(url.indexOf("/aj/quickdefreeze"),
								url.lastIndexOf(")"));
				while (img == null) {
					img = getImage(url, cookie);
					// TimeUtils.Pause(RequestConfig.TIME_REQUEST_GAP);
				}
				faces_bytes.add(new ImageBytes(img));
				// TimeUtils.Pause(RequestConfig.TIME_REQUEST_GAP);
			}
		}
		return faces_bytes;
	}

	public synchronized static boolean unfreezeAccount(WeiboAccount account,
			FaceNameStorage fs, CookieStorage cs) {
		if (fs == null)
			return false;
		Out.println("Unfreeze account: " + account.USERNAME);
		while (true) {
			try {
				Response res = Jsoup.connect("http://www.weibo.com")
						.followRedirects(true).cookies(account.COOKIES)
						.timeout(RequestConfig.TIME_REQUEST_OUT)
						.ignoreContentType(true).execute();
				if (res.url().toString().matches(".*login.*")) {
					Out.println("Cookie expired. Refreshing cookie...");
					try {
						account.COOKIES = Account.newCookie(account.USERNAME,
								account.PASSWORD);
						cs.updateCookie(account);
					} catch (JSONException e) {
						Out.println("Cookie refresh failed, " + e.getMessage());
						cs.invalidateCookie(account);
					}
					continue;
				} else if (!res.url().toString().matches(".*sass.*unfreeze.*")) {
					Out.println("No need to unfreeze, redirected to: "
							+ res.url());
					break;
				}
				Document doc = res.parse();
				if (!doc.select("#pl_unfreeze_phone").isEmpty()) {
					Out.println("Unfreeze by PHONE! (" + account.USERNAME + ","
							+ account.PASSWORD + ")\n"
							+ "Press ENTER key when you are done.");
					wait.nextLine();
					return true;
				}
				List<ImageBytes> faces_bytes = getFaces(doc, account.COOKIES);
				List<ImageBytes> names_bytes = getNames(doc, account.COOKIES);
				Map<String, Integer> names = new HashMap<String, Integer>();
				for (int i = 0; i < names_bytes.size(); i++) {
					names.put(names_bytes.get(i).getMD5Checksum(), i);
				}
				if (fs.storeImages(faces_bytes, names_bytes)) {
					Out.println("Mannually sovle FACE-NAME pairs.\n"
							+ " Press ENTER key when you are done.");
					wait.nextLine();
				}
				Map<String, String> pairs = fs.getPairs();
				String ans = "";
				for (int i = 0; i < faces_bytes.size(); i++) {
					ans += names.get(pairs.get(faces_bytes.get(i)
							.getMD5Checksum()));
				}
				if (ans.length() != 5) {
					Out.println("Order not right: " + ans);
					continue;
				}
				String url = "http://sass.weibo.com/aj/quickdefreeze/check?_wv=5&__rnd="
						+ System.currentTimeMillis();
				Out.println("Order: " + ans);
				res = Jsoup.connect(url).cookies(account.COOKIES)
						.followRedirects(true).ignoreContentType(true)
						.timeout(RequestConfig.TIME_REQUEST_OUT)
						.data("order", ans).data("_t", "0").method(Method.POST)
						.execute();
				if (res.url().toString().matches(".*sass.*unfreeze.*")) {
					Out.println("Unfreeze failed: " + res.url());
					continue;
				}
				Out.println("Unfreeze Succeed!");
				return true;
			} catch (IOException e) {
				Out.println("IOEXCEPTION:" + account.USERNAME + ", "
						+ e.getMessage());
				TimeUtils.PauseOneMinute();
			}
		}
		return false;
	}

}
