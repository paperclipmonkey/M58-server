/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ChatServer;

import static java.lang.Thread.sleep;
import java.util.Date;
import javax.jws.WebService;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
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
     * Web service operation
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
     * Web service operation
     */
    @WebMethod(operationName = "join")
    public String join() {
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
     * Web service operation
     * @param uuid
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
     * Web service operation
     * @param uuid
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
    
    private void updateClientTimeout(String uuid){
        ClientHandler ch = (ClientHandler) clients.get(uuid);
            if(ch != null){
                ch.setDate(new Date());
            }
    }
    
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