package org.ub.dev.service;

import io.vertx.core.json.JsonObject;
import org.ub.dev.sql.SQLHub;
import org.ub.dev.tools.Tools;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.*;

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

    public ArrayList<String> fitsInPlan(String inst, Calendar date, List<String> fitting, String area, long duration, HashMap<String, JsonObject> timeslots) {

        duration = duration*60*1000;    //convert from minutes to milliseconds

        ArrayList<String> possibleGaps = new ArrayList<>();

        Calendar opening = (Calendar)date.clone();

        opening.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeslots.get(inst).getJsonArray("interval").getJsonObject(0).getString("from").split(":")[0]));
        opening.set(Calendar.MINUTE, Integer.parseInt(timeslots.get(inst).getJsonArray("interval").getJsonObject(0).getString("from").split(":")[1]));
        opening.set(Calendar.SECOND, 0);
        opening.set(Calendar.MILLISECOND, 0);

        Calendar closing = (Calendar)date.clone();

        closing.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeslots.get(inst).getJsonArray("interval").getJsonObject(0).getString("until").split(":")[0]));
        closing.set(Calendar.MINUTE, Integer.parseInt(timeslots.get(inst).getJsonArray("interval").getJsonObject(0).getString("until").split(":")[1])+1);
        closing.set(Calendar.SECOND, 0);
        closing.set(Calendar.MILLISECOND, 0);

        for(HashMap workspace:workspaces) {
            if (!workspace.get("institution").equals(inst)) continue;
            if(!area.equals("no selection")) {
                if (!workspace.get("area").equals(area)) continue;
            }

            if(!fitting.isEmpty())
            {
                String foundfitting = (String)workspace.get("fitting");

                boolean nofitting = false;

                for(String f:fitting) {

                    if(!foundfitting.contains(f)) nofitting=true;
                }

                if(nofitting) continue;

            }

            int id = (int)workspace.get("id");

            ArrayList<Long> bookedworkspace = new ArrayList<>();
            bookedworkspace.add(opening.getTimeInMillis());


            for(HashMap booking:bookings) {
                if((int)booking.get("workspaceId")!=id) continue;
                if(!booking.get("institution").equals(inst)) continue;

                Timestamp ts_start = (Timestamp)booking.get("start");
                Timestamp ts_end = (Timestamp)booking.get("end");

                if(ts_start.getTime()>=opening.getTimeInMillis()&&ts_end.getTime()<=closing.getTimeInMillis()) {
                    bookedworkspace.add(ts_start.getTime());
                    bookedworkspace.add(ts_end.getTime());
                }
            }

            bookedworkspace.add(closing.getTimeInMillis());

            Collections.sort(bookedworkspace);

            boolean free = false;
            if(bookedworkspace.size()==2) free = true;

            while(bookedworkspace.size()>0) {

                long pos_duration = bookedworkspace.get(1) - bookedworkspace.get(0);

                if(pos_duration>=duration) {
                    if(free) possibleGaps.add(id+":"+bookedworkspace.get(0)+":F");
                    possibleGaps.add(id+":"+bookedworkspace.get(0)+":N");
                }
                bookedworkspace.remove(0);
                bookedworkspace.remove(0);
            }
        }

        return possibleGaps;
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

        //List<String> fittings = new ArrayList<>();

        //System.out.println(WorkloadStats.getInstance(p).fitsInPlan("Bibliotheca Albertina", Tools.setCalendarOnDate(19,8,2020), fittings, "no selection", 4*60));

    }

}
