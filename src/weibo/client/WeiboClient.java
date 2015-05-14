package weibo.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import weibo.interfaces.AccountExceptionHandler;
import weibo.interfaces.UserExceptionHandler;
import weibo.objects.WeiboAccount;
import common.Out;

public class WeiboClient {

	protected Map<String, WeiboAccount> accounts = null;
	protected List<String> uns = null;
	protected int count = 0;
	protected AccountExceptionHandler accHandler = null;
	protected UserExceptionHandler userHandler = null;
	protected static int TIMEOUT = 60 * 1000;
	protected boolean VISITOR_MODE = false;

	public WeiboClient() throws IOException {
		accounts = getVisitorAccounts();
		uns = new ArrayList<String>(accounts.keySet());
		VISITOR_MODE = true;
	}

	public WeiboClient(Map<String, WeiboAccount> accounts) {
		this.accounts = accounts;
		this.uns = new ArrayList<String>(accounts.keySet());
		VISITOR_MODE = false;
	}

	public void setAccountExceptionHandler(AccountExceptionHandler handler) {
		this.accHandler = handler;
	}

	public void setUserExceptionHandler(UserExceptionHandler handler) {
		this.userHandler = handler;
	}

	public static void setTimeout(int seconds) {
		TIMEOUT = seconds * 1000;
	}

	public static void setProxy(String ip, String port) {
		System.getProperties().setProperty("proxySet", "ture");
		System.setProperty("http.proxyHost", ip);
		System.setProperty("http.proxyPort", port);
		System.setProperty("https.proxyHost", ip);
		System.setProperty("https.proxyPort", port);
	}

	public static Map<String, String> login(String account, String password)
			throws IOException, JSONException {
		Out.println("LOGIN => " + account);
		String client_js_version = "v1.4.18";
		String prelogin_url = "http://login.sina.com.cn/sso/prelogin.php?"
				+ "entry=weibo&callback=sinaSSOController.preloginCallBack"
				+ "&rsakt=mod&checkpin=1" + "&client=ssologin.js("
				+ client_js_version + ")" + "&_=" + System.currentTimeMillis()
				/ 1000;
		String login_url = "http://login.sina.com.cn/sso/login.php?"
				+ "client=ssologin.js(" + client_js_version + ")";
		Response res = Jsoup.connect(prelogin_url).timeout(TIMEOUT)
				.ignoreContentType(true).execute();

		String php = res.body();
		php = php.substring(php.indexOf("(") + 1, php.indexOf(")"));

		JSONObject obj = new JSONObject(php);
		String servertime = obj.getInt("servertime") + "";
		String nonce = obj.getString("nonce");
		String rsakv = obj.getString("rsakv");

		String pswd_str = servertime + "\t" + nonce + "\n" + password;
		String sp = new BigIntegerRSA().rsaCrypt(obj.getString("pubkey"),
				"10001", pswd_str);

		String encodedName = Base64.encodeBase64String(account.replaceFirst(
				"@", "%40").getBytes());

		res = Jsoup
				.connect(login_url)
				.timeout(TIMEOUT)
				.data("entry", "weibo")
				.data("gateway", "1")
				.data("from", "")
				.data("savestate", "7")
				.data("useticket", "1")
				.data("pagerefer", "")
				.data("vsnf", "1")
				.data("su", encodedName)
				.data("service", "miniblog")
				.data("servertime", servertime)
				.data("nonce", nonce)
				.data("pwencode", "rsa2")
				.data("rsakv", rsakv)
				.data("sp", sp)
				.data("encoding", "UTF-8")
				.data("prelt", "228")
				.data("url",
						"http://weibo.com/ajaxlogin.php?framelogin=1"
								+ "&callback=parent.sinaSSOController.feedBackUrlCallBack")
				.data("returntype", "META").method(Method.POST)
				.referrer("http://weibo.com").cookies(res.cookies()).execute();

		// Out.println(res.body());

		res = Jsoup.connect("http://weibo.com/login.php").timeout(TIMEOUT)
				.cookies(res.cookies()).followRedirects(true).execute();

		// Out.println(res.body());

		Elements els = res.parse().select("html head meta[http-equiv=refresh]");
		if (els.isEmpty())
			return null;
		String redirect_url = els.first().attr("content");
		redirect_url = redirect_url.substring(8, redirect_url.length() - 1);
		res = Jsoup.connect(redirect_url).cookies(res.cookies())
				.timeout(TIMEOUT).followRedirects(true).execute();
		Out.println(res.cookies().toString());
		return res.cookies();
	}

	public static Map<String, WeiboAccount> getVisitorAccounts()
			throws IOException {
		String url = "http://passport.weibo.com/visitor/visitor?"
				+ "a=restore&cb=restore_back&from=weibo&_rand=" + Math.random();
		Map<String, WeiboAccount> accounts = new HashMap<String, WeiboAccount>();
		String srt = "E.vAfsJqAoJDRuiZJnv%21YEJvmBvXvCvXM46rcCvnEXBvzvGXfCvvvIEQW"
				+ "bTdH6R-oYW-PpJObvJOz-Ici6UA0BvA77%2AB.vAflW-P9Rc0lR-ykQDvnJqiQ"
				+ "VbiRVPBtS%21r3JeWQVqbgVdWiMZ4siOzu4DbmKPWFRsApPcM-dOPTiqWwKFu1"
				+ "KmkaM3SOi49nPZYAP4oHS-bYVdin5PPZNmPbNrnsS4kwRsM14-E%21AeMr";
		String srf = "" + (System.currentTimeMillis() / 1000);
		Response res = null;
		for (int i = 0; i < 100; i++) {
			res = Jsoup
					.connect(url)
					.cookie("tid",
							"V85+ctzkdifJu9icaeH33fA8lzu/pa6Fb3HxdPrE9LU=__100")
					.cookie("SRF", srf).cookie("SRT", srt).timeout(TIMEOUT)
					.execute();
			Map<String, String> cookie = res.cookies();
			if (cookie.containsValue("deleted"))
				break;
			srf = cookie.get("SRF");
			srt = cookie.get("SRT");
			WeiboAccount acc = new WeiboAccount();
			acc.COOKIES = cookie;
			acc.UN = "VISITOR-" + i;
			accounts.put(acc.UN, acc);
			Out.println(cookie.toString());
		}
		Out.println("VALID VISITOR NUM: " + accounts.size());
		return accounts;
	}

	public Document getHtmlDocument(WeiboAccount acc, String url)
			throws IOException, JSONException {
		if (acc == null) {
			Out.println("No available account for crawling.");
			System.exit(0);
		}
		Response res = Jsoup.connect(url).cookies(acc.COOKIES).timeout(TIMEOUT)
				.followRedirects(true).execute();
		Document doc = getFullHtml(res.parse());
		String redirected_url = res.url().toString();
		if (redirected_url.contains("http://sass.weibo")
				|| redirected_url.contains("sorry?userblock")) {
			if (accHandler != null)
				accHandler.freezeException(acc.UN);
			removeAccount(acc.UN);
			return null;
		} else if (redirected_url.contains("/signup/signup.php")
				|| redirected_url.contains("login.php")
				|| redirected_url.contains("http://passport")
				|| redirected_url.contains("http://login.sina.com.cn")) {
			refreshAccount(acc.UN);
			return null;
		} else if (redirected_url.endsWith("10.3.8.211")) {
			Out.println("REDIRECTED TO => " + redirected_url);
			Out.println("Your network connection is expired!");
			System.exit(0);
		} else if (!doc.select("div.veriyfycode").isEmpty()) {
			if (accHandler != null)
				accHandler.verifycodeException(acc.UN);
			removeAccount(acc.UN);
			return null;
		} else if (!VISITOR_MODE
				&& !doc.select("div#pl_common_unloginbase").isEmpty()) {
			refreshAccount(acc.UN);
		}
		return doc;
	}

	public static Document getFullHtml(Document doc) throws JSONException {
		Elements scripts = doc.select("script");
		StringBuilder sb = new StringBuilder(doc.body().toString());
		for (Element e : scripts) {
			String json = e.toString();
			if (!json.contains("\"html\":\""))
				continue;
			json = json.substring(json.indexOf("{"), json.lastIndexOf("}") + 1);
			JSONObject obj = new JSONObject(json);
			sb.append(obj.get("html"));
		}
		return Jsoup.parse(sb.toString());
	}

	public Document getAjaxHtml(WeiboAccount acc, String url)
			throws IOException, JSONException {
		if (acc == null) {
			Out.println("No available account for crawling.");
			System.exit(0);
		}
		if(acc.COOKIES==null) {
			removeAccount(acc.UN);
			return null;
		}
		Response res = Jsoup.connect(url).cookies(acc.COOKIES).timeout(TIMEOUT)
				.ignoreContentType(true).followRedirects(true).execute();
		String redirected_url = res.url().toString();
		if (redirected_url.contains("http://sass.weibo")
				|| redirected_url.contains("sorry?userblock")) {
			if (accHandler != null)
				accHandler.freezeException(acc.UN);
			removeAccount(acc.UN);
			return null;
		} else if (redirected_url.contains("/signup/signup.php")
				|| redirected_url.contains("login.php")
				|| redirected_url.contains("http://passport")
				|| redirected_url.contains("http://login.sina.com.cn")) {
			refreshAccount(acc.UN);
			return null;
		} else if (redirected_url.endsWith("10.3.8.211")) {
			Out.println("REDIRECTED TO => " + redirected_url);
			Out.println("Your network connection is expired!");
			System.exit(0);
		} else if (res.body().contains("div.veriyfycode")) {
			if (accHandler != null)
				accHandler.verifycodeException(acc.UN);
			removeAccount(acc.UN);
			return null;
		}
		if(!res.body().startsWith("{")){
			Out.println(res.body());
			Out.println(res.url().toString());
			System.exit(0);
		}
		JSONObject obj = new JSONObject(res.body()).getJSONObject("data");
		return Jsoup.parse(obj.getString("html"));
	}

	public void refreshAccount(String un) throws IOException, JSONException {
		if (VISITOR_MODE) {
			uns.remove(un);
			accounts.remove(un);
			Out.println("NUM OF ACCOUNTS IN USE => " + accounts.size());
			return;
		}
		WeiboAccount acc = accounts.get(un);
		if (acc == null)
			return;
		acc.COOKIES = login(acc.UN, acc.PSWD);
		acc.LOGIN_COUNTER++;
		acc.INCOMPLETE_COUNTER = 0;
		if (acc.COOKIES == null || acc.COOKIES.containsKey("login_sid_t")) {
			Out.println("ACCOUNT LIMITED => ( UN=" + acc.UN + ", RC="
					+ acc.REQUEST_COUNTER + ", LC=" + acc.LOGIN_COUNTER + " )");
			if (accHandler != null)
				accHandler.verifycodeException(acc.UN);
			accounts.remove(acc.UN);
			uns.remove(acc.UN);
		} else if (accHandler != null)
			accHandler.updateCookie(acc);
		Out.println("NUM OF ACCOUNTS IN USE => " + accounts.size());
	}

	public synchronized WeiboAccount getAccount() throws IOException,
			JSONException {
		WeiboAccount acc = null;
		do {
			if (accounts.size() == 0) {
				if (!VISITOR_MODE)
					break;
				accounts = getVisitorAccounts();
				uns = new ArrayList<String>(accounts.keySet());
			}
			if (count >= accounts.size())
				count = 0;
			String account = uns.get(count++);
			acc = accounts.get(account);
			if (acc.COOKIES == null)
				refreshAccount(account);
			acc.REQUEST_COUNTER++;
		} while (acc.COOKIES == null);
		return acc;
	}

	public void removeAccount(String un) {
		accounts.remove(un);
		uns.remove(un);
	}
}
