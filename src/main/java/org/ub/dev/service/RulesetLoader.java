package org.ub.dev.service;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.ub.dev.tools.Tools;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;

public class RulesetLoader {

    ArrayList<SpecialRuleset> rsrlist;

    public RulesetLoader(ArrayList<SpecialRuleset> rulesets) {
        rsrlist = rulesets;
        init();
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

                SpecialRuleset srs = new SpecialRuleset(type, library, name);
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

                ArrayList<Integer> ids = new ArrayList<>();
                for(String item:idlist.split(",")) {
                    ids.add(Integer.parseInt(item));
                }

                srs.setWorkspaceIDs(ids);

                rsrlist.add(srs);

            }


        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
