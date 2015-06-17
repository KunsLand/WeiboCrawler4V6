package weibo;

import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import weibo.client.AccountManager;
import weibo.client.AccountQueue;
import weibo.client.CookieStorage;
import weibo.client.WeiboClient;
import weibo.database.AccountDB;
import weibo.database.UserRelationDB;

public class CrawlFollows {

	public static void main(String[] args) throws SQLException {
		UserRelationDB mysql = new UserRelationDB();
		CookieStorage accDB = new AccountDB();
		AccountManager am = new AccountQueue(accDB);
		WeiboClient weiboClient = new WeiboClient(am);

		ExecutorService es = Executors.newFixedThreadPool(10);
		Map<String, Integer> pairs = mysql.getFollowsUncrawledPairs();
		for (Map.Entry<String, Integer> entry : pairs.entrySet()) {
			int pages = entry.getValue() / 20 + 1;
			String uid = entry.getKey();
			for (int page = 1; page <= pages; page++) {
				int current = page;
				es.execute(new Runnable() {
					@Override
					public void run() {
						mysql.stroreRelations(uid, weiboClient
								.getFollowsFromAttsListPage(uid, current));
						if (current == pages) {
							mysql.markFollowsCrawled(uid);
						}
					}
				});
			}
		}
	}

}
