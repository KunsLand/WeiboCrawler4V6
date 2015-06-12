package weibo;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import common.Out;
import common.TimeUtils;
import weibo.client.WeiboClient4Microblog;
import weibo.database.AccountDB;
import weibo.database.MicroblogDB;
import weibo.interfaces.GlobalConfig;
import weibo.objects.WeiboAccount;

public class CrawlReposts {

	public static void main(String[] args) throws SQLException {
		AccountDB adb = new AccountDB();
		MicroblogDB mdb = new MicroblogDB();
		List<String> mids = new ArrayList<String>(mdb.getMids4CrawlingReposts());
		Map<String, WeiboAccount> accs = adb.getAvailableWeiboAccounts();
		WeiboClient4Microblog weiboClient = new WeiboClient4Microblog(accs);
		weiboClient.setAccountExceptionHandler(adb);
		weiboClient.setMicroblogExceptionHandler(mdb);

		ExecutorService es = Executors.newFixedThreadPool(10);
		int count = 0;
		for (String mid : mids) {
			int n = ++count;
			es.execute(new Runnable() {
				@Override
				public void run() {
					try {
						mdb.updateMicroblogRelations(mid,
								weiboClient.getRepostMids(mid));
						TimeUtils.Pause(GlobalConfig.TIME_REQUEST_GAP);
					} catch (SQLException e) {
						Out.println(e.getMessage());
						TimeUtils.Pause(GlobalConfig.TIME_REQUEST_ERORR);
					}
					if (n % 100 == 0) {
						Out.println(n + "/" + mids.size());
					}
				}
			});
		}
	}
}
