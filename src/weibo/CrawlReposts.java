package weibo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import common.Out;
import common.TimeUtils;
import weibo.client.AccountManager;
import weibo.client.AccountQueue;
import weibo.client.CookieStorage;
import weibo.client.RequestConfig;
import weibo.client.WeiboClient;
import weibo.database.AccountDB;
import weibo.database.MicroblogDB;

public class CrawlReposts {

	public static void main(String[] args) {
		CookieStorage adb = new AccountDB();
		MicroblogDB mdb = new MicroblogDB();
		List<String> mids = new ArrayList<String>(mdb.getMids4CrawlingReposts());
		AccountManager am = new AccountQueue(adb);
		WeiboClient weiboClient = new WeiboClient(am);

		ExecutorService es = Executors.newFixedThreadPool(10);
		int count = 0;
		Out.println(mids.size() + "");
		for (String mid : mids) {
			int n = ++count;
			es.execute(new Runnable() {
				@Override
				public void run() {
					mdb.updateMicroblogRelations(mid,
							weiboClient.getRepostMids(mid));
					TimeUtils.Pause(RequestConfig.TIME_REQUEST_GAP);
					if (n % 100 == 0) {
						Out.println(n + "/" + mids.size());
					}
				}
			});
		}
		es.shutdown();
	}
}
