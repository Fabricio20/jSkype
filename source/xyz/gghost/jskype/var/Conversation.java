package xyz.gghost.jskype.var;

import lombok.Getter;
import lombok.Setter;
import xyz.gghost.jskype.api.SkypeAPI;
import xyz.gghost.jskype.internal.packet.packets.PingPrepPacket;
import xyz.gghost.jskype.internal.packet.packets.SendMessagePacket;

import java.util.ArrayList;

/**
 * Created by Ghost on 22/08/2015.
 */
public class Conversation extends  Group{
    @Getter @Setter private boolean userChat;
    @Getter @Setter private String id;
    private SkypeAPI api;
    /** Do not use - this is to be used. Usually this will get the group by id, however if it is forced, it'll return and use the forced group*/
    @Setter @Getter private Group forcedGroupGroup;
    @Getter @Setter private boolean forcedGroup = false;
    /** End of do not use */
    @Getter @Setter private ArrayList<GroupUser> connectedClients = new ArrayList<GroupUser>();
    @Getter @Setter private String topic = "";

    /** This class allows backwards and forwards compatibility of the User and Group class.*/

    public Conversation(SkypeAPI api, String id, boolean isGroup) {
        super(id, "", null);
        try {
            if (isGroup) {
                this.setConnectedClients(api.getSkype().getGroupById(id).getConnectedClients());
            }
        }catch(NullPointerException e){
            //jSkype is still loading
        }
        this.userChat = !isGroup;
        this.id = id;
        this.api = api;
    }

    public String getTopic(){
        if (!userChat){
            return getGroup().getTopic();
        }
        return "";
    }
    /** Send an image (url - not a gif ) to a chat*/
    public Message sendImageToChat(SkypeAPI api, String URL){
        if (!userChat) {
            return new SendMessagePacket(api, api.getSkype()).sendPing(getGroup(), new Message("hi"), new PingPrepPacket(api).urlToId(URL, id));
        } else {
            return new SendMessagePacket(api, api.getSkype()).sendPing(id, new Message("hi"), new PingPrepPacket(api).urlToId(URL, id));
        }
    }
    /** send message to the chat*/
    public Message sendMessage(SkypeAPI api, String text) {
        return sendMessage(api, new Message(text));
    }
    /** send message to the chat*/
    public Message sendMessage(SkypeAPI api, Message text) {
        if (!userChat) {
            return new SendMessagePacket(api, api.getSkype()).sendMessage(new Group(id, "", null), text);
        } else {
            return new SendMessagePacket(api, api.getSkype()).sendMessage(id, text);
        }
    }
    /** Do not use*/
    public void setForcedGroupGroup(Group forcedGroupGroup){
        this.forcedGroupGroup = forcedGroupGroup;
    }
    /** If chat is a group, get group or return null*/
    public Group getGroup() {
        if(forcedGroup){
            return forcedGroupGroup;
        }

        return api.getSkype().getGroupById(id);
    }
    /** If chat is a user, get user or return null*/
    public User getUser() {
        for (User user : api.getSkype().getContacts()) {
            if (user.getUsername().equalsIgnoreCase(id))
                return user;
        }
        return null;
    }
    /** get user by username */
    public GroupUser getUser(String username){
        if(!userChat){
            return getGroup().getUser(username);
        }
        GroupUser user = new GroupUser(api.getSkype().getUserByUsername(username));
        user.setRole(Role.ADMIN);
        return user;
    }
    /**Gets connected clients in the group or user with user chat*/
    public ArrayList<GroupUser> getConnectedClients(){
        if(!userChat){
            return getGroup().getConnectedClients();
        }else{
            ArrayList<GroupUser> users = new ArrayList<GroupUser>();
            users.add(new GroupUser(api.getSkype().getContact(id)));
            users.add(new GroupUser(api.getSkype()));
            return users;
        }
    }
    /** Kick a user from a group - groups only*/
    public void kick(String username){
        if (!userChat)
            getGroup().kick(api, username);
    }
     /**Add a user from a group - groups only*/
    public void add(String username){
        if (!userChat)
            getGroup().add(api, username);
    }
     /** Kick a user from a group - groups only */
    public void kick(User username){
        if (!userChat)
            getGroup().kick(api, username);
    }
    /** Add a user from a group - groups only*/
    public void add(User username){
        if (!userChat)
            getGroup().add(api, username);
    }
    /** Checks if a string is admin*/
    public boolean isAdmin(String user){
        if (!userChat)
            return getGroup().isAdmin(user);
        return true;

    }
    /** Checks if LocalUser or a User is admin*/
    public boolean isAdmin(User user){
        if (!userChat)
            return getGroup().isAdmin(user);
        return true;

    }


}
