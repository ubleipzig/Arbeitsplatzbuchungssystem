import com.fasterxml.jackson.databind.util.JSONPObject;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.*;

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
        router.route("/booking/areas*").handler(BodyHandler.create());
        router.get("/booking/areas").handler(this::areas);

    }

    private void areas(RoutingContext rc) {
        String institution = rc.request().getParam("institution");

        SQLHub hub = new SQLHub(p);
        ArrayList<HashMap<String, Object>> list = hub.getMultiData("select distinct area from areas where institution = '"+institution+"'", "bookingservice");
        System.out.println(list);
        JsonArray array = new JsonArray();

        for(HashMap area:list) {
            array.add(area.get("area"));
        }

        JsonObject answer = new JsonObject();
        answer.put("areas", array);

        rc.response().headers().add("Access-Control-Allow-Origin","*");
        rc.response().end(answer.encodePrettily());
    }

    private String[] do_booking(String institution, String area, String day, String month, String year, String hour, String minute, String duration, String readernumber, String token) {

        /**
         * Verfahrensweise:
         * 1. Suche nach Plätzen im Pool, die den Anforderungen genügen
         * 2. Prüfe jeden gefunden Platz auf Verfügbarkeit im gewählten Zeitraum
         * 3. Beende die Suche bei gefundenem Platz
         *
         */

        String bookingArray[] = {"",""};

        //convert day|month|year|hour|minute to Timestamp
        Calendar cal = Calendar.getInstance();
        cal.set(Integer.parseInt(year), Integer.parseInt(month), Integer.parseInt(day), Integer.parseInt(hour), Integer.parseInt(minute));
        Timestamp start_sql = new Timestamp(cal.getTimeInMillis());

        //convert duration to milliseconds
        long duration_ms = Integer.parseInt(duration)*60*1000;
        Timestamp end_sql = new Timestamp(cal.getTimeInMillis()+duration_ms);

        if(institution.equals("BA")) institution = "Bibliotheca Albertina";

        SQLHub hub = new SQLHub(p);
        ArrayList<HashMap<String, Object>> result = hub.getMultiData("select id from workspace where institution = '"+institution.trim()+"'", "bookingservice");

        boolean found_workplace = false;

        int workspace_id = -1;
        for(HashMap<String, Object> workspace:result) {
            workspace_id = (int)workspace.get("id");
            SQLHub hub_intern = new SQLHub(p);
            if(hub_intern.getSingleData("select * from booking where workspaceId = "+workspace_id+" and start <= '"+start_sql+"' and end between '"+start_sql+"' and '"+end_sql+"'","bookingservice").isEmpty()&&
            hub_intern.getSingleData("select * from booking where workspaceId = "+workspace_id+" and start >= '"+start_sql+"' and end between '"+start_sql+"' and '"+end_sql+"'", "bookingservice").isEmpty()&&
            hub_intern.getSingleData("select * from booking where workspaceId = "+workspace_id+" and end >= '"+end_sql+"' and start between '"+start_sql+"' and '"+end_sql+"'", "bookingservice").isEmpty()&&
            hub_intern.getSingleData("select * from booking where workspaceId = "+workspace_id+" and end <= '"+end_sql+"' and start between '"+start_sql+"' and '"+end_sql+"'", "bookingservice").isEmpty()) {
                System.out.println("Platz mit der ID "+workspace_id+" gefunden!");
                found_workplace = true;
            }

            if(found_workplace) break;
        }
        System.out.println(found_workplace);
        if(found_workplace) {

            //generate bookingcode, uses randomized UUID
            bookingArray[0] = UUID.randomUUID().toString();
            bookingArray[1] = String.valueOf(workspace_id);

            SQLHub hub_intern = new SQLHub(p);
            hub_intern.executeData("insert into booking (workspaceId, start, end, readernumber, bookingCode) values ('" + workspace_id + "','" + start_sql + "','" + end_sql + "','" + readernumber + "','"+bookingArray[0]+"')", "bookingservice");
        }

        return bookingArray;
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

        //System.out.println(institution+":"+area+":"+day+":"+month+":"+year+":"+hour+":"+minute+":"+duration+":"+readernumber+":"+token);

        String bookingArray[] = do_booking(institution, area, day, month, year, hour, minute, duration, readernumber, token);


        JsonObject json = new JsonObject();
        json.put("bookingCode", bookingArray[0]);
        json.put("workspaceId", bookingArray[1]);

        rc.response().headers().add("Access-Control-Allow-Origin","*");
        rc.response().end(json.encodePrettily());

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
