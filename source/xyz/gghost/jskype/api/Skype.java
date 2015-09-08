package xyz.gghost.jskype.api;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import salt.samczsun.SkypeAuthentication;
import salt.samczsun.exception.ConnectionException;
import salt.samczsun.exception.InvalidCredentialsException;
import xyz.gghost.jskype.exception.AccountUnusableForRecentException;
import xyz.gghost.jskype.exception.BadResponseException;
import xyz.gghost.jskype.exception.NoPendingContactsException;
import xyz.gghost.jskype.internal.impl.MessageHistory;
import xyz.gghost.jskype.internal.packet.PacketBuilder;
import xyz.gghost.jskype.internal.packet.PacketBuilderUploader;
import xyz.gghost.jskype.internal.packet.RequestType;
import xyz.gghost.jskype.internal.packet.packets.GetContactsPacket;
import xyz.gghost.jskype.internal.packet.packets.GetConvos;
import xyz.gghost.jskype.internal.packet.packets.GetPendingContactsPacket;
import xyz.gghost.jskype.internal.packet.packets.GetProfilePacket;
import xyz.gghost.jskype.var.Conversation;
import xyz.gghost.jskype.internal.impl.Group;
import xyz.gghost.jskype.var.LocalAccount;
import xyz.gghost.jskype.var.User;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;

public class Skype {
    private SkypeAPI api;
    @Getter
    @Setter
    private String email;
    @Getter
    @Setter
    private String username;
    @Getter
    private String password;
    @Getter
    @Setter
    private String xSkypeToken;
    @Getter
    @Setter
    private String regToken;
    @Setter
    private ArrayList<User> contactCache = new ArrayList<User>();
    @Setter
    private ArrayList<Conversation> recentCache = new ArrayList<Conversation>();
    @Getter
    private HashMap<String, MessageHistory> history = new HashMap<String, MessageHistory>();

    public Skype(String username, String password, SkypeAPI api) {
        this.username = username;
        this.email = username;
        this.password = password;
        this.api = api;
        init();
    }

    public Skype(String email, String username, String password, SkypeAPI api) {
        this.email = email;
        this.username = username;
        this.password = password;
        this.api = api;
        init();
    }

    private void init() {

        if (api.displayInfoMessages())
            System.out.println("API> Logging in");
        relog();
        System.out.println("API> Getting user data");
           if (api.displayInfoMessages())
        System.out.println("API> Getting contacts");
        try {
            new GetContactsPacket(api, this).setupContact();
        } catch (Exception e) {
            if (api.displayInfoMessages())
                 System.out.println("API> Failed to get your entire contacts due to a bad account. Try an alt?");
        }
        System.out.println("API> Getting groups, non-contact conversations, group information");
        try {
            recentCache = new GetConvos(api, this).getRecentChats();
        } catch (AccountUnusableForRecentException e) {
            System.out.println("API> Failed to get recent contacts due to a bad account. Try an alt?");
        }
        if (api.displayInfoMessages())
             System.out.println("API> Initialized!");
    }

    /**
     * Login
     */
    public void relog() {
        try {
            new SkypeAuthentication().login(api, this);
        } catch (InvalidCredentialsException e) {
            if (api.displayInfoMessages())
                System.out.println("API> Bad username + password");
            System.exit(-1);
        } catch(ConnectionException e) {
            System.out.println("API> Failed to connect to the internet... Retying in 5 secs");
            try {
                Thread.sleep(5000);
            }catch(InterruptedException ee){}
            relog();
        } catch (Exception e) {
            e.printStackTrace();
            if (api.displayInfoMessages())
                System.out.println("API> Failed to login!");
            System.exit(-1);
        }
    }


    /**
     * Get group by short id (no 19: + @skype blah blah blah)
     */
    public Group getGroupById(String id) {
        for (Conversation group : recentCache) {
            if ((!group.isUserChat()) && group.getId().equals(id))
                return group.getForcedGroupGroup();
        }
        return null;
    }

    /**
     * This method will get as much data as possible about a user without contacting to skype
     */
    public User getSimpleUser(String username) {
        User user = getContact(username);
        return user != null ? user : new User(username);
    }

    /**
     * get user by username
     */
    public User getUserByUsername(String username) {
        User user = getContact(username);
        return user != null ? user : new GetProfilePacket(api, this).getUser(username);
    }

    /**
     * Get contact by username
     */
    public User getContact(String username) {
        for (User contact : getContacts()) {
            if (contact.getUsername().equalsIgnoreCase(username))
                return contact;
        }
        return null;
    }

    /**
     * Now same as #getRecent
     */
    @Deprecated
    public ArrayList<Conversation> getAllChats() {
        return recentCache;
    }

    /**
     * Gets contacts
     */
    public ArrayList<User> getContacts() {
        return contactCache;
    }

    /**
     * Get recent chats - this includes contacts, none-contacts and groups
     */
    public ArrayList<Conversation> getRecent() {
        return recentCache;
    }

    /**
     * Get the known groups ("recent conversations" and active chats).
     */
    public ArrayList<Conversation> getConversations() {
        return recentCache;
    }

    /**
     * Gets pending contact requests
     */
    public ArrayList<User> getContactRequests() throws BadResponseException, NoPendingContactsException {
        return new GetPendingContactsPacket(api, this).getPending();
    }

    /**
     * Attempts to accept a contact request - can take upto 2 minutes to appear as a contact
     */
    public void acceptContact(String username) {
        new GetPendingContactsPacket(api, this).acceptRequest(username);
    }

    /**
     * Attempts to send a contact request
     */
    public void sendContactRequest(String username) {
        new GetPendingContactsPacket(api, this).sendRequest(username);
    }

    /**
     * Attempts to send a contact request with a custom greeting
     */
    public void sendContactRequest(String username, String greeting) {
        new GetPendingContactsPacket(api, this).sendRequest(username, greeting);
    }
    /**
     * Skype db lookup / search
     */
    public ArrayList<User> searchSkypeDB(String keywords){
        PacketBuilder packet = new PacketBuilder(api);
        packet.setType(RequestType.GET);
        packet.setUrl("https://api.skype.com/search/users/any?keyWord=" + URLEncoder.encode(keywords)+ "&contactTypes[]=skype");
        String data = packet.makeRequest(this);
        if (data == null)
            return null;

        JSONArray jsonArray = new JSONArray(data);
        ArrayList<String> usernames = new ArrayList<String>();
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject contact = jsonArray.getJSONObject(i);
            usernames.add(contact.getJSONObject("ContactCards").getJSONObject("Skype").getString("SkypeName"));
        }
        return new GetProfilePacket(api, this).getUsers(usernames);
    }
    /**
     * Get user info about the account
     */
    public LocalAccount getAccountInfo(){
        return new GetProfilePacket(api, this).getMe();
    }
    /**
     * Change profile picture
     */
    public void changePictureFromFile(String url){
        try {;
            //No point of mkaing a new class just for this one small method
            PacketBuilderUploader uploader = new PacketBuilderUploader(api);
            uploader.setSendLoginHeaders(true);
            uploader.setUrl("https://api.skype.com/users/itsghostbot/profile/avatar");
            uploader.setType(RequestType.PUT);
            uploader.makeRequest(this, new FileInputStream(url));
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    public void changePictureFromUrl(String url){
        try {
            //No point of mkaing a new class just for this one small method
            PacketBuilderUploader uploader = new PacketBuilderUploader(api);
            uploader.setSendLoginHeaders(true);
            uploader.setUrl("https://api.skype.com/users/itsghostbot/profile/avatar");
            uploader.setType(RequestType.PUT);
            URL image = new URL(url);
            InputStream data = image.openStream();
            uploader.makeRequest(this, data);
        }catch (Exception e){
            e.printStackTrace();}
    }

}
