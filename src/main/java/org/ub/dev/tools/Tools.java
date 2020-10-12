package org.ub.dev.tools;


import io.vertx.core.json.JsonObject;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

public class Tools {

    public static Calendar setCalendarOnDate(int day, int month, int year) {
        Calendar calX = Calendar.getInstance();
        calX.set(Calendar.HOUR_OF_DAY, 0);
        calX.set(Calendar.MINUTE, 0);
        calX.set(Calendar.SECOND, 0);
        calX.set(Calendar.DAY_OF_MONTH, day);
        calX.set(Calendar.MONTH, month-1);
        calX.set(Calendar.YEAR, year);

        return calX;
    }

    public static Date getDateFromms(long ms) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ms);

        return c.getTime();
    }

    public static Calendar setCalendarOnComplete(int day, int month, int year, int hour, int minute) {
        Calendar calX = Calendar.getInstance();
        calX.set(Calendar.HOUR_OF_DAY, hour);
        calX.set(Calendar.MINUTE, minute);
        calX.set(Calendar.SECOND, 0);
        calX.set(Calendar.DAY_OF_MONTH, day);
        calX.set(Calendar.MONTH, month-1);
        calX.set(Calendar.YEAR, year);

        return calX;
    }

    public static String getStringFromCal(Calendar cal) {
        String hour = cal.get(Calendar.HOUR_OF_DAY)<=9 ? "0"+cal.get(Calendar.HOUR_OF_DAY) : String.valueOf(cal.get(Calendar.HOUR_OF_DAY));
        String minute = cal.get(Calendar.MINUTE)<=9 ? "0"+cal.get(Calendar.MINUTE) : String.valueOf(cal.get(Calendar.MINUTE));
        String day = cal.get(Calendar.DAY_OF_MONTH)<=9 ? "0"+cal.get(Calendar.DAY_OF_MONTH) : String.valueOf(cal.get(Calendar.DAY_OF_MONTH));
        String month = (cal.get(Calendar.MONTH)+1)<=9 ? "0"+(cal.get(Calendar.MONTH)+1) : String.valueOf(cal.get(Calendar.MONTH)+1);
        String year = String.valueOf(cal.get(Calendar.YEAR));

        return day+"."+month+"."+year+" "+hour+":"+minute;
    }

    public static void JSONHashMaptoXML(HashMap<String, JsonObject> map) {
        Element root = new Element("institutions");
        for(String institution:map.keySet()) {
            JsonObject jso = map.get(institution);
            Element institution_element = new Element("institution");
            institution_element.setAttribute("name", institution);
            for(int i=0;i<jso.getJsonArray("interval").size();i++)
            {
                JsonObject jso_interval = jso.getJsonArray("interval").getJsonObject(i);
                String from = jso_interval.getString("from");
                String until = jso_interval.getString("until");
                Element interval = new Element("interval");
                interval.setAttribute("from", from);
                interval.setAttribute("until",until);

                if(jso_interval.getValue("day")!=null) {
                    interval.setAttribute("day",jso_interval.getString("day"));
                }
                institution_element.addContent(interval);
            }

            if(jso.containsKey("recclosuredays")) {
                Element recclosuredays_element = new Element("recurrentClosureDays");
                recclosuredays_element.setText(jso.getString("recclosuredays"));
                institution_element.addContent(recclosuredays_element);
            }

            if(jso.containsKey("specclosuredays")) {
                Element specclosuredays_element = new Element("specialClosureDays");
                specclosuredays_element.setText(jso.getString("specclosuredays"));
                institution_element.addContent(specclosuredays_element);
            }

            root.addContent(institution_element);


        }

        Document doc = new Document();
        doc.setRootElement(root);

        try {
            Files.copy(Paths.get("config/timeslots.xml"), Paths.get("config/timeslots_"+System.currentTimeMillis()+".xml"));
            new XMLOutputter(Format.getPrettyFormat()).output(doc, new FileOutputStream("config/timeslots.xml"));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
