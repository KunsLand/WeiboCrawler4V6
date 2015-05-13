package weibo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import common.Out;
import common.TimeUtils;

public class WeiboClient {

	private Map<String, WeiboAccount> accounts = null;
	private List<String> uns = null;
	private int count = 0;
	private AccountExceptionHandler accHandler = null;
	private UserExceptionHandler userHandler = null;
	private static int TIMEOUT = 60 * 1000;
	private boolean VISITOR_MODE = false;

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

	public List<String> getAllFollows(String uid, int folNum) {
		if (VISITOR_MODE) {
			Out.println("You NEED initialize the WeiboClient instance with a TRUE weibo ACCOUNT!"
					+ "\nVisitors CANNOT access the user follows pages!");
			System.exit(0);
		}
		Out.println(uid + " => " + folNum);
		try {
			Set<String> fol = new HashSet<String>();
			int pages = folNum / 20 + 1;
			for (int i = 1; i <= pages; i++) {
				String url = "http://gov.weibo.com/attention/attsList.php"
						+ "?action=1&uid=" + uid + "&page=" + i;
				WeiboAccount acc = getAccount();
				if (acc.COOKIES == null) {
					Out.println("No Available Account.");
					System.exit(0);
				}
				Response res = null;
				try {
					res = Jsoup.connect(url).cookies(acc.COOKIES)
							.timeout(TIMEOUT).followRedirects(true).execute();
				} catch (IOException e) {
					Out.println(e.getMessage() + " => ( UID=" + uid + ", PAGE="
							+ i + " )");
					TimeUtils.PauseOneMinute();
					i--;
					continue;
				}
				String redirected_url = res.url().toString();
				if (redirected_url.endsWith("sorry?userblock")) {
					refreshAccount(acc.UN);
					i--;
					continue;
				} else if (redirected_url.endsWith("10.3.8.211")) {
					Out.println("REDIRECTED TO => " + redirected_url);
					Out.println("Your network connection is expired!");
					System.exit(0);
				} else if (redirected_url
						.startsWith("http://login.sina.com.cn/sso/login.php?")) {
					Out.println("REDIRECTED TO => " + redirected_url);
					refreshAccount(acc.UN);
					i--;
					continue;
				} else if (!redirected_url.startsWith("http://gov.weibo.com")) {
					Out.println("REDIRECTED TO => " + redirected_url);
					if (userHandler != null)
						userHandler.enterpriseUser(uid);
					return null;
				}
				Document doc = res.parse();
				Elements el = doc.select("a[namecard=true]");
				for (Element e : el)
					fol.add(e.attr("uid"));
			}
			return new ArrayList<String>(fol);
		} catch (IOException | JSONException e) {
			Out.println(e.getMessage() + " => " + uid);
		}
		return null;
	}

	public UserIndexPage getUserIndexPage(String uid) {
		try {
			String url = "http://www.weibo.com/u/" + uid;
			WeiboAccount acc = getAccount();
			Document doc = getHtmlDocument(acc, url);
			if (doc == null)
				return null;
			Elements counters = doc.select("table.tb_counter strong");
			if (!doc.select("div.page_error").isEmpty()) {
				if (userHandler != null)
					userHandler.userNotAvailable(uid);
				return null;
			}
			if (counters.isEmpty()) {
				Out.println(uid + " => Incomplete index page.");
				acc.INCOMPLETE_COUNTER++;
				if (acc.INCOMPLETE_COUNTER > 5) {
					refreshAccount(acc.UN);
				}
				return null;
			}
			if (acc.INCOMPLETE_COUNTER > 0)
				acc.INCOMPLETE_COUNTER--;
			UserIndexPage uip = new UserIndexPage();
			uip.UID = uid;
			uip.FOLLOWS = Integer.valueOf(counters.get(0).ownText());
			uip.FANS = Integer.valueOf(counters.get(1).ownText());
			uip.BLOGS = Integer.valueOf(counters.get(2).ownText());
			uip.NICK_NAME = doc.select("span.username").text();
			Elements posts = doc.select("div[action-type="
					+ "feed_list_item]:not([feedtype=top])"
					+ " div.WB_detail > div[class=WB_from S_txt2] a");
			uip.LAST_POST = posts.isEmpty() ? null : posts.first()
					.attr("title");
			boolean v1 = !doc.select("div.verify_area a[class~=icon_verify_v]")
					.isEmpty(), v2 = !doc.select(
					"div.verify_area a[class~=icon_verify_co_v]").isEmpty();
			if (v1)
				uip.VERIFIED = 1;
			else if (v2)
				uip.VERIFIED = 2;
			else
				uip.VERIFIED = 0;
			Out.println(uip.toString());
			return uip;
		} catch (IOException | JSONException e) {
			Out.println(e.getMessage() + " => " + uid);
			TimeUtils.PauseOneMinute();
		}
		return null;
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
				|| redirected_url.contains("http://passport")) {
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
