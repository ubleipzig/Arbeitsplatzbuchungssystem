package org.ub.dev.tools;

import java.util.Calendar;
import java.util.Date;

public class Tools {

    public static Calendar setCalendarOnDate(int day, int month, int year) {
        Calendar calX = Calendar.getInstance();
        calX.set(Calendar.HOUR_OF_DAY, 0);
        calX.set(Calendar.MINUTE, 0);
        calX.set(Calendar.SECOND, 0);
        calX.set(Calendar.DAY_OF_MONTH, day);
        calX.set(Calendar.MONTH, month-1);
        calX.set(Calendar.YEAR, year);

        return calX;
    }

    public static Date getDateFromms(long ms) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ms);

        return c.getTime();
    }

    public static Calendar setCalendarOnComplete(int day, int month, int year, int hour, int minute) {
        Calendar calX = Calendar.getInstance();
        calX.set(Calendar.HOUR_OF_DAY, hour);
        calX.set(Calendar.MINUTE, minute);
        calX.set(Calendar.SECOND, 0);
        calX.set(Calendar.DAY_OF_MONTH, day);
        calX.set(Calendar.MONTH, month-1);
        calX.set(Calendar.YEAR, year);

        return calX;
    }

    public static String getStringFromCal(Calendar cal) {
        String hour = cal.get(Calendar.HOUR_OF_DAY)<=9 ? "0"+cal.get(Calendar.HOUR_OF_DAY) : String.valueOf(cal.get(Calendar.HOUR_OF_DAY));
        String minute = cal.get(Calendar.MINUTE)<=9 ? "0"+cal.get(Calendar.MINUTE) : String.valueOf(cal.get(Calendar.MINUTE));
        String day = cal.get(Calendar.DAY_OF_MONTH)<=9 ? "0"+cal.get(Calendar.DAY_OF_MONTH) : String.valueOf(cal.get(Calendar.DAY_OF_MONTH));
        String month = (cal.get(Calendar.MONTH)+1)<=9 ? "0"+(cal.get(Calendar.MONTH)+1) : String.valueOf(cal.get(Calendar.MONTH)+1);
        String year = String.valueOf(cal.get(Calendar.YEAR));

        return day+"."+month+"."+year+" "+hour+":"+minute;
    }

}
