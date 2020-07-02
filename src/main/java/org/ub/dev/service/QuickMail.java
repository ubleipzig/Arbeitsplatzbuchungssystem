package org.ub.dev.service;

import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * QuickMail - Arbeitsplatzbuchungssystem
 *
 * Erstellt Emails für Buchungsbestätigungen und Stornierungen aus den Templates "mail.template" und "storno.template" aus dem config-Verzeichnis
 *
 */
public class QuickMail {

    private static Logger logger = LogManager.getLogger(QuickMail.class);

    public QuickMail() {}

    /**
     * Sended Mail mit dem Betreff subject und dem Inhalt body an die Adresse recipient
     *
     * @param sender
     * @param sendername
     * @param recipient
     * @param body
     * @param subject
     * @throws Exception
     */
    public static void sendMail(String sender, String sendername, String recipient, String body, String subject) throws Exception
    {
        Properties sysprop = System.getProperties();

        Properties props = new Properties();
        props.put("mail.smtp.host","server1.rz.uni-leipzig.de");

        sysprop.putAll(props);

        Session session = Session.getDefaultInstance(sysprop, null);
        MimeMessage message = new MimeMessage(session);

        message.setHeader("Content-Type","text/html; charset=UTF-8");

        message.setFrom(new InternetAddress(sender,sendername));

        if(recipient.contains(","))
        {
            String recipients[] = recipient.split(",");

            for(String r:recipients)
            {
                logger.info("Sending mail to "+r+" [MULTIRECIPIENTS]");
                message.addRecipient(Message.RecipientType.TO, new InternetAddress(r));
            }
        }
        else
        {
            logger.info("Sending mail to "+recipient);
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
        }

        message.setSubject(subject,"UTF-8");

        BodyPart messageBodyPart = new MimeBodyPart();
        messageBodyPart.setText(body);

        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(messageBodyPart);

        message.setContent(multipart);
        message.setSentDate(new java.util.Date());
        Transport.send(message);

    }

    /**
     * Sende Mail mit Anhang
     *
     * @param sender
     * @param sendername
     * @param recipient
     * @param body
     * @param subject
     * @param attachement
     * @throws Exception
     */
    public static void sendMailAttachment(String sender, String sendername, String recipient, String body, String subject, String attachement) throws Exception
    {
        Properties sysprop = System.getProperties();

        Properties props = new Properties();
        props.put("mail.smtp.host","server1.rz.uni-leipzig.de");

        sysprop.putAll(props);

        Session session = Session.getDefaultInstance(sysprop, null);
        MimeMessage message = new MimeMessage(session);

        message.setHeader("Content-Type","text/html; charset=UTF-8");

        message.setFrom(new InternetAddress(sender,sendername));

        if(recipient.contains(","))
        {
            String recipients[] = recipient.split(",");

            for(String r:recipients)
            {
                logger.info("Sending mail to "+r+" [MULTIRECIPIENTS]");
                message.addRecipient(Message.RecipientType.TO, new InternetAddress(r));
            }
        }
        else
        {
            logger.info("Sending mail to "+recipient);
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
        }

        message.setSubject(subject,"UTF-8");

        BodyPart messageBodyPart = new MimeBodyPart();
        messageBodyPart.setText(body);

        BodyPart attachementPart = new MimeBodyPart();
        attachementPart.setFileName(attachement);
        DataSource source = new FileDataSource(attachement);
        attachementPart.setDataHandler(new DataHandler(source));

        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(messageBodyPart);
        multipart.addBodyPart(attachementPart);

        message.setContent(multipart);
        message.setSentDate(new java.util.Date());
        Transport.send(message);
    }

}
