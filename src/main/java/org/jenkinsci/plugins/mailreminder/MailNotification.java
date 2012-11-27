package org.jenkinsci.plugins.mailreminder;

import hudson.tasks.Mailer;

import javax.mail.*;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;

/**
 * Created with IntelliJ IDEA.
 * User: dfeng
 * Date: 10/10/12
 * Time: 2:49 PM
 * To change this template use File | Settings | File Templates.
 */
public class MailNotification {
    final private Mailer.DescriptorImpl mailerDescriptor = Mailer.descriptor();
    private String subject;
    private String body;
    private String recipientList;
    HashMap<String,String> mailMap;

    public MailNotification(String recipientList, HashMap<String,String> mailMap){
        this.recipientList = recipientList;
        this.mailMap = mailMap;
    }

    String getSubject(){
        return this.subject;
    }
    String getBody(){
        return this.body;
    }

    void setSubject(String subject){
        this.subject = subject;
    }
    void setBody(String body){
        this.body = body;
    }

    /**
     * JavaMail session.
     */
    public Session createSession(String server, String port, String user, String password) {
        Properties props = new Properties(System.getProperties());
        if (server != null && server.length()!=0) {
            props.put("mail.smtp.host", server);
        }
        if (port != null && port.length()!=0) {
            props.put("mail.smtp.port", port);
        }
        if (user != null) {
            props.put("mail.smtp.auth", "true");
        }

        return Session.getInstance(props, getAuthenticator(user, password));
    }

    private Authenticator getAuthenticator(final String user, final String password) {
        if (user == null) {
            return null;
        }
        return new Authenticator() {

            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, password);
            }
        };
    }

    public MimeMessage send() throws MessagingException
    {
        if (recipientList==null || recipientList.length()==0) return null;
        final InternetAddress[] recipients = InternetAddress.parse(recipientList);

        String host = mailMap.get("server");
        String suffix = mailMap.get("suffix");
        String address = mailMap.get("address");
        String user = mailMap.get("user");
        String password = mailMap.get("password");
        String port = mailMap.get("port");

        MimeMessage msg;
        if(host!=null && host.length()!=0){
            msg = new MimeMessage(createSession(host, port, user, password));
        }else{
            msg = new MimeMessage(mailerDescriptor.createSession());
        }

        if(address!=null && address.length()!=0){
            msg.setFrom(new InternetAddress(address));
        }else{
            msg.setFrom(new InternetAddress(mailerDescriptor.getAdminAddress()));
        }

        msg.setSentDate(new Date());
        msg.setSubject(subject);
        msg.setText(body);
        msg.setRecipients(Message.RecipientType.TO, recipients);

        Transport.send(msg);

        return msg;
    }
}
