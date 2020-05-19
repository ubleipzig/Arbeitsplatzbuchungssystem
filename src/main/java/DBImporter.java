import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public class DBImporter {

    public DBImporter() {
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
            FileInputStream fis = new FileInputStream("imports_BA.csv");
            byte buffer[] = fis.readAllBytes();
            fis.close();

            data = new String(buffer, "UTF-8");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        SQLHub hub = new SQLHub(p);

        for(String line:data.split("\n")) {

            int c = 0;
            int id = 0;
            String institution = "", area = "", fitting = "";

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
                    }


                }

                c++;
            }


            fitting = cutcommata(fitting);

            System.out.println(id+"###"+institution+"###"+area+"###"+fitting);

            hub.executeData("insert into workspace (id, institution, area, fitting) values ("+id+",'"+institution+"','"+area+"','"+fitting.trim()+"')", "bookingservice");

        }

    }

    private String cutcommata(String s) {

        if(s.endsWith(",")) {
            s = s.substring(0, s.length() - 1);
        }

        return s;
    }

    public static void main(String args[]) {
        new DBImporter();
    }

}
