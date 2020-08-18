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

}
