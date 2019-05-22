package org.jxnu.stu.util;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.jxnu.stu.common.BusinessException;
import org.jxnu.stu.common.ReturnCode;

import java.util.Date;

public class DateTimeHelper {

    public static final String STANDARD_PATTERN = "yyyy-MM-dd HH:mm:ss";

    public static Date strToDate(String str) throws BusinessException {
        if(str == null){
            return null;
        }
        DateTimeFormatter dateTimeFormat = DateTimeFormat.forPattern(STANDARD_PATTERN);
        DateTime dateTime = dateTimeFormat.parseDateTime(str);
        return dateTime.toDate();
    }

    public static Date strToDate(String str,String pattern) throws BusinessException {
        if(str == null){
            return null;
        }
        DateTimeFormatter dateTimeFormat = DateTimeFormat.forPattern(pattern);
        DateTime dateTime = dateTimeFormat.parseDateTime(str);
        return dateTime.toDate();
    }

    public static String dateToString(Date date) throws BusinessException {
        if(date == null){
            return null;
        }
        DateTime dateTime = new DateTime(date);
        return dateTime.toString(STANDARD_PATTERN);
    }

    public static String dateToString(Date date,String pattern) throws BusinessException {
        if(date == null){
            return null;
        }
        DateTime dateTime = new DateTime(date);
        return dateTime.toString(pattern);
    }

}
