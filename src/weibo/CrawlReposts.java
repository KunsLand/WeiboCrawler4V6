package weibo;

import java.sql.SQLException;
import java.util.Map;
import java.util.Set;

import common.Out;
import weibo.client.WeiboClient4Microblog;
import weibo.database.AccountDB;
import weibo.database.MicroblogDB;
import weibo.objects.WeiboAccount;

public class CrawlReposts {

	public static void main(String[] args) throws SQLException{
		AccountDB adb = new AccountDB();
		MicroblogDB mdb = new MicroblogDB(); 
		Set<String> mids = mdb.getMids4CrawlingReposts();
		Map<String, WeiboAccount> accs = adb.getAvailableWeiboAccounts();
		WeiboClient4Microblog weiboClient = new WeiboClient4Microblog(accs);
		weiboClient.setAccountExceptionHandler(adb);
		int count = 0;
		for(String mid: mids){
			count++;
			if(count%100==0) Out.println(count+"/"+mids.size());
			mdb.updateMicroblogRelations(mid, weiboClient.getRepostMids(mid));
		}
	}
}
