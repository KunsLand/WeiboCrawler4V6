package weibo.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import common.Out;
import common.TimeUtils;
import weibo.objects.WeiboAccount;

public class WeiboClient4Microblog extends WeiboClient {

	public WeiboClient4Microblog(Map<String, WeiboAccount> accounts) {
		super(accounts);
	}

	public List<String> getRepostMids(String mid) {
		Out.println(mid);
		List<String> mids = new ArrayList<String>();
		WeiboAccount acc = null;
		Document doc = null;
		String url = "http://weibo.com/aj/v6/mblog/info/big?ajwvr=6&id=" + mid
				+ "&page=1";
		while (true) {
			int count = 0;
			do {
				try {
					acc = getAccount();
					doc = getAjaxHtml(acc, url);
				} catch (IOException | JSONException e) {
					Out.println(e.getMessage());
					TimeUtils.PauseOneMinute();
				}
			} while (doc == null && count < 5);
			if (doc == null)
				return mids;
			Elements lists = doc.select("div[action-type=feed_list_item]");
			for (Element item : lists) {
				mids.add(item.attr("mid"));
			}
			Elements next = doc.select("a.next");
			if (next == null || next.isEmpty())
				return mids;
			url = "http://weibo.com/aj/v6/mblog/info/big?ajwvr=6&"
					+ next.select("span").first().attr("action-data");
		}
	}
}
