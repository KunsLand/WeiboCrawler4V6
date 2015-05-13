package weibo.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONException;
import org.jsoup.Jsoup;
import org.jsoup.Connection.Response;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import weibo.objects.WeiboAccount;
import common.Out;
import common.TimeUtils;

public class WeiboClient4Follows extends WeiboClient {

	public WeiboClient4Follows(Map<String, WeiboAccount> accounts) {
		super(accounts);
	}
	public List<String> getAllFollows(String uid, int folNum) {
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
}
