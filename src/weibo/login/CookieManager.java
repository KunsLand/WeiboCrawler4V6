package weibo.login;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;

import common.Out;
import common.TimeUtils;

public class CookieManager {
	private Connection conn;
	private String table;
	private Map<String, String> accounts;
	private Map<String, Map<String, String>> cookies;
	private int count = 0;
	private List<String> uns;
	private int timeout = 30 * 1000;

	public CookieManager() {
		conn = null;
		accounts = new HashMap<String, String>();
		cookies = new HashMap<String, Map<String, String>>();
	}

	public CookieManager(String dburl, String table)
			throws SQLException, IOException, JSONException {
		conn = DriverManager.getConnection(dburl);
		this.table = table;
		Statement stmt = conn.createStatement();
		String verifycode_time = TimeUtils.format2Minute(new Date(System
				.currentTimeMillis() - 3600 * 24* 1000));
		String sql = "select account, password, cookie from " + table
				+ " where banned=false and (verifycode_time is null or"
				+ " verifycode_time <= '" + verifycode_time + "')";
		ResultSet result = stmt.executeQuery(sql);
		accounts = new HashMap<String, String>();
		cookies = new HashMap<String, Map<String, String>>();
		while (result.next()) {
			String account = result.getString(1), password = result
					.getString(2), cookieStr = result.getString(3);
			accounts.put(account, password);
			Map<String, String> cookie = cookieString2Map(cookieStr);
			if(cookie == null){ cookie = login(account, password);
				if(cookie == null) {
					Out.println("LOGIN FAILED => " + account);
					if (conn != null) handleVerifycodeException(account);
					continue;
				} else storeCookie(account, cookie.toString());
			}
			cookie.put("un", account);
			cookies.put(account, cookie);
		}
		stmt.close();
		if (accounts.isEmpty()) {
			Out.println("No account for login Sina Weibo.");
			System.exit(0);
		}
		uns = new ArrayList<String>(accounts.keySet());
	}

	private Map<String, String> cookieString2Map(String cookie) {
		if (cookie == null || cookie.isEmpty())
			return null;
		Map<String, String> map = new HashMap<String, String>();
		cookie = cookie.substring(1, cookie.indexOf("}"));
		cookie.replaceAll(" ", "");
		String[] kvs = cookie.split(",");
		for (String s : kvs) {
			String[] kv = s.split("=");
			map.put(kv[0], kv[1]);
		}
		return map;
	}

	public void refreshCookie(String account) throws IOException,
			JSONException, SQLException {
		Out.println("REFRESH => " + account);
		Map<String, String> cookie = login(account, accounts.get(account));
		if (cookie == null) {
			Out.println("REFRESH FAILED => " + account);
			if (conn != null) handleVerifycodeException(account);
			return;
		}
		cookie.put("un", account);
		cookies.put(account, cookie);
		if (conn != null) storeCookie(account, cookie.toString());
	}
	
	public void storeCookie(String account, String cookie) throws SQLException{
		String sql = "update " + table + " set cookie = ? where account = ?";
		PreparedStatement stmt = conn.prepareStatement(sql);
		stmt.setString(1, cookie);
		stmt.setString(2, account);
		stmt.execute();
		stmt.close();
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
		Response res = Jsoup.connect(prelogin_url).timeout(timeout)
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
				.timeout(timeout).referrer("http://weibo.com")
				.cookies(res.cookies()).execute();

		res = Jsoup.connect("http://weibo.com/login.php").timeout(timeout)
				.cookies(res.cookies()).followRedirects(true).execute();

		Elements els = res.parse().select("html head meta[http-equiv=refresh]");
		if (els.isEmpty()) return null;
		String redirect_url = els.first().attr("content");
		redirect_url = redirect_url.substring(8, redirect_url.length() - 1);
		res = Jsoup.connect(redirect_url).timeout(timeout)
				.cookies(res.cookies()).followRedirects(true).execute();
		Out.println(res.cookies().toString());
		return res.cookies();
	}

	public synchronized Map<String, String> getCookie() throws IOException,
			JSONException, SQLException {
		if(uns.size() == 0) return null;
		if (count >= uns.size()) count = 0;
		return cookies.get(uns.get(count++));
	}

	public Map<String, String> getCookie(String account) {
		return cookies.get(account);
	}

	public List<String> getAccounts() {
		return uns;
	}

	public synchronized void handleVerifycodeException(String account)
			throws SQLException {
		Out.println("VERIFYCODE => " + account);
		cookies.remove(account);
		if(uns.contains(account)) uns.remove(account);
		String sql = "update " + table + " set verifycode_time = ?"
				+ " where account = ?";
		PreparedStatement stmt = conn.prepareStatement(sql);
		stmt.setString(1, TimeUtils.format2Minute(new Date()));
		stmt.setString(2, account);
		stmt.execute();
		stmt.close();
	}

	public static void main(String[] args) throws SQLException, IOException,
			JSONException {
		CookieManager cm = new CookieManager(
				"jdbc:mysql://localhost:3306/sinamicroblog?"
						+ "user=root&password=root", "account");
		for (int i = 1; i <= 32; i++) {
			Out.println(cm.getCookie().toString());
		}
	}
}
