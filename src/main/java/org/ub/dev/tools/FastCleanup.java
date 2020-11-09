package org.ub.dev.tools;

import de.unileipzig.ub.libero6wsclient.Wachtl;
import org.ub.dev.libero.LiberoManager;
import org.ub.dev.sql.SQLHub;

import java.io.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Properties;

public class FastCleanup {

    public FastCleanup() {
        init();
    }

    private void init() {

        Properties p = new Properties();
        try {
            p.load(new FileInputStream("config/config.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        SQLHub hub = new SQLHub(p);
        ArrayList<HashMap<String, Object>> maplist = hub.getMultiData("select * from booking where start >= '2020-11-05 00:00:00'","bookingservice");

        SQLHub hub2 = new SQLHub(p);

        Calendar cal = Tools.setCalendarOnDate(5,11,2020);
        long cal_long = cal.getTimeInMillis();

        LiberoManager lm = new LiberoManager(p);

        //username and password has to fit in!!!!
        String token = lm.malogin("?","?")[0];

        int c = 0, d=0;
        ArrayList<String> resultlist = new ArrayList<>();

        Wachtl wachtl = new Wachtl();
        wachtl.setLibraryServiceURL(p.getProperty("library_url"));

        System.out.println(maplist.size());

        for(HashMap<String, Object> entry:maplist) {

                String readernumber = (String)entry.get("readernumber");
                String bookingCode = (String)entry.get("bookingCode");

                System.out.println("Counter:"+d++);

                try{
                    String category = lm.isWalkin(readernumber, token, wachtl);

                    if(category.equals("N")||category.equals("S")) {
                        System.out.println("Will be removed: "+readernumber);
                        hub2.executeData("delete from booking where bookingCode = '"+bookingCode+"' and readernumber = '"+readernumber+"'","bookingservice");

                        if(!resultlist.contains(readernumber)) resultlist.add(readernumber);

                        c++;
                    }
                }catch(Exception e){}

        }

        System.out.println(c);

        try {
            FileOutputStream fos = new FileOutputStream(new File("result.txt"));
            for(String rdn: resultlist)
                fos.write((rdn+",").getBytes());
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        lm.close(token);

    }


    public static void main(String args[]) {

        new FastCleanup();

    }

}
