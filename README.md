# Arbeitsplatzbuchung

## Systemvoraussetzungen

* Java 11 +
* MySQL-DB (z.B. MariaDB)

## Einrichtung der Datenbank / Datenbankschema

Datenbank "bookingservice" enthält folgende Tabellen:
````sql
workspace (int id, text institution, text area, text fitting)
````
* id - Nummer des Arbeitsplatzes
* institution - Name des Standortes
* area - Name des Bereichs (im Standort)
* fitting - Auflistung von Ausstattungsmerkmalen des Arbeitsplatzes

````sql
user (int id, varchar(100) readernumber, int quota, int past)
````

* id - Nummer des Nutzers
* readernumber - Bibliotheksnummer/Lesekartennummer des Nutzers
* quota - Nutzerquota
* past - Bereits gebuchte Zeit in Minuten

````sql
booking (int workspaceId, Timestamp start, Timestamp end, varchar(100) readernumber, varchar(255) bookingCode, text institution)
````
* workspaceId - Nummer des Arbeitsplatzes
* start - Startzeitpunkt der Buchung
* end - Endzeitpunkt der Buchung
* readernumber - Bibliotheks-/Lesekartennummer
* bookingCode - Buchungscode
* institution - Standort


Dokumentation folgt in Kürze
