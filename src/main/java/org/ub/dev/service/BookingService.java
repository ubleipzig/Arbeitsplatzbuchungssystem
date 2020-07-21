package org.ub.dev.service;

import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
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
import org.ub.dev.libero.LiberoManager;
import org.ub.dev.sql.SQLHub;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * org.ub.dev.service.BookingService -  Hauptklasse für das Arbeitsplatzbuchungssystem
 *
 * Die Klasse stellt sowohl REST-API-Endpunkte zur Verfügung, über die das Frontend kommuniziert, als auch die gesamten Logikkomponenten.
 */


public class BookingService {

    Properties p = new Properties();

    //Hashmap zur relationalen Speicherung von LKN und Libero-Token
    HashMap<String, String> tokenmap = new HashMap<>();

    //Hashmap zur Speicherung von LKN und Anmeldezeitpunkt
    HashMap<String, Long> tokentimes = new HashMap<>();

    //Hashmap zur Speicherung von Nutzername (Mitarbeiter) und Libero-Token
    HashMap<String, String> tokenmapma = new HashMap<>();

    //Hashmap zur Speichberung von Nutzername (Mitarbeiter) und Anmeldezeitpunkt
    HashMap<String, Long> tokentimesma = new HashMap<>();

    //Hashmap zur Speicherung der Zeitslots nach Institution
    HashMap<String, JsonObject> timeslots = new HashMap<>();

    //Hashmap zur Speicherung der Nutzerkategorie
    public static HashMap<String, String> categorymap = new HashMap<>();

    //Cache-Speicher für die Institutionsliste
    ArrayList<HashMap<String, Object>> institutionlist = new ArrayList<>();

    //Cache-Speicher für die Bereichsliste
    HashMap<String,ArrayList<HashMap<String, Object>>> areamap = new HashMap<>();

    //Zähler für die angemeldeten Nutzer
    int user_counter = 0;

    public static boolean secured_area = false;

    long call_stats[] = new long[8];

    public BookingService() {

        init();
        initNetwork();
    }

    //Initialisiere Vertx-basierte API-Endpunkte
    private void initNetwork() {

        final VertxOptions vertOptions = new VertxOptions();
        vertOptions.setMaxEventLoopExecuteTime(9000000000L);

        Vertx vertx = Vertx.vertx(vertOptions);
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
        router.route("/booking/storno*").handler(BodyHandler.create());
        router.post("/booking/storno").handler(this::storno);
        router.route("/booking/checkdate*").handler(BodyHandler.create());
        router.post("/booking/checkdate").handler(this::checkdate);
        router.route("/booking/counter*").handler(BodyHandler.create());
        router.get("/booking/counter").handler(this::counter);

        router.route("/booking/malogin*").handler(BodyHandler.create());
        router.post("/booking/malogin").handler(this::malogin);
        router.route("/booking/malogout*").handler(BodyHandler.create());
        router.post("/booking/malogout").handler(this::malogout);
        router.route("/booking/check*").handler(BodyHandler.create());
        router.post("/booking/check").handler(this::checkReservation);
        router.route("/booking/plan*").handler(BodyHandler.create());
        router.post("/booking/plan").handler(this::plan);
        router.route("/booking/mastorno*").handler(BodyHandler.create());
        router.post("/booking/mastorno").handler(this::mastorno);

        router.route("/booking/stats*").handler(BodyHandler.create());
        router.get("/booking/stats").handler(this::stats);
    }

    /**
     * API-Endpunkt für Statisktiken unter Angabe der Institution, dem Startzeitpunkt und Endzeitpunkt
     *
     * @param rc
     */

    private void stats(RoutingContext rc) {
        String institution = rc.request().getParam("institution");
        String from = rc.request().getParam("from");
        String until = rc.request().getParam("until");

        DataCore dc = new DataCore(institution, from, until, p);
        String data = dc.getData();

        rc.response().headers().add("Content-type", "text/csv");
        rc.response().end(data);
    }

    /**
     * API-Endpunkt für die Darstellung der aktuellen Aufrufzahlen für die einzelnen API-Endpunkte
     *
     * @param rc
     */

    private void counter(RoutingContext rc) {
        String results = "";

        results+="Institutions: "+call_stats[0]+"\n";
        results+="Areas: "+call_stats[1]+"\n";
        results+="Timeslots: "+call_stats[2]+"\n";
        results+="Booking: "+call_stats[3]+"\n";
        results+="Checkdate: "+call_stats[4]+"\n";
        results+="Storno: "+call_stats[5]+"\n";
        results+="Logout: "+call_stats[6]+"\n";
        results+="Login: "+call_stats[7]+"\n";
        results+="User-Count:"+user_counter+"\n";
        results+="tokenmap:"+tokenmap+"\n";
        results+="tokentimes:"+tokentimes+"\n";

        rc.response().end(results);
    }

    /**
     * Endpunkt für Ausgabe der verschiedenen Institutionen
     * Content-Type: html/text
     *
     * @param rc
     */

    private void institutions(RoutingContext rc) {

        call_stats[0]++;

        String retval = "";

        if(institutionlist.isEmpty()) {

            SQLHub hub = new SQLHub(p);
            institutionlist = hub.getMultiData("select distinct institution from workspace", "bookingservice");
        }

        for(HashMap institutions:institutionlist) {
            //if(institutions.get("institution").equals("Bibliothek Rechtswissenschaft")) continue;
            retval+=institutions.get("institution")+"#";
        }
        if(retval.contains("#"))
            retval = retval.substring(0, retval.length()-1);

        rc.response().headers().add("Content-type","text/html");

        rc.response().end(retval);
    }

    /**
     * Retourniert die Bereiche, die ein Standort aufweisen kann
     * in einfachen Text-String mit #-Delimiter
     * Content-Type: text/html
     *
     *
     * @param rc
     */
    private void areas(RoutingContext rc) {

        call_stats[1]++;

        String institution = rc.request().getParam("institution");
        String retval = "";

        if(areamap.get(institution)==null) {

            SQLHub hub = new SQLHub(p);
            ArrayList<HashMap<String, Object>> list = hub.getMultiData("select distinct area from workspace where institution = '" + institution + "'", "bookingservice");

            areamap.put(institution, list);
        }

        for(HashMap area:areamap.get(institution)) {

            retval+=area.get("area")+"#";
        }
        if(retval.contains("#"))
            retval = retval.substring(0, retval.length()-1);

        rc.response().headers().add("Content-type","text/html");

        rc.response().end(retval);
    }


    /**
     * Retournierung der Zeitslots nach Standort
     * Content-type: application/json
     *
     * @param rc
     */
    private void timeslots(RoutingContext rc) {

        call_stats[2]++;

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
        String bookingArray[] = {"","","",""}; //UUID, workspaceId, email, msg

        //convert day|month|year|hour|minute to Timestamp
        Calendar cal = Calendar.getInstance();
        cal.set(Integer.parseInt(year), Integer.parseInt(month)-1, Integer.parseInt(day), Integer.parseInt(hour), Integer.parseInt(minute));
        Timestamp start_sql = new Timestamp(cal.getTimeInMillis());

        Calendar cal_today = Calendar.getInstance();
        cal_today.set(Calendar.HOUR_OF_DAY, 23);
        cal_today.set(Calendar.MINUTE, 59);
        cal_today.set(Calendar.SECOND, 59);
        cal_today.add(Calendar.DAY_OF_MONTH,7);

        if(cal.getTimeInMillis()>=cal_today.getTimeInMillis()||cal.getTimeInMillis()<System.currentTimeMillis()) {
            bookingArray[1] = "";
            bookingArray[3]= "outofreach";
            return bookingArray;
        }

        if(!checkdate_internal(year+"-"+month+"-"+day, institution))
        {
            bookingArray[1] = "";
            bookingArray[3]= "outofdate";
            return bookingArray;
        }

        //convert duration to milliseconds
        long duration_ms = Integer.parseInt(duration)*60*1000;
        Timestamp end_sql = new Timestamp(cal.getTimeInMillis()+duration_ms);

        if(!checktime_internal(start_sql, end_sql, institution)) {
            bookingArray[1] = "";
            bookingArray[3] = "outoftime";
            return bookingArray;
        }

        ArrayList<HashMap<String, Object>> result = new ArrayList<>();

        String area_query = "";

        if(!area.equals("no selection")) area_query = "and area = '"+area.trim()+"'";

        int x = 0;

        while(secured_area) {
            try {
                System.out.println("Blocked booking thread!");
                Thread.sleep(100);


            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            x++;
            if(x>=50) {
                bookingArray[3] = "concurrent_error";
                secured_area = false;
                return bookingArray;
            }

            if(!secured_area) break;
        }

        //semaphore
        secured_area = true;

        SQLHub hub = new SQLHub(p);
        if(fitting.isEmpty())
            result = hub.getMultiData("select id, area from workspace where institution = '"+institution.trim()+"' "+area_query, "bookingservice");
        else
        {
            String fitting_query = "";
            for(String f:fitting) {
                if(f.equals("kein PC"))
                    fitting_query = "and fitting not like '%PC%'";
                else if(f.equals("mit Strom"))
                    fitting_query = "and fitting not like '%ohne Strom%'";
                else
                    fitting_query+= "and fitting like '%"+f+"%'";
            }

            String query = "select id, area from workspace where institution = '"+institution.trim()+"' "+area_query+" "+fitting_query;

            //System.out.println(query);

            result = hub.getMultiData(query, "bookingservice");
        }

        Collections.shuffle(result);

        boolean found_workplace = false;

        int workspace_id = -1;
        String found_area = "";
        for(HashMap<String, Object> workspace:result) {
            workspace_id = (int)workspace.get("id");
            found_area = (String)workspace.get("area");
            SQLHub hub_intern = new SQLHub(p);

            ArrayList<HashMap<String, Object>> possible_conflicts = hub_intern.getMultiData("select start, end from booking where workspaceId ="+workspace_id+" and institution = '"+institution.trim()+"'","bookingservice");
            boolean trapped = false;
            for(HashMap<String, Object> pc:possible_conflicts) {
                long a = ((Timestamp)pc.get("start")).getTime();
                long b = ((Timestamp)pc.get("end")).getTime();

                long a1 = start_sql.getTime();
                long b1 = end_sql.getTime();

                if(a1<=a&&b1>=b) trapped = true;
                if(a1>=a&&b1<=b) trapped = true;
                if(a1>=a&&a1<=b&&b1>=b) trapped = true;
                if(a1<=a&&b1>=a&&b1<=b) trapped = true;

                if(trapped) break;
            }

            if(!trapped) found_workplace = true;

            if(found_workplace) break;
        }

        if(found_workplace) {

            area = found_area;

            if(p.getProperty("check_concurrently_booking", "true").equals("true")) {
                boolean check = checkConcurrentlyBooking(readernumber, start_sql, end_sql, institution);
                if (check) {
                    bookingArray[3] = "concurrently_booking";
                    secured_area = false;
                    return bookingArray;
                }
            }

            //generate bookingcode, uses randomized UUID
            bookingArray[0] = UUID.randomUUID().toString();
            bookingArray[1] = String.valueOf(workspace_id);

            String email = new LiberoManager(p).getMailAdress(readernumber, token);
            sendMail(email, readernumber, institution, area, workspace_id, day, month, year, start_sql, end_sql, bookingArray[0]);

            bookingArray[2] = email;

            SQLHub hub_intern = new SQLHub(p);
            hub_intern.executeData("insert into booking (workspaceId, start, end, readernumber, bookingCode, institution) values ('" + workspace_id + "','" + start_sql + "','" + end_sql + "','" + readernumber + "','"+bookingArray[0]+"','"+institution.trim()+"')", "bookingservice");
            hub_intern.executeData("update user set past = past+"+Integer.parseInt(duration)+" where readernumber = '"+readernumber+"'", "bookingservice");
        }

        secured_area = false;

        //autologout
        if(bookingArray[1]!="")
        {
            syslogout(readernumber, token);
        }

        return bookingArray;
    }

    /**
     * Logout eines Nutzers via Angabe der Parameter readernumber und des Libero-Tokens
     *
     * @param readernumber
     * @param token
     */

    private void syslogout(String readernumber, String token) {
        if(tokenmap.containsKey(readernumber)) {
            if (tokenmap.get(readernumber).equals(token)) {
                tokenmap.remove(readernumber);
                new LiberoManager(p).close(token);
                categorymap.remove(readernumber);
                tokentimes.remove(readernumber);
            }
        }

        user_counter--;
    }

    /**
     * API-Endpunkt für die Abfrage aller Buchungen eines Nutzers via Parameter readernumber
     * Content-Type: application/json
     *
     * @param rc
     */

    private void checkReservation(RoutingContext rc) {
        String readernumber = rc.request().formAttributes().get("readernumber");

        SQLHub sqlHub = new SQLHub(p);
        ArrayList<HashMap<String, Object>> resultlist = sqlHub.getMultiData("select * from booking where readernumber = '"+readernumber+"'","bookingservice");

        JsonObject json_all = new JsonObject();
        JsonArray json_array = new JsonArray();
        for(HashMap<String, Object> entry:resultlist) {

            JsonObject json = new JsonObject();

            for(String key:entry.keySet()) {
                json.put(key, String.valueOf(entry.get(key)));
            }
            json_array.add(json);

        }

        json_all.put("result", json_array);
        rc.response().headers().add("Content-type","application/json");
        rc.response().end(json_all.encodePrettily());
    }

    /**
     * API-Endpunkt für den Tagesbuchungsplan. Hier können Mitarbeiter über die Angabe der Institution die Belegung
     * für einen bestimmten Tag einsehen.
     *
     * @param rc
     */

    private void plan(RoutingContext rc) {
        String token = rc.request().formAttributes().get("token");
        String institution = rc.request().formAttributes().get("institutions");
        String date = rc.request().formAttributes().get("datepicker");

        Calendar cal = Calendar.getInstance();
        cal.set(Integer.parseInt(date.split("-")[0]), Integer.parseInt(date.split("-")[1])-1, Integer.parseInt(date.split("-")[2]));

        int y = cal.get(Calendar.YEAR);
        int m = cal.get(Calendar.MONTH)+1;
        int d = cal.get(Calendar.DAY_OF_MONTH);

        String today = y+"-"+(m<10 ? "0"+m : m)+"-"+(d<10 ? "0"+d : d)+" 00:00:00";

        cal.add(Calendar.DAY_OF_MONTH, 1);

        y = cal.get(Calendar.YEAR);
        m = cal.get(Calendar.MONTH)+1;
        d = cal.get(Calendar.DAY_OF_MONTH);

        String tomorrow = y+"-"+(m<10 ? "0"+m : m)+"-"+(d<10 ? "0"+d : d)+" 00:00:00";

        if(!tokenmapma.containsValue(token)) {

            rc.response().end();
            return;
        }

        SQLHub sqlHub = new SQLHub(p);
        ArrayList<HashMap<String, Object>> resultlist = sqlHub.getMultiData("select * from booking where institution = '"+institution+"' and start >= '"+today+"' and end <= '"+tomorrow+"' order by workspaceId","bookingservice");

        JsonObject json_all = new JsonObject();
        JsonArray json_array = new JsonArray();
        for(HashMap<String, Object> entry:resultlist) {

            JsonObject json = new JsonObject();

            for(String key:entry.keySet()) {
                json.put(key, String.valueOf(entry.get(key)));
            }
            json_array.add(json);

        }

        json_all.put("result", json_array);

        rc.response().headers().add("Content-type","application/json");

        rc.response().end(json_all.encodePrettily());
    }

    /**
     * Prüfung auf eine nebenläufige Buchung in der gleichen Institution des selben Nutzers.
     *
     * @param readernumber
     * @param start
     * @param end
     * @param institution
     * @return
     */

    private boolean checkConcurrentlyBooking(String readernumber, Timestamp start, Timestamp end, String institution) {

        boolean trapped = false;

        SQLHub sqlhub = new SQLHub(p);
        ArrayList<HashMap<String, Object>> resultlist = sqlhub.getMultiData("select start, end from booking where readernumber = '"+readernumber+"' and institution = '"+institution+"'","bookingservice");
        for(HashMap<String, Object> entry:resultlist)
        {
            long a = ((Timestamp)entry.get("start")).getTime();
            long b = ((Timestamp)entry.get("end")).getTime();

            long a1 = start.getTime();
            long b1 = end.getTime();

            if(a1<=a&&b1>=b) trapped = true;
            if(a1>=a&&b1<=b) trapped = true;
            if(a1>=a&&a1<=b&&b1>=b) trapped = true;
            if(a1<=a&&b1>=a&&b1<=b) trapped = true;

            if(trapped) return true;
        }

        return false;
    }

    /**
     * Versand der Stornomail
     *
     * @param address
     * @param readernumber
     * @param institution
     * @param workspaceId
     * @param start
     * @param end
     */

    private void sendStornoMail(String address, String readernumber, String institution, int workspaceId, Timestamp start, Timestamp end) {
        try {

            FileInputStream fis = new FileInputStream("config/storno.template");
            byte buffer[] = fis.readAllBytes();
            fis.close();

            String data = new String(buffer);
            data = data.replaceAll("###readernumber###",readernumber);
            data = data.replaceAll("###institution###",institution);
            data = data.replaceAll("###id###",String.valueOf(workspaceId));

            String day = String.valueOf(start.toLocalDateTime().getDayOfMonth()).length()<2 ? "0"+start.toLocalDateTime().getDayOfMonth() : ""+start.toLocalDateTime().getDayOfMonth();
            String month = String.valueOf(start.toLocalDateTime().getMonth().getValue()).length()<2 ? "0"+start.toLocalDateTime().getMonth().getValue() : ""+start.toLocalDateTime().getMonth().getValue();
            String year = String.valueOf(start.toLocalDateTime().getYear());

            String mydate = day+"."+month+"."+year;
            data = data.replaceAll("###date###",mydate);

            String mytimeslot = start.toLocalDateTime().format(DateTimeFormatter.ofPattern("HH:mm"))+" - "+end.toLocalDateTime().format(DateTimeFormatter.ofPattern("HH:mm"));
            data = data.replaceAll("###timeslot###",mytimeslot);


            QuickMail.sendMail("mailservice@ub.uni-leipzig.de", "UBL Service", address, data, "Stornierung");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Versand der Buchungsbestätigungs-Mail
     *
     * @param address
     * @param readernumber
     * @param institution
     * @param area
     * @param workspaceId
     * @param day
     * @param month
     * @param year
     * @param start
     * @param end
     * @param bookingCode
     */

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

            QuickMail.sendMail("mailservice@ub.uni-leipzig.de", "UBL Service", address, data, "Arbeitsplatzbuchung an der UBL");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * API-Endpunkt für die Buchung.
     *
     * @param rc
     */

    private void booking(RoutingContext rc) {

        call_stats[3]++;

        String institution = rc.request().formAttributes().get("institution");
        String area = rc.request().formAttributes().get("area");
        String from_date = rc.request().formAttributes().get("from_date");
        String from_time = rc.request().formAttributes().get("from_time");
        String until_time = rc.request().formAttributes().get("until_time");
        List<String> fitting = rc.request().formAttributes().getAll("fitting");
        String readernumber = rc.request().formAttributes().get("readernumber");
        String token = rc.request().formAttributes().get("token");

        if(!tokenmap.get(readernumber).equals(token)) {
            JsonObject json = new JsonObject();
            json.put("message", "sessionerror");
            rc.response().end(json.encodePrettily());

            return;
        }

        String year, month, day, timeslot;

        year = from_date.split("-")[0];
        month = from_date.split("-")[1];
        day = from_date.split("-")[2];

        timeslot = from_time.replaceAll(":","")+"-"+until_time.replaceAll(":","");

        String bookingArray[] = do_booking(institution, area, day, month, year, timeslot, readernumber, token, fitting);

        JsonObject json = new JsonObject();
        json.put("bookingCode", bookingArray[0]);
        json.put("workspaceId", bookingArray[1]);
        json.put("email", bookingArray[2]);
        json.put("message", bookingArray[3]);

        rc.response().end(json.encodePrettily());

    }

    /**
     * Prüft, ob das Buchungsdatum an einem Schließtag stattfindet. Die Schließtage können periodisch wiederkehrend sein "recurrent closure days",
     * wie beispielsweise Wochenenden oder ob es spezielle Schließtage sind "special closure days". Sie werden in der Datei
     * timeslots.xml konfiguriert.
     *
     * @param date
     * @param institution
     * @return
     */

    public boolean checkdate_internal(String date, String institution) {

        Calendar cal = Calendar.getInstance();
        //year, month, date
        int year = Integer.parseInt(date.split("-")[0]);
        int month = Integer.parseInt(date.split("-")[1])-1;
        int day = Integer.parseInt(date.split("-")[2]);

        cal.set(year, month, day);
        int dow = cal.get(Calendar.DAY_OF_WEEK); //Sonntag = 1

        String closuredays = timeslots.get(institution).getString("recclosuredays");
        String specclosuredays = timeslots.get(institution).getString("specclosuredays");

        if(specclosuredays!=null) {
            for (String spec : specclosuredays.split(",")) {

                int s_year = Integer.parseInt(spec.split("[.]")[2]);
                int s_month = Integer.parseInt(spec.split("[.]")[1])-1;
                int s_day = Integer.parseInt(spec.split("[.]")[0]);

                if (s_day == day && s_month == month && s_year == year) {

                    return false;
                }
            }
        }

        if(closuredays==null) {

            return true;
        }

        for(String cd:closuredays.split(","))
            if(dow==Integer.parseInt(cd))
                return false;

        return true;

    }

    /**
     * Prüft, ob eine Buchungszeit innerhalb der Öffnungszeiten ist.
     *
     * @param start
     * @param end
     * @param institution
     * @return
     */

    private boolean checktime_internal(Timestamp start, Timestamp end, String institution) {
        //start same day like end

        //attention: getDayOfWeek starts with 1 = Monday!!!
        int dow = start.toLocalDateTime().getDayOfWeek().getValue();
        //we "convert" the value

        if(dow!=7) dow+=1;
        else dow-=6;

        Calendar a = Calendar.getInstance();
        a.set(Calendar.HOUR_OF_DAY, start.toLocalDateTime().getHour());
        a.set(Calendar.MINUTE, start.toLocalDateTime().getMinute());

        Calendar b = Calendar.getInstance();
        b.set(Calendar.HOUR_OF_DAY, end.toLocalDateTime().getHour());
        b.set(Calendar.MINUTE, end.toLocalDateTime().getMinute());

        boolean has_own_openings = false;

        for(int i=0;i<timeslots.get(institution).getJsonArray("interval").getList().size();i++) {
            if (timeslots.get(institution).getJsonArray("interval").getJsonObject(i).getString("day")==null) continue;
            if (timeslots.get(institution).getJsonArray("interval").getJsonObject(i).getString("day").trim().equals(""+dow)) {
                //es existiert eine spezifizierte öffnungszeit


                //Opening
                Calendar a1 = Calendar.getInstance();
                a1.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeslots.get(institution).getJsonArray("interval").getJsonObject(i).getString("from").split(":")[0]));
                a1.set(Calendar.MINUTE, Integer.parseInt(timeslots.get(institution).getJsonArray("interval").getJsonObject(i).getString("from").split(":")[1]));
                a1.set(Calendar.SECOND, 0);
                a1.set(Calendar.MILLISECOND, 0);


                //Closing
                Calendar a2 = Calendar.getInstance();
                a2.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeslots.get(institution).getJsonArray("interval").getJsonObject(i).getString("until").split(":")[0]));
                a2.set(Calendar.MINUTE, Integer.parseInt(timeslots.get(institution).getJsonArray("interval").getJsonObject(i).getString("until").split(":")[1])+1);
                a2.set(Calendar.SECOND, 0);
                a2.set(Calendar.MILLISECOND, 0);


                has_own_openings = true;

                if (a.before(a1)) return false;
                if (b.after(a2)) return false;
            }
        }

        if(!has_own_openings)
        {
            Calendar a1 = Calendar.getInstance();
            a1.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeslots.get(institution).getJsonArray("interval").getJsonObject(0).getString("from").split(":")[0]));
            a1.set(Calendar.MINUTE, Integer.parseInt(timeslots.get(institution).getJsonArray("interval").getJsonObject(0).getString("from").split(":")[1]));
            a1.set(Calendar.SECOND, 0);
            a1.set(Calendar.MILLISECOND, 0);

            Calendar a2 = Calendar.getInstance();
            a2.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeslots.get(institution).getJsonArray("interval").getJsonObject(0).getString("until").split(":")[0]));
            a2.set(Calendar.MINUTE, Integer.parseInt(timeslots.get(institution).getJsonArray("interval").getJsonObject(0).getString("until").split(":")[1])+1);
            a2.set(Calendar.SECOND, 0);
            a2.set(Calendar.MILLISECOND, 0);

            has_own_openings = true;

            if(a.before(a1)) return false;
            if(b.after(a2)) return false;

        }

        return true;
    }

    /**
     * API-Endpunkt für die ajax-basierten Requests aus dem Buchungsformular, um zu Prüfen, ob das gewählte Datum überhaupt gebucht werden kann.
     *
     * @param rc
     */

    private void checkdate(RoutingContext rc) {

        call_stats[4]++;

        String date = rc.getBodyAsJson().getString("date");
        String institution = rc.getBodyAsJson().getString("institution");

        boolean checkdateboolean = checkdate_internal(date, institution);
        if(checkdateboolean) rc.response().end("true");

        rc.response().end("false");

    }

    /**
     * API-Endpunkt für die Stornierung durch Mitarbeiter.
     *
     * @param rc
     */
    private void mastorno(RoutingContext rc) {

        String readernumber = rc.getBodyAsJson().getString("readernumber");
        String bookingCode = rc.getBodyAsJson().getString("bookingCode");
        String token = rc.getBodyAsJson().getString("token");

        if(!tokenmapma.containsValue(token)) {
            rc.response().end();
            return;
        }

        String msg = storno_core(token, bookingCode, readernumber);

        rc.response().headers().add("Content-type", "application/json");

        JsonObject json = new JsonObject();
        json.put("message", msg);

        rc.response().end(json.encodePrettily());

    }

    /**
     * Storno-Funktion. Storniert eine Buchung unter Angabe des Buchungscodes, der Bibliotheksnummer(Lesekartennummer),
     * und dem Libero-Token (desjenigen, der die Stornierung auslöst).
     *
     * @param token
     * @param bookingcode
     * @param readernumber
     * @return
     */

    private String storno_core(String token, String bookingcode, String readernumber) {
        String msg = new String();

        SQLHub hub = new SQLHub(p);
        HashMap<String, Object> map = hub.getSingleData("select * from booking where bookingCode='"+bookingcode+"' and readernumber='"+readernumber+"'", "bookingservice");
        if(map.get("bookingCode")!=null)
        {
            Timestamp a = (Timestamp)map.get("start");
            Timestamp b = (Timestamp)map.get("end");

            String institution = (String)map.get("institution");
            int workspaceId = (int)map.get("workspaceId");


            if(b.getTime()<=System.currentTimeMillis()) {
                msg = "Zeitlich zurückliegende Buchungen können nicht storniert werden!";
            }
            else {

                SQLHub hub2 = new SQLHub(p);

                String email = new LiberoManager(p).getMailAdress(readernumber, token);

                long c = System.currentTimeMillis();
                if(c>=a.getTime()&&c<=b.getTime())
                {
                    long duration = (c - a.getTime())/(1000*60); //in minutes

                    hub2.executeData("update booking set start = '"+a.toString()+"', end = '"+Timestamp.from(Instant.ofEpochMilli(c)).toString()+"' where bookingCode='" + bookingcode + "' and readernumber='" + readernumber + "'", "bookingservice");
                    hub2.executeData("update user set past = past-" + duration + " where readernumber = '" + readernumber + "'", "bookingservice");

                    msg = "Die Restlaufzeit Ihrer Buchung wurde gelöscht.";

                    sendStornoMail(email, readernumber, institution, workspaceId, Timestamp.from(Instant.ofEpochMilli(c)), b);

                }else {

                    long duration = (b.getTime() - a.getTime())/(1000*60); //in minutes
                    hub2.executeData("delete from booking where bookingCode='" + bookingcode + "' and readernumber='" + readernumber + "'", "bookingservice");
                    hub2.executeData("update user set past = past-" + duration + " where readernumber = '" + readernumber + "'", "bookingservice");

                    msg = "Ihre Buchung wurde gelöscht.";
                    sendStornoMail(email, readernumber, institution, workspaceId, a, b);
                }

            }

        }
        else {
            msg = "Der angegebene Buchungscode wurde nicht gefunden!";
        }

        return msg;

    }

    /**
     * API-Endpunkt für die Stornierung von Buchungen durch Nutzer.
     *
     * @param rc
     */

    private void storno(RoutingContext rc) {

        call_stats[5]++;

        String readernumber = rc.request().formAttributes().get("readernumber");
        String password = rc.request().formAttributes().get("password");
        String bookingcode = rc.request().formAttributes().get("bookingcode");

        String logvalue[] = new LiberoManager(p).login(readernumber, password);

        String token = logvalue[0];
        String msg = logvalue[1];

        //Wenn token==null, dann war der Login nicht möglich
        if(token==null||msg.equals("Wrong readernumber or password")) {
            token="null";
            msg = "Lesekartennummer oder Password falsch.";
        }

        if(!token.equals("null")) {

            msg = storno_core(token, bookingcode, readernumber);

        }

        rc.response().headers().add("Content-type", "application/json");

        JsonObject json = new JsonObject();
        json.put("message", msg);

        rc.response().end(json.encodePrettily());

    }

    /**
     * API-Endpunkt für das Logout.
     *
     * @param rc
     */
    private void logout(RoutingContext rc) {

        call_stats[6]++;

        String token = rc.getBodyAsJson().getString("token");
        String readernumber = rc.getBodyAsJson().getString("readernumber");

        syslogout(readernumber, token);

        rc.response().end();

    }

    /**
     * API-Endpunkt für das Logout (Mitarbeiter).
     *
     * @param rc
     */
    private void malogout(RoutingContext rc) {
        String token = rc.getBodyAsJson().getString("token");
        String username = rc.getBodyAsJson().getString("username");

        new LiberoManager(p).close(token);

        tokenmapma.remove(username);
        tokentimesma.remove(username);

        rc.response().end();
    }

    /**
     *   API-Endpunkt für das Login.
     *
     *   Erwartet werden die Variablen readernumber und password
     *   Content-Type: application/x-www-form-urlencoded
     */
    private void login(RoutingContext rc) {

        call_stats[7]++;

        if(tokenmap.keySet().size()>=Integer.parseInt(p.getProperty("concurrent_user", "25"))) {
            System.out.println("too many users: "+tokenmap.keySet().size());

            JsonObject answer_object = new JsonObject();
            answer_object.put("token", "null");
            answer_object.put("msg", "Achtung, zuviele Nutzer sind gerade angemeldet. Bitte versuchen Sie es später noch einmal.");

            rc.response().headers().add("Content-type","application/json");

            rc.response().end(answer_object.encodePrettily());
            return;
        }

        user_counter++;

        String readernumber = rc.request().formAttributes().get("readernumber");
        String password = rc.request().formAttributes().get("password");

        //Zugriff auf Libero-Manager, ermittle Token für das Login mit gewählter LKN und Passwort

        String logvalue[] = new LiberoManager(p).login(readernumber, password);

        String token = logvalue[0];
        String msg = logvalue[1];

        //Wenn token==null, dann war der Login nicht möglich
        if(token==null||msg.equals("Wrong readernumber or password")) {
            token="null";
            msg = "Lesekartennummer oder Password falsch.";
            user_counter--;
        }

        if(!token.equals("null")) {

            //add relation readernumber <-> token to the map

            if(tokenmap.containsKey(readernumber)) user_counter--;

            tokenmap.put(readernumber, token);
            tokentimes.put(readernumber, System.currentTimeMillis());

            //prüfe auf existenz des nutzers in der DB und lege diesen ggf. an
            SQLHub hub = new SQLHub(p);
            if(hub.getSingleData("select * from user where readernumber = '"+readernumber+"'", "bookingservice").get("readernumber")==null)
                hub.executeData("insert into user (readernumber, quota, past) values ('"+readernumber.trim()+"',600,0)", "bookingservice");

        }

        if(msg.equals("Wrong category")) {
            msg = "Leider gibt es für Ihre Nutzerkategorie keine Buchungserlaubnis.";
            user_counter--;
        }

        JsonObject answer_object = new JsonObject();
        answer_object.put("token", token);
        answer_object.put("msg", msg);

        rc.response().headers().add("Content-type","application/json");

        rc.response().end(answer_object.encodePrettily());
    }

    /**
     * API-Endpunkt für das Login für Mitarbeiter.
     *
     * @param rc
     */

    private void malogin(RoutingContext rc) {
        String username = rc.request().formAttributes().get("username");
        String passwd = rc.request().formAttributes().get("passwd");

        String logvalue[] = new LiberoManager(p).malogin(username, passwd);
        String token = logvalue[0];
        String msg = logvalue[1];

        if(token==null||msg.equals("Wrong username or password")) {
            token="null";
            msg = "Nutzername oder Password falsch.";
        }
        if(!token.equals("null")) {
            tokenmapma.put(username, token);
            tokentimesma.put(username, System.currentTimeMillis());
        }

        JsonObject answer_object = new JsonObject();
        answer_object.put("token", token);
        answer_object.put("msg", msg);

        rc.response().headers().add("Content-type","application/json");

        rc.response().end(answer_object.encodePrettily());

    }


    /**
     * Inititalisierung.
     */
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


                JsonObject jso = new JsonObject();

                JsonArray json_array = new JsonArray();

                for(Element e_interval : e_inst.getChildren("interval")) {

                    JsonObject jso_interval = new JsonObject();


                    jso_interval.put("from", e_interval.getAttributeValue("from"));
                    jso_interval.put("until", e_interval.getAttributeValue("until"));
                    jso_interval.put("day", e_interval.getAttributeValue("day"));

                    json_array.add(jso_interval);

                }

                jso.put("interval", json_array);

                String e_recclosuredays = e_inst.getChildText("recurrentClosureDays");
                String e_specclosuredays = e_inst.getChildText("specialClosureDays");

                if(e_recclosuredays!=null)
                    jso.put("recclosuredays", e_recclosuredays.trim());

                if(e_specclosuredays!=null)
                    jso.put("specclosuredays", e_specclosuredays.trim());

                timeslots.put(e_inst.getAttributeValue("name"), jso);

            }

        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Cleanup-Thread: Räumt alle verwaisten Nutzer nach 15 Minuten auf und gibt somit den Platz frei.
        Thread cleanup = new Thread(){
          public void run() {

              while(true) {
                  ArrayList<String> candidates = new ArrayList<>();

                  for (String t : tokentimes.keySet()) {
                      long l = tokentimes.get(t);

                      if (System.currentTimeMillis() - l >= (15 * 60 * 1000))
                      {
                          candidates.add(t);
                          user_counter--;
                      }
                  }

                  for (String t : candidates) {
                      Iterator<String> iter = tokenmap.keySet().iterator();
                      while(iter.hasNext()) {
                          String readernumber = iter.next();
                          if (readernumber.equals(t)) {
                              System.out.println("Entferne BN: "+readernumber);
                              iter.remove();
                              categorymap.remove(readernumber);
                              tokentimes.remove(t);
                          }
                      }
                  }

                  candidates.clear();

                  try {
                      Thread.sleep(60 * 1000);
                  } catch (InterruptedException e) {
                      e.printStackTrace();
                  }
              }
          }
        };

        cleanup.start();

    }

    public static void main(String args[])
    {
        new BookingService();
    }

}
