package weibo;

import java.io.IOException;
import java.util.ArrayList;
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

import common.Out;

public class WeiboClient {

	private Map<String, WeiboAccount> accounts = null;
	private List<String> uns = null;
	private int count = 0;
	private ExceptionHandler handler = null;

	public WeiboClient(Map<String, WeiboAccount> accounts,
			ExceptionHandler handler) {
		this.accounts = accounts;
		this.uns = new ArrayList<String>(accounts.keySet());
		this.handler = handler;
	}

	public Map<String, String> login(String account, String password)
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
		Response res = Jsoup.connect(prelogin_url).ignoreContentType(true)
				.execute();

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
		
		Out.println(res.body());

		res = Jsoup.connect("http://weibo.com/login.php")
				.cookies(res.cookies()).followRedirects(true).execute();

		Out.println(res.body());

		Elements els = res.parse().select("html head meta[http-equiv=refresh]");
		if (els.isEmpty())
			return null;
		String redirect_url = els.first().attr("content");
		redirect_url = redirect_url.substring(8, redirect_url.length() - 1);
		res = Jsoup.connect(redirect_url).cookies(res.cookies())
				.followRedirects(true).execute();
		Out.println(res.cookies().toString());
		return res.cookies();
	}

	public List<String> getAllFollows(String uid, int folNum) {
		Out.println(uid + " => " + folNum);
		try {
			Set<String> fol = new HashSet<String>();
			int pages = folNum / 20 + 1;
			for (int i = 1; i <= pages; i++) {
				String url = "http://gov.weibo.com/attention/attsList.php"
						+ "?action=1&uid=" + uid + "&page=" + i;
				Response res = Jsoup.connect(url).cookies(getAccount().COOKIES)
						.followRedirects(true).execute();
				if (!res.url().toString().startsWith("http://gov.weibo.com"))
					return null;
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
			Document doc = getHtmlDocument(url);
			Elements counters = doc.select("table.tb_counter strong");
			if (!doc.select("div.page_error").isEmpty()) {
				handler.userNotAvailable(uid);
				return null;
			}
			if (counters.isEmpty()) {
				Out.println(uid + " => Incomplete index page.");
				return null;
			}
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
			uip.VERIFIED = !doc.select(
					"div.verify_area a[class~=icon_verify_(co_)?v]").isEmpty();
			Out.println(uip.toString());
			return uip;
		} catch (IOException | JSONException e) {
			Out.println(e.getMessage() + " => " + uid);
		}
		return null;
	}

	public Document getHtmlDocument(String url) throws IOException,
			JSONException {
		WeiboAccount acc = getAccount();
		if (acc == null) {
			Out.println("No available account for crawling.");
			System.exit(0);
		}
		Response res = Jsoup.connect(url).cookies(acc.COOKIES)
				.followRedirects(true).execute();
		Document doc = getFullHtml(res.parse());
		String redirected_url = res.url().toString();
		if (redirected_url.contains("http://sass.weibo")
				|| redirected_url.contains("sorry?userblock")) {
			handler.freezeException(acc.UN);
			return null;
		} else if (redirected_url.contains("/signup/signup.php")
				|| redirected_url.contains("login.php")
				|| redirected_url.contains("http://passport")) {
			acc.COOKIES = login(acc.UN, acc.PSWD);
			return null;
		} else if (!doc.select("div.veriyfycode").isEmpty()) {
			handler.verifycodeException(acc.UN);
			return null;
		} else if (!doc.select("div#pl_common_unloginbase").isEmpty()) {
			acc.COOKIES = login(acc.UN, acc.PSWD);
		}
		return doc;
	}

	public Document getFullHtml(Document doc) throws JSONException {
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

	public synchronized WeiboAccount getAccount() throws IOException,
			JSONException {
		if (accounts.size() == 0)
			return null;
		if (count >= accounts.size())
			count = 0;
		String account = uns.get(count++);
		Out.println(count + " / " + uns.size() + " => " + account);
		WeiboAccount acc = accounts.get(account);
		if(acc.COOKIES == null) acc.COOKIES = login(acc.UN, acc.PSWD);
		acc.REQUEST_COUNTER++;
		return acc;
	}
}
