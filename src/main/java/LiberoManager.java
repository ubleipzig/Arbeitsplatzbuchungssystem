

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
    String token = new String();
    Wachtl wachtl;

    public static LiberoManager liberomanager;

    public static LiberoManager getInstance() {
        if(liberomanager==null)
            liberomanager = new LiberoManager();
        return liberomanager;
    }

    public static LiberoManager getInstance(Properties p) {
        if(liberomanager==null)
            liberomanager = new LiberoManager(p);
        return liberomanager;
    }


    private LiberoManager() {

    }

    public void setProperty(Properties p) {
        this.p = p;

    }

    private LiberoManager(Properties p) {
        this.p = p;

    }

    private void init() {
        try {
            token = getToken();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //get login token
    private String getToken() throws IOException {

        wachtl = new Wachtl();
        wachtl.setAuthenticatServiceURL(p.getProperty("authentication_url"));

        String token = wachtl.getAuthSoapClient().patronLogin(p.getProperty("libero_user"), p.getProperty("libero_password")).getToken();

        return token;
    }

    public String login(String readernumber, String password) {
        wachtl = new Wachtl();
        wachtl.setAuthenticatServiceURL(p.getProperty("authentication_url"));

        String token = wachtl.getAuthSoapClient().patronLogin(readernumber, password).getToken();

        return token;
    }

    public void close() {
        wachtl.getAuthSoapClient().logout(token);

    }

    public Member getUser(String membercode) {
        wachtl.setLibraryServiceURL(p.getProperty("library_url"));
        Member member = wachtl.getLibrarySoapClient().getMemberDetails(token, membercode, membercode);
        return member;
    }

    public boolean existUser(String membercode) {
        wachtl.setLibraryServiceURL(p.getProperty("library_url"));

        Member member = wachtl.getLibrarySoapClient().getMemberDetails(token, membercode, membercode);
        if(member==null) return false;

        return true;
    }

    private String preventNull2String(Object value) {
        if(value!=null)
            return String.valueOf(value);
        return "0.00";
    }

    public String getReadernumberFromLogin(String login) {

        CacheSQLHub csh = new CacheSQLHub(p);

        String borrowerCode = (String)csh.getSingleData("select BorrowerCode from LBmem.Member where BorField01 = '"+login+"'", null).get("BorrowerCode");

        return borrowerCode;
    }







}
