import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

public class DBRepair {
    public DBRepair() {
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
        ArrayList<HashMap<String, Object>> all_seats = hub.getMultiData("select * from workspace","bookingservice");
        for(HashMap<String, Object> seat:all_seats) {
            String inst = (String)seat.get("institution");
            SQLHub hub2 = new SQLHub(p);
            ArrayList<HashMap<String, Object>> bookings = hub2.getMultiData("select * from booking where workspaceId = "+seat.get("id")+" and institution = '"+inst+"'", "bookingservice");
            for(HashMap<String, Object> entry:bookings) {
                long a = ((Timestamp)entry.get("start")).getTime();
                long b = ((Timestamp)entry.get("end")).getTime();
                String bookingcode = (String)entry.get("bookingCode");

                String as = ((Timestamp)entry.get("start")).toString().split(" ")[0];

                boolean trapped = false;

                for(HashMap<String, Object> entry2:bookings) {
                    if(entry2.get("bookingCode").equals(bookingcode)) continue;
                    String a1s = ((Timestamp)entry2.get("start")).toString().split(" ")[0];

                    if(!as.equals(a1s)) continue;

                    long a1 = ((Timestamp)entry2.get("start")).getTime();
                    long b1 = ((Timestamp)entry2.get("end")).getTime();

                    if(a1<=a&&b1>=b) trapped = true;

                    if(a1>=a&&b1<=b) trapped = true;

                    if(a1>=a&&a1<=b&&b1>=b) trapped = true;

                    if(a1<=a&&b1>=a&&b1<=b) trapped = true;




                }

                if(trapped) {
                    System.out.println(entry);
                }

            }
        }
    }

    public static void main(String args[]) {
        new DBRepair();
    }
}
