package org.ub.dev.service;

import io.vertx.core.json.JsonObject;
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
    HashMap<String, Integer> numberOfSeats;

    long timestamp = 0L;

    static WorkloadStats wls;

    public static WorkloadStats getInstance(Properties p) {
        if(wls==null) wls = new WorkloadStats(p);
        return wls;
    }

    public void init() {

    }

    private WorkloadStats(Properties p) {
        this.p = p;
        numberOfSeats = new HashMap<>();
        cacheDB();

    }

    private void cacheDB() {
        System.out.println("Daten werden erneuert!");
        workspaces = new SQLHub(p).getMultiData("select * from workspace","bookingservice");
        bookings = new SQLHub(p).getMultiData("select * from booking","bookingservice");

        timestamp = System.currentTimeMillis();
    }

    public ArrayList<int[]> getSevenDays(String inst) {



        ArrayList<int[]> list = new ArrayList<>();

        Calendar today = Calendar.getInstance();

        for(int i=0;i<8;i++)
        {
            list.add(getData(inst, today));
            today.add(Calendar.DAY_OF_MONTH, 1);
        }

        return list;
    }

    private void setNumberOfSeats(String inst) {


        int nos = Integer.parseInt(String.valueOf(new SQLHub(p).getSingleData("select count(*) from workspace where institution = '"+inst+"'","bookingservice").get("count(*)")));

        nos = Integer.parseInt(p.getProperty("nos_"+inst.replaceAll(" ",""), ""+nos));

        numberOfSeats.put(inst, nos);
    }

    private int[] getData(String inst, Calendar cal) {

        if(!numberOfSeats.containsKey(inst)) setNumberOfSeats(inst);

        if(System.currentTimeMillis()-timestamp>=300000) cacheDB();

        Calendar cals[] = new Calendar[24];
        for(int i=0;i<cals.length;i++)
        {
            cals[i] = (Calendar)cal.clone();
            cals[i].set(Calendar.HOUR_OF_DAY, i);
            cals[i].set(Calendar.MINUTE, 0);
            cals[i].set(Calendar.SECOND, 0);
            cals[i].set(Calendar.MILLISECOND, 0);

        }

        int c_array[] = new int[24];
        for(int i=0;i<c_array.length;i++)
            c_array[i]=numberOfSeats.get(inst);

        for(HashMap workspace:workspaces) {
            if(!workspace.get("institution").equals(inst)) continue;

            int id = (int)workspace.get("id");
            for(HashMap booking:bookings) {
                if((int)booking.get("workspaceId")!=id) continue;
                if(!booking.get("institution").equals(inst)) continue;

                Timestamp ts_start = (Timestamp)booking.get("start");
                Timestamp ts_end = (Timestamp)booking.get("end");

                for(int i=0;i<cals.length;i++) {

                    if(cals[i].getTimeInMillis()>=ts_start.getTime() && cals[i].getTimeInMillis()<=ts_end.getTime())
                    {
                        c_array[i]--;
                    }

                }

            }


        }

        //in Prozent umrechnen
        for(int i=0;i< c_array.length;i++) {
            c_array[i] = (100* c_array[i])/numberOfSeats.get(inst);
        }

        return c_array;
    }

    /**
     * Test-Aufruf als Stand-Alone-Applikation
     *
     * @param args
     */
    public static void main(String args[]) {
        Properties p = new Properties();
        try {
            p.load(new FileInputStream("config/config.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        /*Calendar test = Calendar.getInstance();
        test.set(Calendar.DAY_OF_MONTH, 18);
        test.set(Calendar.MONTH, Calendar.JUNE);*/

        int r[] = WorkloadStats.getInstance(p).getData("Bibliothek Veterinärmedizin",Calendar.getInstance());
        for(int x:r) System.out.println(x);

        /*for(int[] e:WorkloadStats.getInstance(p).getSevenDays("Bibliothek Veterinärmedizin")){
            for(int i:e) System.out.print(i+" ");
            System.out.println();
        }*/

    }

}
