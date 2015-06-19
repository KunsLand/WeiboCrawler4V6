package weibo.client;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.select.Elements;

import com.cloudsightapi.CloudSight;

import common.Out;

public class Account {
	private final static String CLIENT_JS_VERSION = "v1.4.18";

	private static Response prelogin() throws IOException {
		String prelogin_url = "http://login.sina.com.cn/sso/prelogin.php?"
				+ "entry=weibo&callback=sinaSSOController.preloginCallBack"
				+ "&rsakt=mod&checkpin=1" + "&client=ssologin.js("
				+ CLIENT_JS_VERSION + ")" + "&_=" + System.currentTimeMillis()
				/ 1000;
		return Jsoup.connect(prelogin_url)
				.timeout(RequestConfig.TIME_REQUEST_OUT)
				.ignoreContentType(true).execute();
	}

	private static Map<String, String> prepareLoginData(JSONObject obj,
			String account, String password) throws JSONException {
		String servertime = obj.getInt("servertime") + "";
		String nonce = obj.getString("nonce");
		String rsakv = obj.getString("rsakv");

		String pswd_str = servertime + "\t" + nonce + "\n" + password;
		String sp = new BigIntegerRSA().rsaCrypt(obj.getString("pubkey"),
				"10001", pswd_str);

		String encodedName = Base64.encodeBase64String(account.replaceFirst(
				"@", "%40").getBytes());

		Map<String, String> data = new HashMap<String, String>();

		data.put("entry", "weibo");
		data.put("gateway", "1");
		data.put("from", "");
		data.put("savestate", "7");
		data.put("useticket", "1");
		data.put("pagerefer", "");
		data.put("vsnf", "1");
		data.put("su", encodedName);
		data.put("service", "miniblog");
		data.put("servertime", servertime);
		data.put("nonce", nonce);
		data.put("pwencode", "rsa2");
		data.put("rsakv", rsakv);
		data.put("sp", sp);
		data.put("encoding", "UTF-8");
		data.put("prelt", "228");
		data.put("url", "http://weibo.com/ajaxlogin.php?framelogin=1"
				+ "&callback=parent.sinaSSOController.feedBackUrlCallBack");
		data.put("returntype", "META");
		return data;
	}

	private static Map<String, String> afterlogin(Map<String, String> cookie)
			throws IOException {
		Response res = Jsoup.connect("http://weibo.com/login.php")
				.timeout(RequestConfig.TIME_REQUEST_OUT).cookies(cookie)
				.followRedirects(true).execute();

		Elements els = res.parse().select("html head meta[http-equiv=refresh]");
		if (els.isEmpty()) {
			Out.println("Login failed.");
			return null;
		}
		Out.println("Login Succeed.");
		String redirect_url = els.first().attr("content");
		redirect_url = redirect_url.substring(8, redirect_url.length() - 1);
		return Jsoup.connect(redirect_url).cookies(res.cookies())
				.timeout(RequestConfig.TIME_REQUEST_OUT).followRedirects(true)
				.execute().cookies();
	}

	public static Map<String, String> newCookie(String account, String password)
			throws IOException, JSONException {
		Out.println("LOGIN => " + account);

		// send prelogin request
		Response res = prelogin();

		// parse response information
		String php = res.body();
		php = php.substring(php.indexOf("(") + 1, php.indexOf(")"));
		JSONObject obj = new JSONObject(php);
		String pcid = obj.getString("pcid");

		// prepare data for login
		String login_url = "http://login.sina.com.cn/sso/login.php?"
				+ "client=ssologin.js(" + CLIENT_JS_VERSION + ")";
		Map<String, String> data = prepareLoginData(obj, account, password);

		// send login request without CAPTCHA
		// CAPTCHA is another fancy name of verification code
		res = Jsoup.connect(login_url).cookies(res.cookies())
				.timeout(RequestConfig.TIME_REQUEST_OUT).method(Method.POST)
				.data(data).referrer("http://weibo.com")
				.ignoreContentType(true).execute();

		// retcode=4049, CAPTCHA code is needed
		// retcode=2070, you submit wrong CAPTCHA code
		while (res.body().contains("retcode=4049")
				|| res.body().contains("retcode=2070")) {
			String captcha = null;
			while (true) {
				String captchaUrl = "http://login.sina.com.cn/cgi/pin.php?r="
						+ String.format("%.0f", Math.floor(Math.random() * 1e8))
						+ "&pcid=" + pcid;
				// Use CloudSight API to recognize numbers in CAPTCHA image.
				try {
					captcha = CloudSight.solveCaptcha(captchaUrl);
					if (captcha.length() == 5) {
						Out.println("Captcha:" + captcha);
						break;
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				// CloudSight is unstable for free-charge users.
				Out.println("Invalid Captcha:" + captcha);
			}
			// send login requests with CAPTCHA
			res = Jsoup.connect(login_url).cookies(res.cookies())
					.timeout(RequestConfig.TIME_REQUEST_OUT)
					.method(Method.POST).referrer("http://weibo.com")
					.ignoreContentType(true).data(data).data("pcid", pcid)
					.data("door", captcha).execute();
		}

		String body = res.body();
		int index = body.indexOf("retcode=");
		body = body.substring(index);
		Out.println(body.substring(0, body.contains("'") ? 9 : 12));

		// login succeed
		return afterlogin(res.cookies());
	}
}
