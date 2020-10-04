package org.ub.dev.tools;

import org.ub.dev.sql.SQLHub;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * DBImporter - Arbeitsplatzbuchungssystem
 *
 * Stellt Funktionalität bereit um aus einer CSV-Datei die Daten in die DB zu überführen
 *
 */
public class DBImporter_RewiII {

    public DBImporter_RewiII() {
        init();
    }

    private void init() {
        Properties p = new Properties();
        try {
            p.load(new FileInputStream("config/config.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        String data = "";

        try {
            FileInputStream fis = new FileInputStream("rewi2bearbeitet.csv");
            byte buffer[] = fis.readAllBytes();
            fis.close();

            data = new String(buffer, "UTF-8");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //System.out.println(data);

        for(String entry:data.split("\n")) {
            String id = entry.split(";")[0].trim();
            String institution = entry.split(";")[1].trim();
            String area = entry.split(";")[2].trim();
            String fitting = entry.split(";")[3].trim();

            SQLHub hub = new SQLHub(p);
            hub.executeData("insert into workspace (id, institution, area, fitting) values ("+id+",'"+institution+"','"+area+"','"+fitting+"')", "bookingservice");
        }

        /*
        SQLHub hub = new SQLHub(p);

        for(String line:data.split("\n")) {

            int c = 0;
            int id = 0;
            String institution = "", area = "", fitting = "";
            boolean has_pc = false;

            for(String attr:line.split(";")) {
                if(c==0) {
                    id = Integer.parseInt(attr);
                }
                if(c==1) {
                    institution = attr;
                }
                if(c==2) {
                    area = attr;
                }
                if(c>2) {
                    if(attr.length()>=2) {

                        fitting += attr + ",";
                        if(attr.equals("PC")) has_pc = true;
                    }


                }

                c++;
            }

            //if(!has_pc) fitting += "kein PC,";

            fitting = cutcommata(fitting.trim());

            System.out.println(id+"###"+institution+"###"+area+"###"+fitting);

            hub.executeData("insert into workspace (id, institution, area, fitting) values ("+id+",'"+institution.trim()+"','"+area.trim()+"','"+fitting.trim()+"')", "bookingservice");

        }
*/
    }

    private String cutcommata(String s) {

        if(s.endsWith(",")) {
            s = s.substring(0, s.length() - 1);
        }

        return s;
    }

    public static void main(String args[]) {
        new DBImporter_RewiII();
    }

}
