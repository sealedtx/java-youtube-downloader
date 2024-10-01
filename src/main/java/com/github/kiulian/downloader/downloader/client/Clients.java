package com.github.kiulian.downloader.downloader.client;

import com.github.kiulian.downloader.model.videos.formats.Itag;
import com.github.kiulian.downloader.model.videos.quality.AudioQuality;
import com.github.kiulian.downloader.model.videos.quality.VideoQuality;

import java.lang.reflect.Field;
import java.util.*;

public class Clients {


 private static final SortedSet<Client> defaultClients;
 private static Client HIGHEST_PRIORITY_CLIENT;


 static{
  Comparator<Client> comparator = new Comparator<Client>() {
   @Override
   public int compare(Client o1, Client o2) {
    if(o1.getType()==o2.getType()){
     return 0;
    }
    int res = Integer.compare(o1.getPriority(),o2.getPriority());
    return res==0?1:res;


   }
  }.reversed();
     SortedSet<Client> clientSet = new TreeSet<Client>(comparator){
   @Override
   public boolean add(Client client) {
    if(HIGHEST_PRIORITY_CLIENT==null ||client.getPriority() > HIGHEST_PRIORITY_CLIENT.getPriority()){
     HIGHEST_PRIORITY_CLIENT=client;
    }
    return super.add(client);
   }
  };


  Field[] fields = ClientType.class.getDeclaredFields();
  Class<?> type = ClientType.class;
  ArrayList<ClientType> clients = new ArrayList<>();


  for (Field field :fields) {
   int modifiers = field.getModifiers();
   if(java.lang.reflect.Modifier.isStatic(modifiers) &&
           java.lang.reflect.Modifier.isFinal(modifiers) &&
           field.getType().equals(type)){
    try {
     clients.add((ClientType) field.get(null));

    } catch (IllegalAccessException ignored) {

    }
   }
  }
  for (ClientType client:clients) {
   clientSet.add(new Client(client));
  }

  clientSet.add(new Client(ClientType.IOS,4));
  clientSet.add(new Client(ClientType.ANDROID_MUSIC,3));
  clientSet.add(new Client(ClientType.ANDROID_TV,2));
  clientSet.add(new Client(ClientType.WEB,1));
  clientSet.add(new Client(ClientType.MWEB,1));
  clientSet.add(new Client(ClientType.ANDROID,0));


 defaultClients= Collections.unmodifiableSortedSet(clientSet);



 }

 /**Gets a list of all the default clients.
  * @return an unmodifiable sorted set of all pre-initialized clients ordered by their priority.
  * The priority of a client is generally determined by its reliability, although said reliability has a
  * volatile nature and a given client can, at any time, become unreliable.*/
 public static SortedSet<Client> defaultClients(){
 return defaultClients;
 }
 public static ClientType highestPriorityClient(){
  return HIGHEST_PRIORITY_CLIENT.getType();
 }






}
