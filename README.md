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

## Konfiguration

### config.properties

Allgemeine Konfigurationseinstellungen lassen sich in der config.properties im config-Ordner vornehmen.

Die Einstellungen:

* *authentication_url* und *library_url* sind für die Libero-Anbindung notwenig
* *rest_port* definiert den Port für die API
* *categories* definiert die zugelassenen Kategorien aus Libero
* *concurrent_user* definiert die maximale Anzahl an Nutzern, die sich gleichzeitig im System befinden können
* *check_concurrently_booking* ist ein Schalter für die Prüfung auf zeitgleiche Buchungen des Nutzers am gleichen Standort
* *timestamp_limit* definiert den Zeitpunkt, bis zu dem die Datenbank bereinigt werden kann
* *database_user* und *database_password* sind die Login-Daten für die Datenbank

### mail.template und storno.template

Beide Dateien stellen ein Template für den Mailversand dar. Damit kann der Text für die Buchungsmail und die Stornomail voreingestellt werden. Durch Platzhalter in der Form ###*Variablenname*### wird dann im System die Mail komplettiert und mit den entsprechenden Daten vervollständigt.

### timeslots.xml

Die timeslots.xml-Datei konfiguriert die Schließtage/Öffnungszeiten für einen Standort.

Jeder Standort kann über das *institution*-Tag dabei einzeln individuell konfiguriert werden.

````xml
<institutions>
    <institution name="Standort 1">
      
        ...
      
    </institution>
</institutions>
````

Tagesöffnungszeiten lassen sich über das *interval*-Tag definieren. Dabei gelten die Attribute *from* und *until* in der Form "hh:mm" für den Zeitpunkt der Öffnung bzw. Schließung. Durch Angabe des *day*-Attributes lassen sich die Zeiten für einen spezifizierten Tag einstellen.
Hier das Beispiel für die täglichen Öffnungszeiten von 8 Uhr bis 20 Uhr von Standort 1 (Montag bis Sonntag):

````xml
  <institution name="Standort 1">
    <interval from="08:00" until="20:00"/>
  </institution>
````

Jeder Tag lässt sich folglich innerhalb dieser Grenzen buchen.
Sollen Tage wiederkehrend als Schließtage definiert werden, kann dies über das Tag *recurrentClosureDays* definiert werden.
Wenn das Wochenende als Schließtage eingestellt werden soll, kann dies so aussehen:

````xml
  <institution name="Standort 1">
    <interval from="08:00" until="20:00"/>
    <recurrentClosureDays>
      7,1
    </reccurentClosureDays>
  </institution>
````
Achtung: Im genutzten Kalender beginnt die Woche in der Zählung mit dem **Sonntag**, also mit 1.
Demzufolge sind die Ziffern 7 und 1 die Codierung für Samstag und Sonntag. An beiden Tagen wird das System mit der obigen Konfiguration also keine Buchungen zulassen.

Sollen spezielle sich nicht im Intervall (7 Tage) wiederholende Tage als Schließtage definiert werden, beispielsweise Feiertage oder Brückentage, so kann dies mit Hilfe des *specialClosureDays*-Tag vorgenommen werden. Hier muss das komplette Datum (bzw mehrere Datumsangaben durch Kommata getrennt) angegeben werden.
Beispiel für die Eingabe des 1. Mai 2020 als Schließtag:

````xml
  <institution name="Standort 1">
    <interval from="08:00" until="20:00"/>
    <recurrentClosureDays>
      7,1
    </reccurentClosureDays>
    <specialClosureDays>
      01.05.2020
    </specialClosureDays>
  </institution>
````

Abschließend ein Beispiel für den Einsatz des *day*-Attributes beim *interval*-Tag. Wir gehen hier von einer Öffnungszeit von Montag bis Donnerstag von 8 Uhr bis 20 Uhr aus, Freitag von 8 Uhr bis 16 Uhr und Samstag/Sonntag soll der Standort geschlossen sein:

````xml
  <institution name="Standort 1">
    <interval from="08:00" until="20:00"/>
    <interval from="08:00" until="16:00" day="6"/>
    <recurrentClosureDays>
      7,1
    </reccurentClosureDays>
  </institution>
````


...Fortsetzung folgt...
