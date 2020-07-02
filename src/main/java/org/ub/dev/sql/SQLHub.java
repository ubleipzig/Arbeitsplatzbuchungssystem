package org.ub.dev.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

/**
 * SQLHub - Arbeitsplatzbuchungssystem
 *
 * Management-Klasse für SQL-Zugriffe
 *
 */
public class SQLHub {

    Properties p;

    public SQLHub(Properties p) {
        this.p = p;
    }

    public HashMap<String, Object> getSingleData(String sqlrequest, String database) {

        HashMap<String, Object> retval = new HashMap<String, Object>();

        String db_user = p.getProperty("database_user");
        String db_passwd = p.getProperty("database_password");

        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        try {
            Connection connect_digilife = DriverManager.getConnection(
                    "jdbc:mysql://localhost/" + database + "?user=" + db_user + "&password=" + db_passwd);
            Statement statement_digilife = connect_digilife.createStatement();
            ResultSet rs = statement_digilife.executeQuery(sqlrequest);
            if (rs.next()) {
                for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++)
                    retval.put(rs.getMetaData().getColumnLabel(i), rs.getObject(i));
            }
            rs.close();
            statement_digilife.close();
            connect_digilife.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return retval;

    }

    public void executeData(String sqlrequest, String database) {

        String db_user = p.getProperty("database_user");
        String db_passwd = p.getProperty("database_password");

        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        try {
            Connection connect_digilife = DriverManager.getConnection(
                    "jdbc:mysql://localhost/" + database + "?user=" + db_user + "&password=" + db_passwd);
            Statement statement_digilife = connect_digilife.createStatement();
            statement_digilife.execute(sqlrequest);
            statement_digilife.close();
            connect_digilife.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    public ArrayList<HashMap<String, Object>> getMultiData(String sqlrequest, String database) {

        ArrayList<HashMap<String, Object>> datalist = new ArrayList<HashMap<String, Object>>();

        String db_user = p.getProperty("database_user");
        String db_passwd = p.getProperty("database_password");

        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        try {
            Connection connect_digilife = DriverManager.getConnection(
                    "jdbc:mysql://localhost/" + database + "?user=" + db_user + "&password=" + db_passwd);
            Statement statement_digilife = connect_digilife.createStatement();
            ResultSet rs = statement_digilife.executeQuery(sqlrequest);
            while (rs.next()) {
                HashMap<String, Object> retval = new HashMap<String, Object>();
                for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++)
                    retval.put(rs.getMetaData().getColumnLabel(i), rs.getObject(i));
                datalist.add(retval);
            }
            rs.close();
            statement_digilife.close();
            connect_digilife.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return datalist;

    }

}
