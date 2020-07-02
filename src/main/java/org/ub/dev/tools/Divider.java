package org.ub.dev.tools;

import org.ub.dev.sql.SQLHub;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

/**
 * Divider - Arbeitsplatzbuchungssystem
 *
 * Stand-Alone-Klasse zur Extraktion und DB-Cleanup von Buchungen
 *
 */

public class Divider {

    Properties p;

    public Divider() {

        init();
    }

    private void init() {

        p = new Properties();
        try {
            p.load(new FileInputStream("config/config.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        String timestamp = p.getProperty("timestamp_limit");

        String filename = timestamp.replaceAll("-","").replaceAll(":","").replaceAll(" ", "")+".db";

        if(new File(filename).exists()) System.exit(0);

        try {
            FileOutputStream fos = new FileOutputStream(filename);

            SQLHub hub = new SQLHub(p);
            ArrayList<HashMap<String, Object>> list = hub.getMultiData("select * from booking where end <= '"+timestamp+"'","bookingservice");
            for(HashMap<String, Object> entry:list) {
                String data = new String();
                for(String key:entry.keySet())
                    data+=entry.get(key)+",";

                data = data.substring(0, data.length()-1);
                data+="\n";
                fos.write(data.getBytes());
            }

            hub.executeData("delete from booking where end <= '"+timestamp+"'", "bookingservice");

            System.out.println(list.size()+" Datasets found and saved. Database clean up complete.");

            fos.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void main(String args[]) {

        new Divider();
    }

}
