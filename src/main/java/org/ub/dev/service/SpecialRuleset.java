package org.ub.dev.service;

import java.util.ArrayList;
import java.util.Calendar;

public class SpecialRuleset {

    String typeOfRuleset, area, library;
    Calendar from, until;
    ArrayList<Integer> workspaceIDs;

    public SpecialRuleset(String typeOfRuleset, String library){
        this.typeOfRuleset=typeOfRuleset;
        this.library = library;
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

}
