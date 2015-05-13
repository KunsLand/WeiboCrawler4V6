package weibo.interfaces;

import weibo.WeiboAccount;

public interface AccountExceptionHandler {
	
	public void verifycodeException(String account);
	
	public void freezeException(String account);
	
	public void updateCookie(WeiboAccount account);

}
