package com.github.kiulian.downloader.downloader.client;

import com.github.kiulian.downloader.model.videos.formats.Itag;
import com.github.kiulian.downloader.model.videos.quality.AudioQuality;
import com.github.kiulian.downloader.model.videos.quality.VideoQuality;

import java.util.*;

public class Clients {

 private static final Map<ClientType,ClientTraits> clientTraits;

 private static void putTrait(ClientTraits trait){
  clientTraits.put(trait.getType(),trait);
 }
 static{
  clientTraits = new EnumMap<>(ClientType.class);
  putTrait(new ClientTraits(ClientType.WEB)
          .minAudioQuality(AudioQuality.low)
          .maxAudioQuality(AudioQuality.high)
          .minVideoQuality(VideoQuality.tiny)
          .maxVideoQuality(VideoQuality.ultrahighres)
          .priority(1));

  putTrait(new ClientTraits(ClientType.ANDROID)
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
  traits.sort(Comparator.comparingInt(ClientTraits::getClientPriority));
  return traits;
 }






}
