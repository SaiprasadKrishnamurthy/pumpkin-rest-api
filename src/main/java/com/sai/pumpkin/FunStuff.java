package com.sai.pumpkin;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by saipkri on 21/03/17.
 */
public class FunStuff {

    public static void main(String[] args) throws Exception {


        long l = 1490434174000L;
        TimeZone tz = TimeZone.getDefault();
        int offsetGmtToPst = tz.getOffset(Calendar.ZONE_OFFSET);
        long adjustedTime = l - offsetGmtToPst;
        System.out.println(new Date(adjustedTime));
        System.out.println(adjustedTime);







    }
}
