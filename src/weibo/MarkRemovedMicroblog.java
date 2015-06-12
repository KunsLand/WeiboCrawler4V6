package weibo;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONException;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;

import common.Out;
import common.TimeUtils;
import weibo.client.WeiboClient;
import weibo.database.MicroblogDB;

public class MarkRemovedMicroblog {

	public static void main(String[] args) throws SQLException, IOException,
			JSONException {
		MicroblogDB mdb = new MicroblogDB();
		List<String> urls = new ArrayList<String>(mdb.getURLs4CrawlingReposts());
		Out.println(urls.size() + "");
		ExecutorService es = Executors.newFixedThreadPool(10);
		for (String url : urls) {
			es.execute(new Runnable() {
				@Override
				public void run() {
					Out.println(url);
					try {
						Map<String, String> cookies = WeiboClient.getVisitorCookie();
						if(cookies.isEmpty()) return;
						Out.println(cookies.toString());
						Response res = Jsoup.connect(url).cookies(cookies)
								.referrer(url).timeout(10*1000)
								.followRedirects(true).execute();
						if (res.body().contains(
								"http://weibo.com/sorry?pagenotfound")) {
							mdb.pageNotFound(url);
						}else{
							mdb.pageAvailable(url);
						}
						Thread.sleep(5000);
					} catch (IOException | JSONException | InterruptedException e) {
						Out.println(e.getMessage());
						TimeUtils.Pause(5);
					}
				}
			});
		}
	}

}
