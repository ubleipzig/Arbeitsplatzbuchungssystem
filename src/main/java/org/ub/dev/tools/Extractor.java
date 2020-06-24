package org.ub.dev.tools;

import org.ub.dev.service.DataCore;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Klasse um Daten nach spezifizierten Parametern aus der Datenbank zu extrahieren und in CSV-Format bereitzustellen
 */
public class Extractor {

    public Extractor() {
        init();
    }

    private void init() {
        Properties p = new Properties();
        try {
            p.load(new FileInputStream("config/config.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        DataCore dc = new DataCore("Bibliotheca Albertina", "2020-06-08 00:00:00", "2020-06-10 00:00:00", p);
        System.out.println(dc.getData());
    }

    public static void main(String args[]){
        new Extractor();
    }

}
