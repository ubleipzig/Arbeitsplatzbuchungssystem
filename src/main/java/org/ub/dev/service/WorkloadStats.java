package org.ub.dev.service;

import org.ub.dev.sql.SQLHub;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Properties;

public class WorkloadStats {

    Properties p = new Properties();

    ArrayList<HashMap<String, Object>> workspaces;
    ArrayList<HashMap<String, Object>> bookings;

    public WorkloadStats(Properties p) {
        this.p = p;
        cacheDB();

        Calendar test = Calendar.getInstance();
        test.set(Calendar.DAY_OF_MONTH, 18);
        test.set(Calendar.MONTH, Calendar.JUNE);

        System.out.println(getData("Bibliotheca Albertina", test));
    }

    private void cacheDB() {
        workspaces = new SQLHub(p).getMultiData("select * from workspace","bookingservice");
        bookings = new SQLHub(p).getMultiData("select * from booking","bookingservice");
    }

    private String getData(String inst, Calendar cal) {

        //define checkdates
        Calendar c1 = (Calendar)cal.clone();
        Calendar c2 = (Calendar)cal.clone();
        Calendar c3 = (Calendar)cal.clone();
        Calendar c4 = (Calendar)cal.clone();
        c1.set(Calendar.HOUR_OF_DAY, 8);
        c1.set(Calendar.MINUTE, 0);
        c2.set(Calendar.HOUR_OF_DAY, 12);
        c2.set(Calendar.MINUTE, 0);
        c3.set(Calendar.HOUR_OF_DAY, 16);
        c3.set(Calendar.MINUTE, 0);
        c4.set(Calendar.HOUR_OF_DAY, 20);
        c4.set(Calendar.MINUTE, 0);

        int c_8 = 0, c_12 = 0, c_16 = 0, c_20 = 0;

        for(HashMap workspace:workspaces) {
            if(!workspace.get("institution").equals(inst)) continue;

            int id = (int)workspace.get("id");
            for(HashMap booking:bookings) {
                if((int)booking.get("workspaceId")!=id) continue;
                if(!booking.get("institution").equals(inst)) continue;

                Timestamp ts_start = (Timestamp)booking.get("start");
                Timestamp ts_end = (Timestamp)booking.get("end");

                if(c1.getTimeInMillis()>=ts_start.getTime() && c1.getTimeInMillis()<=ts_end.getTime())
                    c_8++;
                if(c2.getTimeInMillis()>=ts_start.getTime() && c2.getTimeInMillis()<=ts_end.getTime())
                    c_12++;
                if(c3.getTimeInMillis()>=ts_start.getTime() && c3.getTimeInMillis()<=ts_end.getTime())
                    c_16++;
                if(c4.getTimeInMillis()>=ts_start.getTime() && c4.getTimeInMillis()<=ts_end.getTime())
                    c_20++;

            }


        }

        return c_8 +","+ c_12+","+c_16+","+c_20;
    }

    public static void main(String args[]) {
        Properties p = new Properties();
        try {
            p.load(new FileInputStream("config/config.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        new WorkloadStats(p);
    }

}
