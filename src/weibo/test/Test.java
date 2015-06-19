package weibo.test;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.codec.binary.Hex;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import weibo.client.Account;
import weibo.client.AccountManager;
import weibo.client.AccountQueue;
import weibo.client.CookieStorage;
import weibo.client.WeiboAccount;
import weibo.database.AccountDB;
import common.Out;
import common.TimeUtils;

public class Test {

	public static void test4Visitor() throws IOException, JSONException {
		String url = "http://passport.weibo.com/visitor/genvisitor";
		Response res = Jsoup.connect(url).data("cb", "gen_callback")
				.ignoreContentType(true).method(Method.POST)
				.followRedirects(true).execute();
		String msg = res.body();
		System.out.println(msg);
		JSONObject obj = new JSONObject(msg.substring(msg.indexOf("{"),
				msg.lastIndexOf("}") + 1));
		String tid = obj.getJSONObject("data").getString("tid");
		System.out.println(tid);

		url = "http://passport.weibo.com/visitor/visitor";
		res = Jsoup.connect(url).data("a", "incarnate").data("t", tid)
				.cookie("tid", tid).ignoreContentType(true)
				.followRedirects(true).execute();
		System.out.println(res.cookies());

		url = "http://weibo.com/1875034341/CgY1SjF2J";
		res = Jsoup.connect(url).cookies(res.cookies()).followRedirects(true)
				.execute();
		System.out.println(res.body());
	}

	public static void testExecutorService() {
		ExecutorService es = Executors.newFixedThreadPool(100);
		Random rnd = new Random();
		for (int i = 0; i < 100; i++) {
			es.execute(new Runnable() {
				@Override
				public void run() {
					int time = rnd.nextInt(1000);
					Out.println(time + " seconds.");
					try {
						Thread.sleep(time * 1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					Out.println("End.");
				}
			});
		}
		es.shutdown();
	}

	public static void downloadCaptcha() throws IOException {
		String url = "http://login.sina.com.cn/cgi/pin.php";
		// String r = String.format("%.0f", Math.floor(Math.random()*1e8));
		Response res = Jsoup.connect(url).ignoreContentType(true).execute();
		BufferedOutputStream out = new BufferedOutputStream(
				new FileOutputStream("G:/sina-captcha/test.png"));
		out.write(res.bodyAsBytes());
		out.close();
	}

	public static void downloadCaptcha(String prefix, int n)
			throws InterruptedException {
		String url = "http://login.sina.com.cn/cgi/pin.php";
		for (int i = 0; i < n; i++) {
			if ((1 + i) % 100 == 0)
				Out.println((i + 1) + "");
			try {
				Response res = Jsoup.connect(url).ignoreContentType(true)
						.timeout(10 * 1000).execute();
				BufferedOutputStream out = new BufferedOutputStream(
						new FileOutputStream("G:/sina-captcha/" + prefix + i
								+ ".png"));
				out.write(res.bodyAsBytes());
				out.close();
			} catch (IOException e) {
				Out.println(e.getMessage());
				i--;
			}
			// Thread.sleep(100);
		}
	}

	public static void testMap() {
		Map<String, String> cookie = new HashMap<String, String>();
		cookie.put("X", "1");
		cookie.put("Y", "2");
		cookie.put("Z", "3");
		Out.println(Arrays.toString(cookie.entrySet().toArray()));
		Object arg = cookie;
		Out.println("" + (arg instanceof HashMap));
		Out.println(arg.toString());
	}

	public static void testNewLogin() {
		WeiboAccount account = new AccountDB().getAccount();
		try {
			Out.println(Account.newCookie(account.USERNAME, account.PASSWORD)
					.toString());
		} catch (IOException | JSONException e) {
			e.printStackTrace();
		}
	}

	public static void testRefreshAllCookie() {
		CookieStorage cs = new AccountDB();
		AccountManager am = new AccountQueue(cs);
		for (int i = 0; i < 10; i++) {
			WeiboAccount acc = am.getNextAccount();
			Out.println(acc.USERNAME + ": " + acc.COOKIES);
		}
	}

	public static void testRegEx() {
		Out.println("http://login.sina.com.cn".matches(".*loginx.*|.*sina.*"));
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

	public static void testMD5Checksum() {
		File file = new File("G:/sina-unfreeze/wqs2.png");
		try {
			Out.println(MD5Checksum(Files.readAllBytes(file.toPath())));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void crackUnfreeze() {
		CookieStorage cs = new AccountDB();
		AccountManager am = new AccountQueue(cs);
		String faces_path = "G:/sina-unfreeze/faces";
		String names_path = "G:/sina-unfreeze/names";
		int gap = 2;
		File faces_dir = new File(faces_path);
		if (!faces_dir.exists()) {
			faces_dir.mkdirs();
		}
		File names_dir = new File(names_path);
		if (!names_dir.exists()) {
			names_dir.mkdirs();
		}
		int count = 0;
		for (int i = 0; i < 95; i++) {
			WeiboAccount acc = am.getNextAccount();
			Out.println(acc.USERNAME);
			try {
				Response res = Jsoup.connect("http://www.weibo.com")
						.followRedirects(true).cookies(acc.COOKIES).timeout(10)
						.ignoreContentType(true).execute();
				if (res.url().toString().matches(".*sass.*unfreeze.*")) {
					Document doc = res.parse();
					Elements faces = doc.select("div.faces_list img");
					Elements names = doc
							.select("div.names_list div[node-type=name]");
					if (faces != null) {
						for (Element face : faces) {
							byte[] img = Jsoup.connect(face.attr("src"))
									.cookies(acc.COOKIES).followRedirects(true)
									.ignoreContentType(true).timeout(10)
									.execute().bodyAsBytes();
							String md5 = MD5Checksum(img);
							File file = new File(faces_path + "/" + md5
									+ ".png");
							if (!file.exists()) {
								BufferedOutputStream out = new BufferedOutputStream(
										new FileOutputStream(file));
								out.write(img);
								out.close();
							}
							TimeUtils.Pause(gap);
						}
					}
					if (names != null) {
						for (Element name : names) {
							String style = name.attr("style");
							String url = "http://sass.weibo.com"
									+ style.substring(
											style.indexOf("/aj/quickdefreeze"),
											style.lastIndexOf(")"));
							byte[] img = Jsoup.connect(url)
									.cookies(acc.COOKIES).timeout(10)
									.ignoreContentType(true)
									.followRedirects(true).execute()
									.bodyAsBytes();
							String md5 = MD5Checksum(img);
							File file = new File(names_path + "/" + md5
									+ ".png");
							if (!file.exists()) {
								BufferedOutputStream out = new BufferedOutputStream(
										new FileOutputStream(file));
								out.write(img);
								out.close();
							}
							TimeUtils.Pause(gap);
						}
					}
				}
				count++;
			} catch (IOException e) {
				Out.println(e.getMessage());
				i = count - 1;
				TimeUtils.Pause(300);
			}
		}
		for (String str : faces_dir.list()) {
			Out.println(str);
		}
		for (String str : names_dir.list()) {
			Out.println(str);
		}
	}

	public static void main(String[] args) throws IOException, JSONException,
			InterruptedException {
		crackUnfreeze();
	}

}
