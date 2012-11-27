package org.jenkinsci.plugins.mailreminder;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.mail.Authenticator;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;

/**
 * Created with IntelliJ IDEA.
 * User: dfeng
 * Date: 10/10/12
 * Time: 2:48 PM
 * To change this template use File | Settings | File Templates.
 */
public class EmailRemindPublisher extends Notifier {
    private String recipientList;
    private boolean sendIfSuccess;
    private boolean sendIfFail;
    private boolean sendIfChanged;

    @DataBoundConstructor
    public EmailRemindPublisher(String recipientList, boolean sendIfSuccess,
                                boolean sendIfFail, boolean sendIfChanged){
        this.recipientList = recipientList;
        this.sendIfSuccess = sendIfSuccess;
        this.sendIfFail = sendIfFail;
        this.sendIfChanged = sendIfChanged;
    }

    public String getRecipientList(){
        return this.recipientList;
    }
    public boolean getSendIfSuccess(){
        return this.sendIfSuccess;
    }
    public boolean getSendIfFail(){
        return this.sendIfFail;
    }
    public boolean getSendIfChanged(){
        return this.sendIfChanged;
    }

    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {

        HashMap<String,String> mailMap = new HashMap<String,String>();
        mailMap.put("server",getDescriptor().getServer());
        mailMap.put("suffix",getDescriptor().getSuffix());
        mailMap.put("address",getDescriptor().getAddress());
        mailMap.put("username",getDescriptor().getUser());
        mailMap.put("password",getDescriptor().getPassword());
        mailMap.put("port",getDescriptor().getPort());

        try{
            final String expandedRecipientList = build.getEnvironment(listener).expand(recipientList);
            String[] recipientArray = expandedRecipientList.split(",");
            for(int i=0; i<recipientArray.length; i++){
                recipientArray[i] = recipientArray[i].trim();
                if(!recipientArray[i].contains("@")){
                    recipientArray[i] += getDescriptor().getSuffix();
                }
            }
            String recipientStr = "";
            for(int i=0; i<recipientArray.length; i++){
                recipientStr += recipientArray[i];
                if(i<recipientArray.length-1){
                    recipientStr += ",";
                }
            }

            MailNotification mail = new MailNotification(recipientStr, mailMap);
            String subject = "Build " + build.getResult().toString() + " in Jenkins:" +
                    build.getProject().getName() + " #" + build.getNumber();

            String body = "See <" + build.getEnvironment(listener).get("BUILD_URL") + ">";
            mail.setSubject(subject);
            mail.setBody(body);
            boolean sent = false;
            if(sendIfSuccess && !sent){
                if(build.getResult().isBetterOrEqualTo(Result.SUCCESS)){
                    mail.send();
                    listener.getLogger().println("Send E-mail to " + recipientStr);
                    sent = true;
                }
            }
            if(sendIfFail && !sent){
                if(build.getResult().isWorseOrEqualTo(Result.FAILURE)){
                    mail.send();
                    listener.getLogger().println("Send E-mail to " + recipientStr);
                    sent = true;
                }
            }
            if(sendIfChanged && !sent){
                if(build.getResult().isBetterThan(build.getPreviousBuild().getResult()) ||
                        build.getResult().isWorseThan(build.getPreviousBuild().getResult())){
                    mail.send();
                    listener.getLogger().println("Send E-mail to " + recipientStr);
                    sent = true;
                }
            }
        }catch (InterruptedException e){
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }catch (MessagingException e){
            e.printStackTrace();
        }

        return true;
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }


    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        private String server;
        private String suffix;
        private String address;
        private String user;
        private String password;
        private String port;

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            server = formData.getString("server").trim();
            suffix = formData.getString("suffix").trim();
            address = formData.getString("address").trim();
            user = formData.getString("user")==null? null:formData.getString("user").trim();
            password = formData.getString("password")==null? null:formData.getString("password").trim();
            port = formData.getString("port")==null? null:formData.getString("port").trim();
            save();
            return super.configure(req,formData);
        }

        public String getAddress(){
            return address;
        }
        public String getServer(){
            return server;
        }
        public String getSuffix(){
            return suffix;
        }
        public String getUser(){
            return user;
        }
        public String getPassword(){
            return password;
        }
        public String getPort(){
            return port;
        }

        @Override
        public String getDisplayName() {
            return "Email Reminder";
        }

        /*public FormValidation doCheckRecipientList(@QueryParameter String recipientList)
                throws IOException, ServletException {
            String[] recipients = recipientList.split(",");
            for(String recipient:recipients){
                int index = recipient.indexOf("@");
                if(index == -1){
                    return FormValidation.error("Incorrect E-mail address!");
                }
            }
            return FormValidation.ok();
        }*/

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }
    }


}
