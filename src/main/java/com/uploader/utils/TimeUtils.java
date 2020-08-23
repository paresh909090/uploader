package com.uploader.utils;

import java.sql.Timestamp;

import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;

public class TimeUtils {
	
	public static long getCurrentUTCTimeInMilliseconds() {
		LocalDateTime date = LocalDateTime.now();
		DateTimeZone tz = DateTimeZone.getDefault();
		Timestamp ts = new Timestamp(date.toDateTime(tz).toDateTime(DateTimeZone.UTC).getMillis());
		return ts.getTime();
	}
	
	public static String getCurrentDateFormatted() {
		LocalDateTime today = LocalDateTime.now();

		return today.toString("MMddyy_HHmmss");
	}
}