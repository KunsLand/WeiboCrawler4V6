package common;

import java.util.Date;

public class Out {
	public static void println(Object str) {
		System.out.println("["
				+ TimeUtils.format(new Date(System.currentTimeMillis())) + "] "
				+ Thread.currentThread().getName() + ":\t" + str);
	}
}
