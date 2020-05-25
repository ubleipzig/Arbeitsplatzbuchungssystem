

import au.com.libero.libraryapi.types.*;
import de.unileipzig.ub.libero6wsclient.Wachtl;

import java.io.IOException;
import java.util.*;

/**
 * Workspace - Bookingsystem
 *
 * LiberoManager
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

    public String login(String readernumber, String password) {
        Wachtl wachtl = new Wachtl();
        wachtl.setAuthenticatServiceURL(p.getProperty("authentication_url"));
        wachtl.setLibraryServiceURL(p.getProperty("library_url"));

        String token = wachtl.getAuthSoapClient().patronLogin(readernumber, password).getToken();
        if(token==null||token.equals("null")) return "null";
        String user_category = wachtl.getLibrarySoapClient().getMemberDetails(token, null, readernumber).getCategory().getCode();

        BookingService.categorymap.put(readernumber, user_category);

        boolean is_in_category = false;

        for(String c:p.getProperty("categories").split(","))
        {
            if(user_category.equals(c.trim())) is_in_category = true;
        }

        if(!is_in_category) token = "null";

        return token;
    }

    public void close(String token) {

        Wachtl wachtl = new Wachtl();
        wachtl.setAuthenticatServiceURL(p.getProperty("authentication_url"));
        wachtl.getAuthSoapClient().logout(token);

    }

}
