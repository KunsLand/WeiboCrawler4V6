package weibo;

public interface ExceptionHandler {
	
	public void userNotAvailable(String uid);
	
	public void verifycodeException(String account);
	
	public void freezeException(String account);
	
	public void updateCookie(WeiboAccount account);
	
	public void enterpriseUser(String uid);

}
