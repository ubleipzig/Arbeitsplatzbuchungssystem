package org.ub.dev.service;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * org.ub.dev.service.SpecialRuleset - Klasse für die Sonderregel
 *
 */
public class SpecialRuleset {

    String typeOfRuleset, area, library, name, info;
    Calendar from, until;
    ArrayList<Integer> workspaceIDs;
    String day, opening, closing;

    /**
     * Konstruktor, erwartet Regeltyp, Bibliothek, Regelname
     * @param typeOfRuleset
     * @param library
     * @param name
     */
    public SpecialRuleset(String typeOfRuleset, String library, String name){
        this.typeOfRuleset=typeOfRuleset;
        this.library = library;
        this.name = name;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public String getTypeOfRuleset() {
        return typeOfRuleset;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public void setFrom(Calendar cal) {
        from = (Calendar)cal.clone();
    }

    public Calendar getFromAsCal() {
        return (Calendar)from.clone();
    }

    public long getFromAsLong() {
        return from.getTimeInMillis();
    }

    public void setUntil(Calendar cal) {
        until = (Calendar)cal.clone();
    }

    public Calendar getUntilAsCal() {
        return (Calendar)until.clone();
    }

    public long getUntilAsLong() {
        return until.getTimeInMillis();
    }

    public void setWorkspaceIDs(ArrayList<Integer> wIDs) {
        this.workspaceIDs = wIDs;
    }

    public void setClosingModification(String day, String opening, String closing) {
        this.day = day;
        this.opening = opening;
        this.closing = closing;
    }

}
