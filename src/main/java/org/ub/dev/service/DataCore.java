package org.ub.dev.service;

import org.ub.dev.sql.SQLHub;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

/**
 * DataCore - Arbeitsplatzbuchungssystem
 *
 * Generiert CSV-Daten aus der DB anhand des Standortes und dem zeitlichen Bezugsintervall.
 */

public class DataCore {

    String institution = new String();
    String from, until;
    Properties p;

    /**
     * Konstruktor, erwartet die Bibliothek, Start- und Endzeitpunkt
     *
     * @param institution
     * @param from
     * @param until
     * @param p
     */
    public DataCore(String institution, String from, String until, Properties p) {
        this.institution = institution;
        this.from = from;
        this.until = until;
        this.p = p;
    }

    /**
     * Ruft die Daten aus der DB ab und erzeugt CSV
     *
     * @return
     */
    public String getData() {

        String result = new String();

        SQLHub hub = new SQLHub(p);
        ArrayList<HashMap<String, Object>> list = hub.getMultiData("select * from booking where institution = '"+institution+"' and start >= '"+from+"' and end <= '"+until+"' order by start", "bookingservice");

        boolean headerset = false;

        for(HashMap<String, Object> entry:list) {

            if(!headerset) {
                for(String key:entry.keySet()) {
                    result+=key+",";
                }
                result = result.substring(0, result.length()-1)+"\n";
                headerset = true;
            }

            String entryline = new String();
            for(String key:entry.keySet()) {
                entryline+=entry.get(key)+",";
            }
            entryline=entryline.substring(0,entryline.length()-1)+"\n";
            result+=entryline;

        }

        return result;
    }

}
