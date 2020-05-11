import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

/**
 * BookingService -  Hauptklasse für das Arbeitsplatzbuchungssystem
 *
 * Die Klasse stellt im wesentlichen REST-API-Endpunkte zur Verfügung, über die das Frontend kommuniziert.
 */


public class BookingService {

    Properties p = new Properties();

    //Hashmap zur relationalen Speicherung von LKN und Libero-Token
    HashMap<String, String> tokenmap = new HashMap<>();

    public BookingService() {

        init();
        initNetwork();
    }

    private void initNetwork() {
        Vertx vertx = Vertx.vertx();
        Router router = Router.router(vertx);
        vertx.createHttpServer().requestHandler(router).listen(12105, result -> {
            if (result.succeeded()) {
                Promise.promise().complete();
            } else {
                Promise.promise().fail(result.cause());
            }
        });

        router.route("/booking/login*").handler(BodyHandler.create());
        router.post("/booking/login").handler(this::login);
        router.route("/booking/booking*").handler(BodyHandler.create());
        router.post("/booking/booking").handler(this::booking);

    }

    private void do_booking(String institution, String area, String day, String month, String year, String hour, String minute, String duration, String readernumber, String token) {

        /**
         * Verfahrensweise:
         * 1. Suche nach Plätzen im Pool, die den Anforderungen genügen
         * 2. Prüfe jeden gefunden Platz auf Verfügbarkeit im gewählten Zeitraum
         * 3. Beende die Suche bei gefundenem Platz
         *
         */

        SQLHub hub = new SQLHub(p);
        ArrayList<HashMap<String, Object>> result = hub.getMultiData("select id from workspace where institution = '"+institution+"'", "boookingservice");
        for(HashMap<String, Object> workspace:result) {
            int workspace_id = (int)workspace.get("id");
            SQLHub hub_intern = new SQLHub(p);
            //hub_intern.getSingleData("select * from booking where workspaceId = "+workspace_id+" and ")
        }

    }

    private void booking(RoutingContext rc) {
        String institution = rc.request().formAttributes().get("institution");
        String area = rc.request().formAttributes().get("area");
        String day = rc.request().formAttributes().get("from_day");
        String month = rc.request().formAttributes().get("from_month");
        String year = rc.request().formAttributes().get("from_year");
        String hour = rc.request().formAttributes().get("from_hour");
        String minute = rc.request().formAttributes().get("from_minute");
        String duration = rc.request().formAttributes().get("duration");
        String readernumber = rc.request().formAttributes().get("readernumber");
        String token = rc.request().formAttributes().get("token");

        System.out.println(institution+":"+area+":"+day+":"+month+":"+year+":"+hour+":"+minute+":"+duration+":"+readernumber+":"+token);

        do_booking(institution, area, day, month, year, hour, minute, duration, readernumber, token);

        rc.response().headers().add("Access-Control-Allow-Origin","*");
        rc.response().end();

    }

    private void logout(RoutingContext rc) {

    }

    /**
     *   Login-Endpunkt-Deklaration
     *
     *   Erwartet werden die Variablen readernumber und password
     *   Content-Type: application/x-www-form-urlencoded
     */
    private void login(RoutingContext rc) {
        String readernumber = rc.request().formAttributes().get("readernumber");
        String password = rc.request().formAttributes().get("password");

        //Zugriff auf Libero-Manager, ermittle Token für das Login mit gewählter LKN und Passwort
        LiberoManager lm = LiberoManager.getInstance(p);
        String token = lm.login(readernumber, password);

        //Wenn token==null, dann war der Login nicht möglich
        if(token==null) token="null";

        if(!token.equals("null")) {

            //add relation readernumber <-> token to the map
            tokenmap.put(readernumber, token);

            //prüfe auf existenz des nutzers in der DB und lege diesen ggf. an
            SQLHub hub = new SQLHub(p);
            if(hub.getSingleData("select * from user where readernumber = '"+readernumber+"'", "bookingservice").get("readernumber")==null)
                hub.executeData("insert into user (readernumber, quota) values ('"+readernumber.trim()+"',600)", "bookingservice");

        }

        rc.response().headers().add("Access-Control-Allow-Origin","*");
        rc.response().end(token);
    }

    //Initialisierung - Lade Konfiguration aus config.properties
    private void init() {

        try {
            p.load(new FileInputStream(new File("config/config.properties")));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void main(String args[])
    {
        new BookingService();
    }

}
