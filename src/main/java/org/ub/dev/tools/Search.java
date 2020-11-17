package org.ub.dev.tools;

import org.ub.dev.libero.LiberoManager;
import org.ub.dev.sql.SQLHub;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Properties;

/**
 * org.ub.dev.tools.Search - Klasse für die Nachverfolgung von Infektionen
 *
 */
public class Search {


    boolean searchInDB = true;
    String pathtodb = ""; //work is still in progress

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

    /**
     * Rückgabe von NutzerInnen die eine Infektion gemeldet haben und den NuterInnen, die um die Plätze verteilt saßen.
     *
     * @param institution
     * @param seats
     * @param day
     * @param infectedreadernumber
     * @param token
     * @return
     */
    public String request(String institution, String seats, String day, String infectedreadernumber, String token) {

        String protocol = new String();
        String day_start = day+" 00:00:00";
        String day_end = day+" 23:59:59";

        SQLHub sqlHub = new SQLHub(p);
        ArrayList<HashMap<String, Object>> map2 = sqlHub.getMultiData("select * from booking where start between '"+day_start+"' and '"+day_end+"' and institution='"+institution+"' and readernumber = '"+infectedreadernumber+"'","bookingservice");

        LiberoManager lm = new LiberoManager(p);

        for(HashMap<String, Object> entry:map2) {
            Timestamp from = (Timestamp)entry.get("start");
            Timestamp until = (Timestamp)entry.get("end");
            int workspaceId = (int)entry.get("workspaceId");
            protocol+="NutzerIn auf Platz Nr. "+workspaceId+" von "+from.toString().substring(0, from.toString().length()-5)+" bis "+until.toString().substring(0, until.toString().length()-5)+"\n\n";

            String mailadresses = "";

            if(seats.length()>0) {

                ArrayList<HashMap<String, Object>> map = sqlHub.getMultiData("select * from booking where start between '" + from + "' and '" + until + "' and institution='" + institution + "' and workspaceId in (" + seats + ")", "bookingservice");
                for (HashMap<String, Object> entry2 : map) {
                    protocol += (String) entry2.get("readernumber") + " Buchungsbeginn: " + ((Timestamp)entry2.get("start")).toString().substring(0, ((Timestamp)entry2.get("start")).toString().length()-5) + " | Buchungsende: " + ((Timestamp)entry2.get("end")).toString().substring(0,((Timestamp)entry2.get("end")).toString().length()-5) + " auf Arbeitsplatz Nr. " + entry2.get("workspaceId") + "\n";
                    if (token != null) {
                        try {
                            mailadresses += lm.getMailAdress((String) entry2.get("readernumber"), token) + ",";
                        } catch (Exception e) {
                        }
                    }
                }
            }

            if(token!=null)
                if(mailadresses.length()>=1)
                    protocol+="\n"+mailadresses.substring(0, mailadresses.length()-1);

            protocol+="\n\n";
        }

        return protocol;
    }

}
