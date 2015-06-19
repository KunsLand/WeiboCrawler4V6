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

import weibo.FaceNamePairs;
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
		AccountManager am = new AccountQueue(cs, null);
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
		AccountManager am = new AccountQueue(cs, new FaceNamePairs(
				"G:/sina-unfreeze"));
		int i = 0;
		while (i < 95) {
			WeiboAccount account = am.getNextAccount();
			try {
				Response res = Jsoup.connect("http://www.weibo.com")
						.cookies(account.COOKIES).ignoreContentType(true)
						.timeout(10 * 1000).followRedirects(true).execute();
				if (!res.url().toString().matches(".*sass.*unfreeze.*")) {
					continue;
				}
				am.unfreezeAccount(account);
				i++;
			} catch (IOException e) {
				Out.println(e.getMessage());
				TimeUtils.PauseOneMinute();
			}
		}
	}

	public static void printFaceNameFileNames() {
		File faces_dir = new File("G:/sina-unfreeze/faces");
		File names_dir = new File("G:/sina-unfreeze/names");
		String[] faces = faces_dir.list();
		String[] names = names_dir.list();
		for (String str : faces) {
			System.out.println(str.substring(0, str.indexOf(".")));
		}
		System.out.println();
		for (String str : names) {
			System.out.println(str.substring(0, str.indexOf(".")));
		}
	}

	public static void main(String[] args) throws IOException, JSONException,
			InterruptedException {
		crackUnfreeze();
	}

}
