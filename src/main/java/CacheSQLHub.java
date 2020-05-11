
import com.intersys.jdbc.CacheDataSource;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

public class CacheSQLHub {

    String cache_sql_url = "", cache_sql_user = "", cache_sql_password = "";

    public CacheSQLHub(Properties p) {

        cache_sql_url = p.getProperty("cache_sql_url");
        cache_sql_user = p.getProperty("cache_sql_user");
        cache_sql_password = p.getProperty("cache_sql_password");

    }

    public HashMap<String, Object> getSingleData(String sqlrequest, String database) {

        HashMap<String, Object> retval = new HashMap<String, Object>();

        try {

            CacheDataSource cds = new CacheDataSource();

            cds.setURL("jdbc:Cache://" + cache_sql_url + ":1972/LIBERO");
            cds.setUser(cache_sql_user);
            cds.setPassword(cache_sql_password);

            Connection conn = cds.getConnection();

            Statement state = conn.createStatement();
            ResultSet rs = state.executeQuery(sqlrequest);
            if (rs.next()) {
                for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++)
                    retval.put(rs.getMetaData().getColumnLabel(i), rs.getObject(i));
            }
            rs.close();
            state.close();
            conn.close();


        }catch(Exception e){
            e.printStackTrace();
        }

        return retval;
    }

    public ArrayList<HashMap<String, Object>> getMultiData(String sqlrequest, String database) {

        ArrayList<HashMap<String, Object>> datalist = new ArrayList<HashMap<String, Object>>();

        try {
            CacheDataSource cds = new CacheDataSource();

            cds.setURL("jdbc:Cache://" + cache_sql_url + ":1972/LIBERO");
            cds.setUser(cache_sql_user);
            cds.setPassword(cache_sql_password);

            Connection conn = cds.getConnection();

            Statement state = conn.createStatement();
            ResultSet rs = state.executeQuery(sqlrequest);
            while (rs.next()) {
                HashMap<String, Object> retval = new HashMap<String, Object>();
                for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++)
                    retval.put(rs.getMetaData().getColumnLabel(i), rs.getObject(i));
                datalist.add(retval);
            }
            rs.close();
            state.close();
            conn.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return datalist;

    }

    public static void main(String args[]) {

        Properties p = new Properties();
        try {
            p.load(new FileInputStream("config.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        CacheSQLHub csh = new CacheSQLHub(p);

        //System.out.println(csh.getMultiData("select AmountOutstanding, Holds, ILLRequests, Loans, PermanentLoans, ReadingRoom, Reserves, StackRequests from LBmem.MemberStatistics where BorrowerCode = '593941-5'", null));

    }

}
