package weibo.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.Connection.Response;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import weibo.objects.UserIndexPage;
import common.Out;
import common.TimeUtils;

public class WeiboClient {
	private AccountManager accountManager;

	public WeiboClient(AccountManager accountManager) {
		this.accountManager = accountManager;
	}

	private void checkAccounts() {
		if (accountManager.isEmpty()) {
			Out.println("No cookie available.");
			System.exit(0);
		}
	}

	private Document getFullHTMLDocument(Document doc) {
		if (doc == null)
			return null;
		Elements scripts = doc.select("script");
		StringBuilder sb = new StringBuilder(doc.body().toString());
		for (Element script : scripts) {
			String json = script.toString();
			if (!json.contains("\"html\":\""))
				continue;
			json = json.substring(json.indexOf("{"), json.lastIndexOf("}") + 1);

			try {
				JSONObject obj = new JSONObject(json);
				sb.append(obj.get("html"));
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return Jsoup.parse(sb.toString());
	}

	public Response getResponse(String url) {
		Response res = null;
		while (true) {
			checkAccounts();
			WeiboAccount account = accountManager.getNextAccount();
			try {
				res = Jsoup.connect(url).cookies(account.COOKIES)
						.timeout(RequestConfig.TIME_REQUEST_OUT)
						.ignoreContentType(true).followRedirects(true)
						.execute();
				String redirected_url = res.url().toString();
				if (redirected_url.matches(".*signup.*|.*login.*|.*passport.*")) {
					Out.println("Cookie is invalidated: " + account.USERNAME);
					accountManager.refreshCookie(account);
				} else if (redirected_url.matches(".*userblock.*")) {
					Out.println("Redirected FROM " + url + " TO "
							+ redirected_url);
					// Out.println("Blocked: " + account.USERNAME);
					// accountManager.removeAccount(account);
				} else if (res.body().contains("veriyfycode")) {
					Out.println("Return page contains verifycode when using cookie: "
							+ account.USERNAME
							+ " where url = "
							+ url
							+ " and redirected to " + redirected_url);
				} else if (redirected_url.matches(".*pagenotfound.*")) {
					Out.println("Page not found: " + url + ", redirected to " + redirected_url);
					break;
				} else if (redirected_url.contains("10.3.8.211")) {
					Out.println("Network Expired. Redirected url is " + redirected_url);
					System.exit(0);
				} else if (redirected_url.matches(".*usernotexists.*")) {
					Out.println("User not exists: " + url + ", redirected to " + redirected_url);
					break;
				} else {
					TimeUtils.Pause(RequestConfig.TIME_REQUEST_GAP);
					break;
				}
			} catch (IOException e) {
				Out.println(e.getMessage());
				TimeUtils.Pause(RequestConfig.TIME_REQUEST_ERORR);
			}
		}
		return res;
	}

	public List<String> getFollowsFromAttsListPage(String uid, int page) {
		String url = "http://gov.weibo.com/attention/attsList.php"
				+ "?action=1&uid=" + uid + "&page=" + page;
		Document doc = null;
		try {
			doc = getResponse(url).parse();
		} catch (IOException e) {
			e.printStackTrace();
		}
		List<String> follows = new ArrayList<String>();
		if (doc == null) {
			return follows;
		}
		Elements el = doc.select("a[namecard=true]");
		for (Element e : el)
			follows.add(e.attr("uid"));
		return follows;
	}

	public List<String> getRepostMids(String mid) {
		Out.println("Reposts of mid: " + mid);
		List<String> mids = new ArrayList<String>();
		String json = null;
		String url = "http://weibo.com/aj/v6/mblog/info/big?ajwvr=6&id=" + mid
				+ "&page=1&__rnd=" + System.currentTimeMillis();
		while (true) {
			json = getResponse(url).body();
			Document doc = null;
			try {
				JSONObject obj = new JSONObject(json);
				doc = Jsoup.parse(obj.getJSONObject("data").getString("html"));
			} catch (JSONException e) {
				Out.println(e.getMessage() + ": " + url);
			}
			if (doc == null)
				break;
			Elements lists = doc.select("div[action-type=feed_list_item]");
			for (Element item : lists) {
				mids.add(item.attr("mid"));
			}
			Elements next = doc.select("a.next");
			if (next == null || next.isEmpty())
				break;
			url = "http://weibo.com/aj/v6/mblog/info/big?ajwvr=6&"
					+ next.select("span").first().attr("action-data");
		}
		Out.println("Reposts of mid: " + mid + ": " + mids.size());
		return mids;
	}

	public UserIndexPage getUserIndexPageInfo(String uid) {
		UserIndexPage info = null;
		String url = "http://www.weibo.com/u/" + uid;
		Document doc = null;
		try {
			doc = getResponse(url).parse();
			doc = getFullHTMLDocument(doc);
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (doc == null)
			return info;
		Elements counters = doc.select("table.tb_counter strong");
		if (counters.isEmpty())
			return info;

		info = new UserIndexPage();
		info.UID = uid;
		info.FOLLOWS = Integer.valueOf(counters.get(0).ownText());
		info.FANS = Integer.valueOf(counters.get(1).ownText());
		info.BLOGS = Integer.valueOf(counters.get(2).ownText());
		info.NICK_NAME = doc.select("span.username").text();
		Elements posts = doc.select("div[action-type="
				+ "feed_list_item]:not([feedtype=top])"
				+ " div.WB_detail > div[class=WB_from S_txt2] a");
		info.LAST_POST = posts.isEmpty() ? null : posts.first().attr("title");
		boolean v1 = !doc.select("div.verify_area a[class~=icon_verify_v]")
				.isEmpty(), v2 = !doc.select(
				"div.verify_area a[class~=icon_verify_co_v]").isEmpty();
		if (v1)
			info.VERIFIED = 1;
		else if (v2)
			info.VERIFIED = 2;
		else
			info.VERIFIED = 0;
		return info;
	}
}
