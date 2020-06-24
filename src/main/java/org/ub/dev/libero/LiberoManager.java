package org.ub.dev.libero;

import de.unileipzig.ub.libero6wsclient.Wachtl;
import org.ub.dev.service.BookingService;

import java.util.*;

/**
 * Workspace - Bookingsystem
 *
 * org.ub.dev.libero.LiberoManager
 */

public class LiberoManager {

    Properties p;

    public void setProperty(Properties p) {
        this.p = p;
    }

    public LiberoManager(Properties p) {
        this.p = p;
    }

    public String getMailAdress(String readernumber, String token) {

        Wachtl wachtl = new Wachtl();
        wachtl.setLibraryServiceURL(p.getProperty("library_url"));
        String email = wachtl.getLibrarySoapClient().getMemberDetails(token, null, readernumber).getEmailAddress();
        return email;
    }

    public String[] malogin(String username, String password) {
        Wachtl wachtl = new Wachtl();
        wachtl.setAuthenticatServiceURL(p.getProperty("authentication_url"));

        String token = wachtl.getAuthSoapClient().login(username, password).getToken();
        if(token==null||token.equals("null")) {
            String retval[] = {"null","Wrong username or password"};
            return retval;
        }

        String retval[] = {token, ""};
        return retval;
    }

    private String fakeToken() {
        String array = new String("abcdefghijklmnopqrstuvwxyz");
        String token = "";
        for(int i=0;i<10;i++)
        {
            token += array.charAt(new Random().nextInt(26));
            System.out.println(token);
        }

        return token;
    }

    public String[] login(String readernumber, String password) {
        Wachtl wachtl = new Wachtl();
        wachtl.setAuthenticatServiceURL(p.getProperty("authentication_url"));
        wachtl.setLibraryServiceURL(p.getProperty("library_url"));

        String msg = "";

        String token = wachtl.getAuthSoapClient().patronLogin(readernumber, password).getToken();

        if(token==null||token.equals("null")) {
            String retval[] = {"null","Wrong readernumber or password"};
            return retval;
        }
        String user_category = wachtl.getLibrarySoapClient().getMemberDetails(token, null, readernumber).getCategory().getCode();

        BookingService.categorymap.put(readernumber, user_category);

        boolean is_in_category = false;

        for(String c:p.getProperty("categories").split(","))
        {
            if(user_category.equals(c.trim())) is_in_category = true;
        }

        if(!is_in_category) {
            token = "null";
            msg = "Wrong category";
        }

        String retval[] = {token, msg};

        return retval;
    }

    public void close(String token) {

        Wachtl wachtl = new Wachtl();
        wachtl.setAuthenticatServiceURL(p.getProperty("authentication_url"));
        wachtl.getAuthSoapClient().logout(token);

    }

}
