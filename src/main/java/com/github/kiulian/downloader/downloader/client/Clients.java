package com.github.kiulian.downloader.downloader.client;

import com.github.kiulian.downloader.model.videos.formats.Itag;
import com.github.kiulian.downloader.model.videos.quality.AudioQuality;
import com.github.kiulian.downloader.model.videos.quality.VideoQuality;

import java.util.*;

public class Clients {

 private static final Map<ClientType,ClientTraits> clientTraits;
 private static ClientTraits HIGHEST_PRIORITY_CLIENT;

 private static void makeClient(ClientTraits.TraitBuilder builder){
  ClientTraits trait = new ClientTraits(builder);

 putTrait(new ClientTraits(builder));
 if(HIGHEST_PRIORITY_CLIENT==null ||trait.getClientPriority() > HIGHEST_PRIORITY_CLIENT.getClientPriority()){
  HIGHEST_PRIORITY_CLIENT=trait;
 }
 }
 private static void putTrait(ClientTraits trait){
  clientTraits.put(trait.getType(),trait);
 }
 static{
  clientTraits = new HashMap<>();

  makeClient(new ClientTraits.TraitBuilder(ClientType.ANDROID_TV)
          .minAudioQuality(AudioQuality.low)
          .maxAudioQuality(AudioQuality.medium)
          .minVideoQuality(VideoQuality.tiny)
          .maxVideoQuality(VideoQuality.hd1080)
          .priority(2));

  makeClient(new ClientTraits.TraitBuilder(ClientType.WEB)
          .minAudioQuality(AudioQuality.low)
          .maxAudioQuality(AudioQuality.high)
          .minVideoQuality(VideoQuality.tiny)
          .maxVideoQuality(VideoQuality.ultrahighres)
          .priority(1));
  makeClient(new ClientTraits.TraitBuilder(ClientType.MWEB)
          .minAudioQuality(AudioQuality.low)
          .maxAudioQuality(AudioQuality.high)
          .minVideoQuality(VideoQuality.tiny)
          .maxVideoQuality(VideoQuality.ultrahighres)
          .priority(1));

  makeClient(new ClientTraits.TraitBuilder(ClientType.ANDROID)
          .minAudioQuality(AudioQuality.low)
          .maxAudioQuality(AudioQuality.high)
          .minVideoQuality(VideoQuality.tiny)
          .maxVideoQuality(VideoQuality.ultrahighres)
          .priority(0));




 }
 public static ClientTraits defaultTraitsFor(ClientType type){
  return clientTraits.get(type);

 }
 public static List<ClientTraits> byPriority(){
  List<ClientTraits> traits = new ArrayList<>(clientTraits.values());
  traits.sort(Comparator.comparingInt(ClientTraits::getClientPriority).reversed());
  return traits;
 }
 public static ClientTraits highestPriorityClient(){
  return HIGHEST_PRIORITY_CLIENT;
 }






}
