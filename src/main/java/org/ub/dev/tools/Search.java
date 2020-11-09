package org.ub.dev.tools;

import org.ub.dev.sql.SQLHub;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Properties;

public class Search {


    boolean searchInDB = true;
    String pathtodb = "";

    Properties p;

    public Search() {

        init();

    }


    private void init() {

        p = new Properties();
        try {
            p.load(new FileInputStream("config/config.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public String request(String institution, String seats, String day, String infectedreadernumber) {

        String protocol = new String();
        String day_start = day+" 00:00:00";
        String day_end = day+" 23:59:59";

        SQLHub sqlHub = new SQLHub(p);
        ArrayList<HashMap<String, Object>> map2 = sqlHub.getMultiData("select * from booking where start between '"+day_start+"' and '"+day_end+"' and institution='"+institution+"' and readernumber = '"+infectedreadernumber+"'","bookingservice");

        for(HashMap<String, Object> entry:map2) {
            Timestamp from = (Timestamp)entry.get("start");
            Timestamp until = (Timestamp)entry.get("end");
            int workspaceId = (int)entry.get("workspaceId");
            protocol+="NutzerIn auf Platz Nr. "+workspaceId+" von "+from+" bis "+until+"\n";

            ArrayList<HashMap<String, Object>> map = sqlHub.getMultiData("select * from booking where start between '"+from+"' and '"+until+"' and institution='"+institution+"' and workspaceId in ("+seats+")","bookingservice");
            for(HashMap<String, Object> entry2:map) {
                protocol+=(String)entry2.get("readernumber")+" Buchungsbeginn: "+entry2.get("start")+" | Buchungsende: "+entry2.get("end")+" auf Arbeitsplatz Nr. "+entry2.get("workspaceId")+"\n";
            }
        }

        return protocol;
    }

    public static void main(String args[]) {

        Search search = new Search();
        String institution = "Bibliotheca Albertina";
        String seats = "1,2,3,4,5,6,7,8";
        String day = "2020-11-05";
        String infectedreadernumber = "207227-5";

        System.out.println(search.request(institution, seats, day, infectedreadernumber));
    }
}
