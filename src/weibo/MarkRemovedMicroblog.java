package weibo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;

import common.Out;
import common.TimeUtils;
import weibo.client.AccountManager;
import weibo.client.RequestConfig;
import weibo.client.VisitorQueue;
import weibo.client.WeiboAccount;
import weibo.database.MicroblogDB;

public class MarkRemovedMicroblog {

	public static void main(String[] args) {
		MicroblogDB mdb = new MicroblogDB();
		List<String> urls = new ArrayList<String>(mdb.getURLs4CrawlingReposts());
		Out.println(urls.size() + "");
		ExecutorService es = Executors.newFixedThreadPool(10);

		int count = 0;
		AccountManager am = new VisitorQueue(10);
		for (String url : urls) {
			int n = ++count;
			es.execute(new Runnable() {
				@Override
				public void run() {
					Out.println(url);
					WeiboAccount account = am.getNextAccount();
					try {
						if (am.isEmpty())
							return;
						Response res = Jsoup.connect(url)
								.cookies(account.COOKIES)
								.timeout(RequestConfig.TIME_REQUEST_OUT)
								.followRedirects(true).execute();
						Out.println(res.url().toString());
						if (res.body().contains(
								"http://weibo.com/sorry?pagenotfound")) {
							mdb.pageNotFound(url);
						} else {
							mdb.pageAvailable(url);
						}
						TimeUtils.Pause(RequestConfig.TIME_REQUEST_GAP);
					} catch (IOException e) {
						Out.println(e.getMessage());
						am.refreshCookie(account);
						TimeUtils.Pause(RequestConfig.TIME_REQUEST_ERORR);
					}
					if (n % 100 == 0) {
						Out.println(n + "/" + urls.size());
					}
				}
			});
		}
	}

}
