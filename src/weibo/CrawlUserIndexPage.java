package weibo;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONException;

import weibo.client.AccountManager;
import weibo.client.VisitorQueue;
import weibo.client.WeiboClient;
import weibo.database.UserDB;
import weibo.objects.UserIndexPage;
import common.Out;

public class CrawlUserIndexPage {
	public static void main(String[] args) throws SQLException, JSONException,
			IOException {
		final UserDB mysql = new UserDB();
		List<String> uids = mysql.getIndexPageNotCrawledUIDs();
		AccountManager am = new VisitorQueue(10);
		WeiboClient weiboClient = new WeiboClient(am);

		ExecutorService es = Executors.newFixedThreadPool(10);
		List<UserIndexPage> uips = new ArrayList<UserIndexPage>();
		Object lock = new Object();
		for (String uid : uids) {
			es.execute(new Runnable() {
				@Override
				public void run() {
					Out.println("uid => " + uid);
					UserIndexPage uip = weiboClient.getUserIndexPageInfo(uid);
					if (uid != null) {
						synchronized (lock) {
							uips.add(uip);
							if (uips.size() >= 50) {
								mysql.updateUserIndexPageTable(uips);
								uips.clear();
							}
						}
					}
				}
			});
		}
		if (uips.size() > 0)
			mysql.updateUserIndexPageTable(uips);
	}

}
