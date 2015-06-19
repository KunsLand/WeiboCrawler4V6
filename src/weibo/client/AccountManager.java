package weibo.client;

public interface AccountManager {

	public boolean refreshCookie(WeiboAccount account);
	
	public boolean unfreezeAccount(WeiboAccount account);

	public WeiboAccount getNextAccount();

	public WeiboAccount peekAccount();

	public boolean removeAccount(WeiboAccount account);

	public boolean isEmpty();

}
