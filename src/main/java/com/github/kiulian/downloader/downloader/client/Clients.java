package com.github.kiulian.downloader.downloader.client;

import com.github.kiulian.downloader.model.videos.formats.Itag;
import com.github.kiulian.downloader.model.videos.quality.AudioQuality;
import com.github.kiulian.downloader.model.videos.quality.VideoQuality;

import java.lang.reflect.Field;
import java.util.*;

public class Clients {


 private static final SortedSet<Client> defaultClients;
 private static Client HIGHEST_PRIORITY_CLIENT;
 private static final int MANUALLY_DETERMINED_PRIORITY= Integer.MAX_VALUE;




 static{
  Comparator<Client> comparator = ((Comparator<Client>) (o1, o2) -> {
      if (o1.getType() == o2.getType()) {
          return 0;
      }
      int res = Integer.compare(o1.getPriority(), o2.getPriority());
      return res == 0 ? 1 : res;


  }).reversed();
  SortedSet<Client> clientSet = new TreeSet<Client>(comparator){
   @Override
   public boolean add(Client client) {
    if(HIGHEST_PRIORITY_CLIENT==null ||client.getPriority() > HIGHEST_PRIORITY_CLIENT.getPriority()){
     HIGHEST_PRIORITY_CLIENT=client;
    }
    return super.add(client);
   }
  };



  clientSet.add(new Client(ClientType.ANDROID_CREATOR,59));
  clientSet.add(new Client(ClientType.IOS_CREATOR,58));
  clientSet.add(new Client(ClientType.TVLITE,57));
  clientSet.add(new Client(ClientType.XBOXONEGUIDE,56));
  clientSet.add(new Client(ClientType.GOOGLE_ASSISTANT,55));
  clientSet.add(new Client(ClientType.WEB_INTERNAL_ANALYTICS,54));
  clientSet.add(new Client(ClientType.TVHTML5_YONGLE,53));
  clientSet.add(new Client(ClientType.GOOGLE_MEDIA_ACTIONS,52));
  clientSet.add(new Client(ClientType.TVAPPLE,51));
  clientSet.add(new Client(ClientType.MEDIA_CONNECT_FRONTEND,50));
  clientSet.add(new Client(ClientType.ANDROID_VR,MANUALLY_DETERMINED_PRIORITY));
  clientSet.add(new Client(ClientType.WEB_PARENT_TOOLS,48));
  clientSet.add(new Client(ClientType.IOS,47));
  clientSet.add(new Client(ClientType.ANDROID_LITE,46));
  clientSet.add(new Client(ClientType.TVANDROID,45));
  clientSet.add(new Client(ClientType.MWEB_TIER_2,44));
  clientSet.add(new Client(ClientType.ANDROID_TV,43));
  clientSet.add(new Client(ClientType.IOS_PRODUCER,42));
  clientSet.add(new Client(ClientType.GOOGLE_LIST_RECS,41));
  clientSet.add(new Client(ClientType.WEB_PHONE_VERIFICATION,40));
  clientSet.add(new Client(ClientType.IOS_LIVE_CREATION_EXTENSION,39));
  clientSet.add(new Client(ClientType.IOS_EMBEDDED_PLAYER,38));
  clientSet.add(new Client(ClientType.WEB_EXPERIMENTS,37));
  clientSet.add(new Client(ClientType.IOS_MESSAGES_EXTENSION,36));
  clientSet.add(new Client(ClientType.ANDROID_EMBEDDED_PLAYER,35));
  clientSet.add(new Client(ClientType.IOS_MUSIC,34));
  clientSet.add(new Client(ClientType.MUSIC_INTEGRATIONS,33));
  clientSet.add(new Client(ClientType.IOS_UPTIME,32));
  clientSet.add(new Client(ClientType.WEB_UNPLUGGED_PUBLIC,31));
  clientSet.add(new Client(ClientType.TV_UNPLUGGED_ANDROID,30));
  clientSet.add(new Client(ClientType.WEB_UNPLUGGED_ONBOARDING,29));
  clientSet.add(new Client(ClientType.IOS_UNPLUGGED,28));
  clientSet.add(new Client(ClientType.ANDROID_MUSIC,27));
  clientSet.add(new Client(ClientType.WEB_UNPLUGGED_OPS,26));
  clientSet.add(new Client(ClientType.WEB_HEROES,25));
  clientSet.add(new Client(ClientType.ANDROID_UNPLUGGED,24));
  clientSet.add(new Client(ClientType.TVHTML5,23));
  clientSet.add(new Client(ClientType.TV_UNPLUGGED_CAST,22));
  clientSet.add(new Client(ClientType.TVHTML5_VR,21));
  clientSet.add(new Client(ClientType.TVHTML5_SIMPLY_EMBEDDED_PLAYER,20));
  clientSet.add(new Client(ClientType.WEB_MUSIC_ANALYTICS,19));
  clientSet.add(new Client(ClientType.WEB_EMBEDDED_PLAYER,18));
  clientSet.add(new Client(ClientType.TVHTML5_SIMPLY,17));
  clientSet.add(new Client(ClientType.WEB_CREATOR,16));
  clientSet.add(new Client(ClientType.MWEB,15));
  clientSet.add(new Client(ClientType.WEB,14));
  clientSet.add(new Client(ClientType.TVHTML5_UNPLUGGED,13));
  clientSet.add(new Client(ClientType.WEB_UNPLUGGED,12));
  clientSet.add(new Client(ClientType.TVHTML5_CAST,11));
  clientSet.add(new Client(ClientType.IOS_KIDS,10));
  clientSet.add(new Client(ClientType.ANDROID_KIDS,9));
  clientSet.add(new Client(ClientType.TVHTML5_AUDIO,8));
  clientSet.add(new Client(ClientType.TVHTML5_FOR_KIDS,7));
  clientSet.add(new Client(ClientType.WEB_KIDS,6));
  clientSet.add(new Client(ClientType.TVHTML5_KIDS,5));
  clientSet.add(new Client(ClientType.ANDROID_TESTSUITE,4));
  clientSet.add(new Client(ClientType.ANDROID_TV_KIDS,3));
  clientSet.add(new Client(ClientType.ANDROID,2));
  clientSet.add(new Client(ClientType.WEB_MUSIC,1));
  clientSet.add(new Client(ClientType.WEB_REMIX,0));



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
