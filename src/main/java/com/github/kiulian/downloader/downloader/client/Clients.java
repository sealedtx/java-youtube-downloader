package com.github.kiulian.downloader.downloader.client;

import com.github.kiulian.downloader.model.videos.formats.Itag;
import com.github.kiulian.downloader.model.videos.quality.AudioQuality;
import com.github.kiulian.downloader.model.videos.quality.VideoQuality;

import java.util.*;

public class Clients {


 private static final List<Client> defaultClients;
 private static Client HIGHEST_PRIORITY_CLIENT;


 static{
  List<Client> clients = new ArrayList<Client>(){
   @Override
  public boolean add(Client client) {
   if(HIGHEST_PRIORITY_CLIENT==null ||client.getPriority() > HIGHEST_PRIORITY_CLIENT.getPriority()){
    HIGHEST_PRIORITY_CLIENT=client;
   }
   return super.add(client);
  }};



  clients.add(new Client(ClientType.IOS,4));
  clients.add(new Client(ClientType.ANDROID_MUSIC,3));
  clients.add(new Client(ClientType.ANDROID_TV,2));
  clients.add(new Client(ClientType.WEB,1));
  clients.add(new Client(ClientType.MWEB,1));
  clients.add(new Client(ClientType.ANDROID,0));
  clients.sort(Comparator.comparingInt(Client::getPriority).reversed());


 defaultClients= Collections.unmodifiableList(clients);



 }

 /**Gets a list of all the default clients.
  * @return an unmodifiable list of all pre-initialized clients ordered by their priority.
  * The priority of a client is generally determined by its reliability, although said reliability has a
  * volatile nature and a given client can, at any time, become unreliable.*/
 public static List<Client> defaultClients(){
 return defaultClients;
 }
 public static ClientType highestPriorityClient(){
  return HIGHEST_PRIORITY_CLIENT.getType();
 }






}
