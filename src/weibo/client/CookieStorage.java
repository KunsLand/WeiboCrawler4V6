package weibo.client;

import java.util.List;

public interface CookieStorage {

	public WeiboAccount getAccount();

	public List<WeiboAccount> getAccountList();

	public void updateCookie(WeiboAccount account);

	public void invalidateCookie(WeiboAccount account);
	
	public void unfreezeAccount(WeiboAccount account);

	public void banAccount(WeiboAccount account);
}
