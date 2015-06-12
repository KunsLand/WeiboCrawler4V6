package weibo.client;

import java.io.IOException;

import org.json.JSONException;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import weibo.objects.UserIndexPage;
import weibo.objects.WeiboAccount;
import common.Out;
import common.TimeUtils;

public class WeiboClient4User extends WeiboClient{

	public WeiboClient4User() throws IOException, JSONException {
		super();
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
}
