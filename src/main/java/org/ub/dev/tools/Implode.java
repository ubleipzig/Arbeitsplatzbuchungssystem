package org.ub.dev.tools;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Implode - Arbeitsplatzbuchungssystem
 *
 * Klasse für das Zusammenführen von DB-Dateien zu einer gemeinsamen Datenbank-Datei.
 */

public class Implode {

    public Implode() {
        init();
    }

    private void init() {
        File folder = new File("./data/");

        ArrayList<File> filelist = new ArrayList<File>();

        for(File f:folder.listFiles()) {
            filelist.add(f);
        }

        Collections.sort(filelist);

        try {
            FileOutputStream fos = new FileOutputStream("./data/all.db");

        for(File f:filelist) {
            try (FileInputStream fis = new FileInputStream(f)) {
                byte buffer[] = fis.readAllBytes();
                fis.close();

                fos.write(buffer);

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        fos.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void main(String args[]) {
        new Implode();
    }

}
