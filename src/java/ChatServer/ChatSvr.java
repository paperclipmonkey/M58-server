/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ChatServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.jws.WebService;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.co.threeequals.webchat.*;

/**
 *
 * @author michaelwaterworth
 */
@WebService(serviceName = "ChatSvr")
public class ChatSvr {
    private Group gr = new Group();
    private final HashMap clients = new HashMap();
    
    /**
     * Read a files first line given URL string
     * @param path path to file as string
     * @param encoding character encoding to use
     * @return String containing value of first line
     * @throws IOException 
     */
    String readFile(String path, Charset encoding) throws IOException {
        BufferedReader br;
        br = new BufferedReader(
                new InputStreamReader(
                        Thread.currentThread().getContextClassLoader().getResource("password.txt").openStream()
                )
        );
        return br.readLine();
    }
    
    /**
     * Set password used by application group.
     * Read from file
     */
    private void setPassword(){
        try {
            String adminPassword = readFile("password.txt", StandardCharsets.UTF_8);
            gr.setPassword(adminPassword);//Add admin password to Group
            System.out.println("Password set as: " + adminPassword);
        } catch (IOException ex) {
            Logger.getLogger(Group.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Web service operation
     * @param messageStr String message sent by client
     * @param uuid Unique ID of client
     * @return boolean success value for posting of message
     */
    @WebMethod(operationName = "sendMessage")
    public Boolean sendMessage(@WebParam(name = "messageStr") String messageStr, @WebParam(name = "uuid") String uuid) {
        //Find ch from Message
                System.out.println("Posting: " + messageStr);
                System.out.println("To " + clients.size() + " clients");

        if(uuid != null && !uuid.equals("")){
            ClientHandler ch = (ClientHandler) clients.get(uuid);
            if(ch!= null){
                ch.processSendMessage(messageStr);
                return true;
            }
        }
        return false;
    }

    /**
     * Join the group as a client
     * @return UUID to use as a client with every subsequent request
     */
    @WebMethod(operationName = "join")
    public String join() {
        setPassword();
        //Create a unique identifier for the application
        UUID uuid = UUID.randomUUID();
        String uuidStr = uuid.toString();
        //Create a new object for the user in the chat client
        ClientHandler ch = new ClientHandler(gr);
        //Add clientHandler to clients HashMap
        clients.put(uuidStr, ch);
        System.out.println(uuidStr);
        //To be used in subsequent requests
        return uuidStr; 
    }

    /**
     * Get message from user's message queue
     * @param uuid Unique ID of user - Random string as shared-secret
     */
    @WebMethod(operationName = "getMessage")
    public Message getMessage(@WebParam(name = "uuid") String uuid) throws InterruptedException {
        updateClientTimeout(uuid);
        checkClientTimeout();
        if(uuid != null && !uuid.equals("")){
            ClientHandler ch = (ClientHandler) clients.get(uuid);
            if(ch!= null){
                //Message msg = ch.getMessage();
                //int i = 0;
                //while(msg == null || i < 100){//Until message or 10 seconds.
                    //sleep(100);
                //    msg = ch.getMessage();
                //    i++;
                //}
                return ch.getMessage();
            }
        }
        return null;
    }

    /**
     * Logout from service
     * @param uuid unique id of user to logout
     */
    @WebMethod(operationName = "logout")
    public void logout(@WebParam(name = "uuid") String uuid) {
        if(uuid != null && !uuid.equals("")){
            ClientHandler ch = (ClientHandler) clients.get(uuid);
            if(ch != null){
                gr.leaveGroup(ch);//Remove from Group - Message sent
                clients.remove(uuid);//Idempotent - Remove from clients list.
            }
        }
    }
    
    /**
     * Update a users timeout Date - if too long in the past will be removed
     * @param uuid unique id of user
     */
    private void updateClientTimeout(String uuid){
        ClientHandler ch = (ClientHandler) clients.get(uuid);
            if(ch != null){
                ch.setDate(new Date());
            }
    }
    
    /**
     * Remove old stale connections from pool.
     */
    private void checkClientTimeout(){
        Iterator it = clients.entrySet().iterator();
        while (it.hasNext()) {
                Map.Entry pair = (Map.Entry)it.next();
                ClientHandler ch = (ClientHandler) pair.getValue();
                if(ch.getDate().getTime() < new Date().getTime() - 10000){//10 seconds timeout
                    gr.leaveGroup(ch);//Remove from Group - Message sent
                    it.remove();// avoids a ConcurrentModificationException
                }
        }
    }
}