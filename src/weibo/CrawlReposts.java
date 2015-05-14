package weibo;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import common.Out;
import weibo.client.WeiboClient4Microblog;
import weibo.database.AccountDB;
import weibo.database.MicroblogDB;
import weibo.objects.WeiboAccount;

public class CrawlReposts {

	public static void main(String[] args) throws SQLException {
		AccountDB adb = new AccountDB();
		MicroblogDB mdb = new MicroblogDB();
		List<String> mids = new ArrayList<String>(mdb.getMids4CrawlingReposts());
		Map<String, WeiboAccount> accs = adb.getAvailableWeiboAccounts();
		WeiboClient4Microblog weiboClient = new WeiboClient4Microblog(accs);
		weiboClient.setAccountExceptionHandler(adb);

		ExecutorService es = Executors.newFixedThreadPool(4);
		for (int i = 0; i < 4; i++) {
			List<String> tmp = mids.subList(i * mids.size() / 4,
					i < 3 ? (i + 1) * mids.size() / 4 : mids.size());
			es.execute(new Runnable() {

				@Override
				public void run() {
					int count = 0;
					for (String mid : tmp) {
						count++;
						if (count % 100 == 0)
							Out.println(count + "/" + tmp.size());
						try {
							mdb.updateMicroblogRelations(mid,
									weiboClient.getRepostMids(mid));
						} catch (SQLException e) {
							Out.println(e.getMessage());
						}
					}
				}
			});
		}
	}
}
