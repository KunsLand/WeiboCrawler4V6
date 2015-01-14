package common;

import java.util.Date;

public class Out {
	public static void println(String str) {
		System.out.println(Thread.currentThread().getName() + "["
				+ TimeUtils.format(new Date(System.currentTimeMillis()))
				+ "]:\t" + str);
	}
}
