package com.graph.utils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.graph.service.wt.AdvanceGraphService;

public class DateUtil {
    
    public static final DateTimeFormatter FORMATTER_FULL = DateTimeFormatter.ofPattern("E MMM dd  HH:mm:ss z yyyy ", Locale.ENGLISH);
    public static final DateTimeFormatter FORMATTER_YYYY_MM_DD = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final DateTimeFormatter FORMATTER_YYYY_MM_DD_SLASH = DateTimeFormatter.ofPattern("YYYY/MM/dd");
	public static final Logger logger = LoggerFactory.getLogger(DateUtil.class);


	public static Date convertStringToDate(String time) {
		SimpleDateFormat inSDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date outDate = null;	
		try {
			outDate = inSDF.parse(time);
		} catch (ParseException e) {
			logger.error("Convert String To Date Error : " + e);
		}
		return outDate;

	}

	public static Date convertStringToDateTimeStamp(String time) throws ParseException {
	
	String a = time.substring(0, time.length() - 5);

		DateTimeFormatter formatter3 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");
		ZonedDateTime dt = ZonedDateTime.parse(time, formatter3);
		DateFormat utcFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		utcFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		Date date = utcFormat.parse(a);
		Date outDate = null;
		try {
		DateFormat pstFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		pstFormat.setTimeZone(TimeZone.getTimeZone("GMT" + dt.getZone().toString()));
		outDate = utcFormat.parse(pstFormat.format(date));
		}
		catch (ParseException e) {
			logger.error("Convert String To Date TimeStamp Error : " + e);
		}
		
		return outDate;
		
	}
	
	public static Date getCurrentDateInUTC() {
		final SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
		f.setTimeZone(TimeZone.getTimeZone("UTC"));
		Date outDate = null;
		try {
			outDate = f.parse(f.format(new Date()));
		} catch (ParseException e) {
			logger.error("Get CurrentDate In UTC Error: " + e);
		}
		return outDate;
	}

	public static Date convertStringToDateOffset(String time) {

		try {
		DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter
		.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

		String dateString = time;

		//Instance with given offset
		OffsetDateTime odtInstanceAtOffset = OffsetDateTime.parse(dateString, DATE_TIME_FORMATTER);
		return Date.from(odtInstanceAtOffset.toInstant());
		}
		catch(Exception e) {

		}
		return null;

		}
}

