package org.ub.dev.service;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.ub.dev.tools.Tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Calendar;

public class RulesetLoader {

    ArrayList<SpecialRuleset> rsrlist;

    public RulesetLoader(ArrayList<SpecialRuleset> rulesets) {
        rsrlist = rulesets;
        init();
    }

    public static Object preventNull(Object s) {


        if(s.getClass().getName().equals("java.lang.String")) {
            String xs = (String)s;
            if(xs==null) return "";
        }

        return s;
    }

    public static void toFile(ArrayList<SpecialRuleset> rulesets) {

        Document doc = new Document();
        Element xml_rulesets = new Element("rulesets");
        doc.setRootElement(xml_rulesets);

        for(SpecialRuleset srs:rulesets) {

            String type = srs.typeOfRuleset;
            String library = srs.library;
            String name = srs.name;

            String from = Tools.getStringFromCal(srs.from);
            String until = Tools.getStringFromCal(srs.until);
            String area = (String)preventNull(srs.area);
            String idlist = new String();

            if(srs.workspaceIDs==null) idlist="";
            else {

                for (int id : srs.workspaceIDs) {
                    idlist += "" + id + ",";
                }
                if (!idlist.isEmpty())
                    idlist = idlist.substring(0, idlist.length() - 1);
            }

            String info = (String)preventNull(srs.info);

            String day = srs.day;
            String opening = srs.opening;
            String closing = srs.closing;

            Element ruleset = new Element("ruleset");
            ruleset.setAttribute("name", name);
            ruleset.setAttribute("type", type);
            ruleset.setAttribute("library", library);
            ruleset.setAttribute("from", from);
            ruleset.setAttribute("until", until);
            ruleset.addContent(new Element("area").setText(area));
            ruleset.addContent(new Element("workspaceIDs").setText(idlist));
            ruleset.addContent(new Element("info").setText(info));

            if(day!=null) {

                Element closuremodification = new Element("closuremodification");
                closuremodification.setAttribute("day",day);

                if(!opening.isEmpty()&&opening!=null)
                {
                    closuremodification.setAttribute("opening",opening);
                }

                if(!closing.isEmpty()&&closing!=null) {
                    closuremodification.setAttribute("closing", closing);
                }

                ruleset.addContent(closuremodification);

            }

            xml_rulesets.addContent(ruleset);
        }

        try {
            if(new File("config/rulesets.xml").exists()) Files.copy(Paths.get("config/rulesets.xml"), Paths.get("config/rulesets_"+System.currentTimeMillis()+".bkp"));
            new XMLOutputter(Format.getPrettyFormat()).output(doc, new FileOutputStream("config/rulesets.xml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void init() {
        try {

            if(!new File("config/rulesets.xml").exists()) return;

            Document doc = new SAXBuilder().build("config/rulesets.xml");

            for(Element single:doc.getRootElement().getChildren()) {
                String type = single.getAttributeValue("type");
                String library = single.getAttributeValue("library");
                String from = single.getAttributeValue("from");
                String until = single.getAttributeValue("until");

                String name = single.getAttributeValue("name");

                String area = single.getChildText("area");
                String idlist = single.getChildText("workspaceIDs");
                String info = single.getChildText("info");

                SpecialRuleset srs = new SpecialRuleset(type, library, name);

                if(single.getChild("closuremodification")!=null) {

                    String oc_day = single.getChild("closuremodification").getAttributeValue("day");

                    String opening = single.getChild("closuremodification").getAttributeValue("opening");
                    String closing = single.getChild("closuremodification").getAttributeValue("closing");

                    srs.setClosingModification(oc_day, opening, closing);

                }


                int day = Integer.parseInt(from.split(" ")[0].split("[.]")[0]);
                int month = Integer.parseInt(from.split(" ")[0].split("[.]")[1]);
                int year = Integer.parseInt(from.split(" ")[0].split("[.]")[2]);

                int hour = Integer.parseInt(from.split(" ")[1].split(":")[0]);
                int minute = Integer.parseInt(from.split(" ")[1].split(":")[1]);

                srs.setFrom(Tools.setCalendarOnComplete(day, month, year, hour, minute));

                day = Integer.parseInt(until.split(" ")[0].split("[.]")[0]);
                month = Integer.parseInt(until.split(" ")[0].split("[.]")[1]);
                year = Integer.parseInt(until.split(" ")[0].split("[.]")[2]);

                hour = Integer.parseInt(until.split(" ")[1].split(":")[0]);
                minute = Integer.parseInt(until.split(" ")[1].split(":")[1]);

                srs.setUntil(Tools.setCalendarOnComplete(day, month, year, hour, minute));
                srs.setArea(area);
                srs.setInfo(info);

                if(idlist!=null&&!idlist.isEmpty()) {
                    ArrayList<Integer> ids = new ArrayList<>();
                    for (String item : idlist.split(",")) {
                        ids.add(Integer.parseInt(item));
                    }

                    srs.setWorkspaceIDs(ids);
                }
                rsrlist.add(srs);

            }


        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
