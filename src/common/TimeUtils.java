package common;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TimeUtils {

	public static SimpleDateFormat TIME_CHN = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm:ss");

	public static SimpleDateFormat TIME_CHN_DATE = new SimpleDateFormat(
			"yyyy-MM-dd");

	public static SimpleDateFormat TIME_CHN_HOUR = new SimpleDateFormat(
			"yyyy-MM-dd-HH");

	public static SimpleDateFormat TIME_CHN_MINUTE = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm");

	private static String format(SimpleDateFormat sdf, Date date) {
		return sdf.format(date);
	}
	
	public static String format(Date date) {
		return format(TIME_CHN, date);
	}

	public static String format2Date(Date date) {
		return format(TIME_CHN_DATE, date);
	}

	public static String format2Hour(Date date) {
		return format(TIME_CHN_HOUR, date);
	}

	public static String format2Minute(Date date) {
		return format(TIME_CHN_MINUTE, date);
	}

	public static Date parse(SimpleDateFormat sdf, String str)
			throws ParseException {
		return sdf.parse(str);
	}

	public static void PauseOneMinute() {
		try {
			Out.println("WATING for one minute...");
			Thread.sleep(60 * 1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static void Pause(int minutes) {
		if (minutes <= 0)
			return;
		try {
			Out.println("WATING for " + minutes + " minute(s)...");
			Thread.sleep(minutes * 60 * 1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
