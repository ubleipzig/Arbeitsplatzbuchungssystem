import com.fasterxml.jackson.databind.util.JSONPObject;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
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

    //Hashmap zur Speicherung der Zeitslots nach Institution
    HashMap<String, JsonObject> timeslots = new HashMap<>();

    public BookingService() {

        init();
        initNetwork();
    }

    //Initialisiere Vertx-basierte API-Endpunkte
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

        router.route().handler(CorsHandler.create(".*."));
        router.route("/booking/login*").handler(BodyHandler.create());
        router.post("/booking/login").handler(this::login);
        router.route("/booking/logout*").handler(BodyHandler.create());
        router.post("/booking/logout").handler(this::logout);
        router.route("/booking/booking*").handler(BodyHandler.create());
        router.post("/booking/booking").handler(this::booking);
        router.route("/booking/areas*").handler(BodyHandler.create());
        router.get("/booking/areas").handler(this::areas);
        router.route("/booking/timeslots*").handler(BodyHandler.create());
        router.get("/booking/timeslots").handler(this::timeslots);
        router.route("/booking/institutions*").handler(BodyHandler.create());
        router.get("/booking/institutions").handler(this::institutions);
    }

    private void institutions(RoutingContext rc) {

        String retval = "";

        SQLHub hub = new SQLHub(p);
        ArrayList<HashMap<String, Object>> list = hub.getMultiData("select distinct institution from workspace", "bookingservice");

        for(HashMap institutions:list) {
            retval+=institutions.get("institution")+"#";
        }
        if(retval.contains("#"))
            retval = retval.substring(0, retval.length()-1);

        rc.response().headers().add("Content-type","html/text");

        rc.response().end(retval);
    }

    /**
     * Retourniert die Bereiche, die ein Standort aufweisen kann
     * in einfachen Text-String mit #-Delimiter
     *
     * todo: Rückgabe kompletter Option-Einträge, oder Auslieferung als JSON?
     *
      * @param rc
     */
    private void areas(RoutingContext rc) {
        String institution = rc.request().getParam("institution");
        String retval = "";

        SQLHub hub = new SQLHub(p);
        ArrayList<HashMap<String, Object>> list = hub.getMultiData("select distinct area from workspace where institution = '"+institution+"'", "bookingservice");

        for(HashMap area:list) {

            retval+=area.get("area")+"#";
        }
        if(retval.contains("#"))
            retval = retval.substring(0, retval.length()-1);

        rc.response().headers().add("Content-type","html/text");

        rc.response().end(retval);
    }


    /**
     * Retournierung der Zeitslots nach Standort in fertigen Options-Block aus der Hashmap
     * @param rc
     */
    private void timeslots(RoutingContext rc) {
        String institution = rc.request().getParam("institution");

        rc.response().headers().add("Content-type","application/json");
        rc.response().end(timeslots.get(institution).encodePrettily());

    }

    /**
     * Buchungsfunktion. Variante mit Zeitslot statt Stunde/Minute/Dauer.
     * Der timeslot-Parameter wird hierbei zerlegt in Stunde/Minute und die Dauer errechnet.
     * Danach Aufruf der Basis-Buchungsfunktion.
     *
     * @param institution
     * @param area
     * @param day
     * @param month
     * @param year
     * @param timeslot
     * @param readernumber
     * @param token
     * @return
     */
    private String[] do_booking(String institution, String area, String day, String month, String year, String timeslot, String readernumber, String token, List<String> fitting) {
        String hour = timeslot.split("-")[0].substring(0, 2);
        String minute = timeslot.split("-")[0].substring(2);

        String hour_2 = timeslot.split("-")[1].substring(0,2);
        String minute_2 = timeslot.split("-")[1].substring(2);

        long dif_min = 0;

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        try {
            Date date1 = sdf.parse(hour+":"+minute);
            Date date2 = sdf.parse(hour_2+":"+minute_2);

            long dif = date2.getTime()-date1.getTime();
            dif_min = dif/(1000*60);

        } catch (ParseException e) {
            e.printStackTrace();
        }

        return do_booking(institution, area, day, month, year, hour, minute, ""+dif_min, readernumber, token, fitting);
    }

    /**
     * Basis-Buchungsfunktion.
     *
     * @param institution
     * @param area
     * @param day
     * @param month
     * @param year
     * @param hour
     * @param minute
     * @param duration
     * @param readernumber
     * @param token
     * @return
     */
    private String[] do_booking(String institution, String area, String day, String month, String year, String hour, String minute, String duration, String readernumber, String token, List<String> fitting) {

        /**
         * Verfahrensweise:
         * 1. Suche nach Plätzen im Pool, die den Anforderungen genügen
         * 2. Prüfe jeden gefunden Platz auf Verfügbarkeit im gewählten Zeitraum
         * 3. Beende die Suche bei gefundenem Platz
         *
         */

        //bookingcode, workspaceid, emailadress
        String bookingArray[] = {"","",""};

        //convert day|month|year|hour|minute to Timestamp
        Calendar cal = Calendar.getInstance();
        cal.set(Integer.parseInt(year), Integer.parseInt(month)-1, Integer.parseInt(day), Integer.parseInt(hour), Integer.parseInt(minute));
        Timestamp start_sql = new Timestamp(cal.getTimeInMillis());

        //convert duration to milliseconds
        long duration_ms = Integer.parseInt(duration)*60*1000;
        Timestamp end_sql = new Timestamp(cal.getTimeInMillis()+duration_ms);

        ArrayList<HashMap<String, Object>> result = new ArrayList<>();

        String area_query = "";

        if(!area.equals("no selection")) area_query = "and area = '"+area.trim()+"'";

        SQLHub hub = new SQLHub(p);
        if(fitting.isEmpty())
            result = hub.getMultiData("select id from workspace where institution = '"+institution.trim()+"' "+area_query, "bookingservice");
        else
        {
            String fitting_query = "";
            for(String f:fitting) {
                if(f.equals("kein PC"))
                    fitting_query = "and fitting not like '%PC%'";
                else
                    fitting_query+= "and fitting like '%"+f+"%'";
            }

            result = hub.getMultiData("select id from workspace where institution = '"+institution.trim()+"' "+area_query+" "+fitting_query, "bookingservice");
        }

        Collections.shuffle(result);

        boolean found_workplace = false;

        int workspace_id = -1;
        for(HashMap<String, Object> workspace:result) {
            workspace_id = (int)workspace.get("id");
            SQLHub hub_intern = new SQLHub(p);
            if(
                    hub_intern.getSingleData("select * from booking where workspaceId = "+workspace_id+" and institution = '"+institution.trim()+"' and start <= '"+start_sql+"' and end between '"+start_sql+"' and '"+end_sql+"'","bookingservice").isEmpty()&&
                    hub_intern.getSingleData("select * from booking where workspaceId = "+workspace_id+" and institution = '"+institution.trim()+"' and start >= '"+start_sql+"' and end between '"+start_sql+"' and '"+end_sql+"'", "bookingservice").isEmpty()&&
                    hub_intern.getSingleData("select * from booking where workspaceId = "+workspace_id+" and institution = '"+institution.trim()+"' and end >= '"+end_sql+"' and start between '"+start_sql+"' and '"+end_sql+"'", "bookingservice").isEmpty()&&
                    hub_intern.getSingleData("select * from booking where workspaceId = "+workspace_id+" and institution = '"+institution.trim()+"' and end <= '"+end_sql+"' and start between '"+start_sql+"' and '"+end_sql+"'", "bookingservice").isEmpty()
            )
            {
                //System.out.println("Platz mit der ID "+workspace_id+" gefunden!");
                found_workplace = true;
            }

            if(found_workplace) break;
        }

        if(found_workplace) {

            //generate bookingcode, uses randomized UUID
            bookingArray[0] = UUID.randomUUID().toString();
            bookingArray[1] = String.valueOf(workspace_id);

            String email = new LiberoManager(p).getMailAdress(readernumber, token);
            sendMail(email, readernumber, institution, area, workspace_id, day, month, year, start_sql, end_sql, bookingArray[0]);

            bookingArray[2] = email;

            SQLHub hub_intern = new SQLHub(p);
            hub_intern.executeData("insert into booking (workspaceId, start, end, readernumber, bookingCode, institution) values ('" + workspace_id + "','" + start_sql + "','" + end_sql + "','" + readernumber + "','"+bookingArray[0]+"','"+institution.trim()+"')", "bookingservice");
            hub_intern.executeData("insert into user_details (readernumber, bt_start, bt_end, institution) values ('"+readernumber+"','"+start_sql+"','"+end_sql+"','"+institution.trim()+"')", "bookingservice");
            hub_intern.executeData("update user set past = past+"+Integer.parseInt(duration)+" where readernumber = '"+readernumber+"'", "bookingservice");
        }

        return bookingArray;
    }

    private void sendMail(String address, String readernumber, String institution, String area, int workspaceId, String day, String month, String year, Timestamp start, Timestamp end, String bookingCode) {
        try {

            FileInputStream fis = new FileInputStream("config/mail.template");
            byte buffer[] = fis.readAllBytes();
            fis.close();

            String data = new String(buffer);
            data = data.replaceAll("###readernumber###",readernumber);
            data = data.replaceAll("###institution###",institution);
            data = data.replaceAll("###area###",area);
            data = data.replaceAll("###id###",String.valueOf(workspaceId));

            String mydate = day+"."+month+"."+year;
            data = data.replaceAll("###date###",mydate);

            String mytimeslot = start.toLocalDateTime().format(DateTimeFormatter.ofPattern("HH:mm"))+" - "+end.toLocalDateTime().format(DateTimeFormatter.ofPattern("HH:mm"));
            data = data.replaceAll("###timeslot###",mytimeslot);

            data = data.replaceAll("###code###",bookingCode);

            QuickMail.sendMail("mailservice@ub.uni-leipzig.de", "mailservice", address, data, "Arbeitsplatzbuchung an der UBL");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void booking(RoutingContext rc) {
        String institution = rc.request().formAttributes().get("institution");
        String area = rc.request().formAttributes().get("area");
        //String day = rc.request().formAttributes().get("from_day");
        //String month = rc.request().formAttributes().get("from_month");
        //String year = rc.request().formAttributes().get("from_year");
        //String hour = rc.request().formAttributes().get("from_hour");
        //String minute = rc.request().formAttributes().get("from_minute");
        //String duration = rc.request().formAttributes().get("duration");
        //String timeslot = rc.request().formAttributes().get("timeslot");
        String from_date = rc.request().formAttributes().get("from_date");
        String from_time = rc.request().formAttributes().get("from_time");
        String until_time = rc.request().formAttributes().get("until_time");
        List<String> fitting = rc.request().formAttributes().getAll("fitting");
        String readernumber = rc.request().formAttributes().get("readernumber");
        String token = rc.request().formAttributes().get("token");

        //System.out.println(institution+":"+area+":"+day+":"+month+":"+year+":"+hour+":"+minute+":"+duration+":"+readernumber+":"+token);

        String year, month, day, timeslot;

        year = from_date.split("-")[0];
        month = from_date.split("-")[1];
        day = from_date.split("-")[2];

        timeslot = from_time.replaceAll(":","")+"-"+until_time.replaceAll(":","");

        //String bookingArray[] = do_booking(institution, area, day, month, year, hour, minute, duration, readernumber, token, fitting);
        String bookingArray[] = do_booking(institution, area, day, month, year, timeslot, readernumber, token, fitting);

        JsonObject json = new JsonObject();
        json.put("bookingCode", bookingArray[0]);
        json.put("workspaceId", bookingArray[1]);
        json.put("email", bookingArray[2]);

        rc.response().end(json.encodePrettily());

    }

    /**
     * Logout. Löscht die Relation token<->readernumber und beendet die offene Libero-Session.
     * Content-Type: application/json
     *
     * @param rc
     */
    private void logout(RoutingContext rc) {

        String token = rc.getBodyAsJson().getString("token");
        String readernumber = rc.getBodyAsJson().getString("readernumber");

        if(tokenmap.containsKey(readernumber)) {
            if (tokenmap.get(readernumber).equals(token)) {
                tokenmap.remove(readernumber);
                new LiberoManager(p).close(token);
            }
        }

        rc.response().end();

    }

    /**
     *   Login
     *
     *   Erwartet werden die Variablen readernumber und password
     *   Content-Type: application/x-www-form-urlencoded
     */
    private void login(RoutingContext rc) {
        String readernumber = rc.request().formAttributes().get("readernumber");
        String password = rc.request().formAttributes().get("password");

        //Zugriff auf Libero-Manager, ermittle Token für das Login mit gewählter LKN und Passwort

        String token = new LiberoManager(p).login(readernumber, password);

        //Wenn token==null, dann war der Login nicht möglich
        if(token==null) token="null";

        if(!token.equals("null")) {

            //add relation readernumber <-> token to the map
            tokenmap.put(readernumber, token);

            //prüfe auf existenz des nutzers in der DB und lege diesen ggf. an
            SQLHub hub = new SQLHub(p);
            if(hub.getSingleData("select * from user where readernumber = '"+readernumber+"'", "bookingservice").get("readernumber")==null)
                hub.executeData("insert into user (readernumber, quota, past) values ('"+readernumber.trim()+"',600,0)", "bookingservice");

        }

        rc.response().end(token);
    }

    //Initialisierung - Lade Konfiguration aus config.properties
    private void init() {

        try {
            p.load(new FileInputStream(new File("config/config.properties")));
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            Document doc = new SAXBuilder().build("config/timeslots.xml");
            String option_element = new String();
            for(Element e_inst:doc.getRootElement().getChildren("institution")) {
                Element e_interval = e_inst.getChild("interval");

                JsonObject jso = new JsonObject();

                jso.put("from", e_interval.getAttributeValue("from"));
                jso.put("until", e_interval.getAttributeValue("until"));

                timeslots.put(e_inst.getAttributeValue("name"), jso);


            }
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println(timeslots);

    }

    public static void main(String args[])
    {
        new BookingService();
    }

}
