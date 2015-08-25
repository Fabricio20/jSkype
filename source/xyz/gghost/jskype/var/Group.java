package xyz.gghost.jskype.var;

import lombok.Data;
import xyz.gghost.jskype.api.LocalAccount;
import xyz.gghost.jskype.api.SkypeAPI;
import xyz.gghost.jskype.internal.packet.packets.SendMessagePacket;
import xyz.gghost.jskype.internal.packet.packets.UserManagementPacket;
import xyz.gghost.jskype.internal.threading.Ping;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;

@Data
@Deprecated
/*
This class is deprecated. If you're using an event, ignore this deprecated message. Group will be fully replaced with Conversation soon.
 */
public class Group {
    private ArrayList<GroupUser> connectedClients = new ArrayList<GroupUser>();
    private String topic = "NPE";
    private String chatId = "";
    private String pictureUrl = "";

    public Group(String chatId, String topic, ArrayList<GroupUser> users) {
        this.chatId = chatId;
        this.topic = topic;
        this.connectedClients = users;
    }
    /** checks if the LocalAccount is an admin */
    public boolean isAdmin(LocalAccount acc){
        return isAdmin(acc.getUsername());
    }
    /** checks if a user is an admin */
    public boolean isAdmin(User user){
        return isAdmin(user.getUsername());
    }
    /** checks if a usermame is an admin */
    public boolean isAdmin(String user){
        for(GroupUser users : connectedClients){
            if (users.getAccount().getUsername().equalsIgnoreCase(user) && users.getRole() == Role.ADMIN) {
                return true;
            }
        }
        return false;
    }
    /** Leave the group */
    public void leave(SkypeAPI api){
        kick(api, api.getSkype().getUsername().toLowerCase());
    }
    /** Send a message to the group*/
    public Message sendMessage(SkypeAPI api, String message) {
        return new SendMessagePacket(api, api.getSkype()).sendMessage(this, new Message(message));
    }
    /** Send a message to the group*/
    public Message sendMessage(SkypeAPI api, Message message) {
        return new SendMessagePacket(api, api.getSkype()).sendMessage(this, message);
    }
    /** Remove a user from the group - must be MASTER */
    public boolean kick(SkypeAPI api, String username) {
        return new UserManagementPacket(api, api.getSkype()).kickUser(chatId, username.toLowerCase());
    }
    /** Remove a user from the group - must be MASTER */
    public boolean kick(SkypeAPI api, User user) {
        return kick(api, user.getUsername());
    }

    /** Add a user to the group */
    public boolean add(SkypeAPI api, String username) {
        return new UserManagementPacket(api, api.getSkype()).addUser(chatId, username.toLowerCase());
    }
    /** Add a user to the group */
    public boolean add(SkypeAPI api, User user) {
        return add(api, user.getUsername());
    }

    public GroupUser getUser(String username){
        boolean done = false;
        while(!done) {
            try {
                for(GroupUser usr : connectedClients){
                    if (usr.getAccount().getUsername().equalsIgnoreCase(username))
                        return usr;
                }
                done = true;
            }catch(ConcurrentModificationException e){
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        }
        return null;
    }
}
