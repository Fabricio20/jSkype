package xyz.gghost.jskype.internal.packet.packets;

import org.json.JSONArray;
import org.json.JSONObject;
import xyz.gghost.jskype.api.LocalAccount;
import xyz.gghost.jskype.api.SkypeAPI;
import xyz.gghost.jskype.chat.Chat;
import xyz.gghost.jskype.internal.packet.PacketBuilder;
import xyz.gghost.jskype.internal.packet.RequestType;
import xyz.gghost.jskype.var.User;

import java.util.ArrayList;

public class GetProfilePacket {
    private SkypeAPI api;
    private LocalAccount usr;

    public GetProfilePacket(SkypeAPI api, LocalAccount usr) {
        this.api = api;
        this.usr = usr;
    }

    public User getUser(String username) {
        if(username.equalsIgnoreCase("echo123")){
            return minorUserData(username);
        }
        PacketBuilder packet = new PacketBuilder(api);

        packet.setType(RequestType.POST);
        packet.setUrl("https://api.skype.com/users/self/contacts/profiles");
        packet.setData("contacts[]=" + username);
        packet.setIsForm(true);

        String data =  packet.makeRequest(usr);

        if (data == null) {
            //Display debug info and return minimalistic data about the user
            //TODO: retry
            System.out.println("\nFailed to get profile of " + username  + " due to an internal server error");
            System.out.println("Someone may have blocked you or have high privacy settings.");
            System.out.println("You can ignore this...");

            return minorUserData(username);
        }
        try {
            User user = new User(username);

            data = data.replaceFirst("\\[", "").replace("]", "");
            JSONObject jsonObject = new JSONObject(data); //ln 50

            user.setUsername(username);
            user.setPictureUrl(jsonObject.isNull("avatarUrl") ? "https://swx.cdn.skype.com/assets/v/0.0.213/images/avatars/default-avatar-group_46.png" : jsonObject.getString("avatarUrl"));
            user.setDisplayName(jsonObject.isNull("displayname") ? (jsonObject.isNull("firstname") ? username : getDisplayName(data)) : jsonObject.getString("displayname"));
            user.setMood(jsonObject.isNull("richMood") ? (jsonObject.isNull("mood") ? "" : jsonObject.getString("mood")) : jsonObject.getString("richMood"));
            user.setMood(Chat.decodeText(user.getMood()));

            return user;
        }catch(Exception e){
            System.out.println("Failed to get profile of " + username);
            System.out.println("Data : " + data);
            e.printStackTrace();
            return minorUserData(username);
        }
    }
    public ArrayList<User> getUsers(ArrayList<String> usernames) {
        ArrayList<User> contacts = new ArrayList<User>();
        PacketBuilder packet = new PacketBuilder(api);

        packet.setType(RequestType.POST);
        packet.setUrl("https://api.skype.com/users/self/contacts/profiles");
        packet.setData("contacts[]=");

        boolean first = true;

        for (String username : usernames) {
            if (!username.equals("echo123")) {
                packet.setData( packet.getData() + (first ? "" : "&contacts[]=") + username);
            }
            first = false;
        }

        packet.setIsForm(true);

        String data =  packet.makeRequest(usr);
        if (data == null)
            return null;

        try {
            JSONArray jsonObject = new JSONArray(data);
            int count = 0; //offset for displayname grabber
            for (int ii = 0; ii < jsonObject.length(); ii++) {
                JSONObject jData = jsonObject.getJSONObject(ii);
                count ++; // ++ 1

                User user = new User(jData.getString("username"));


                user.setPictureUrl(jData.isNull("avatarUrl") ? "https://swx.cdn.skype.com/assets/v/0.0.213/images/avatars/default-avatar-group_46.png" : jData.getString("avatarUrl"));
                user.setDisplayName(jData.isNull("displayname") ? (jData.isNull("firstname") ? jData.getString("username") : getDisplayName(data, (count))) : jData.getString("displayname"));
                user.setMood(jData.isNull("richMood") ? (jData.isNull("mood") ? "" : jData.getString("mood")) : jData.getString("richMood"));
                user.setMood(Chat.decodeText(user.getMood()));

                contacts.add(user);
            }

            return contacts;
        }catch(Exception e){
            System.out.println("Failed to get profile of arraylist");
            System.out.println("Data : " + data);
            e.printStackTrace();
            return null;
        }
    }
    private User minorUserData(String username){
        return new User(username);
    }
    public String getDisplayName(String data, int count){
        return (data.split("firstname\":")[count].split("\",\"")[0]).replace("\"", "");
    }
    public String getDisplayName(String data){
        return data.split("firstname\":\"")[1].split("\",\"")[0];
    }
}
