package com.github.kiulian.downloader;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests the \"getChannelUploads\" method")
public class YoutubeChannelUploads_Tests {

    protected static final String CHANNELID = "UCSJ4gkVC6NrvII8umztf0Ow"; //ChilledCow
    protected static final String CHANNELNAME = "LinusTechTips"; //LinusTechTips
    protected static final String MUSICCHANNELID = "UCY2qt3dw2TQJxvBrDiYGHdQ"; //Pink Floyd
    protected static final String MUSICCHANNELNAME = "queen"; //Queen

    protected static final String NOTEXISTINGCHANNELID = "UCY2qtDdwVTQJxDBrYiYGHdQ";
    protected static final String NOTEXISTINGCHANNELNAME = "thischanneldoesnotexist11111111111111111111111111";

    protected static final String CHANNELIDPLAYLIST = "UUSJ4gkVC6NrvII8umztf0Ow"; //ChilledCow's uploads
    protected static final String CHANNELNAMEPLAYLIST = "UUXuqSBlHAE6Xw-yeJA0Tunw"; //LinusTechTips's uploads
    protected static final String MUSICCHANNELIDPLAYLIST = "UUY2qt3dw2TQJxvBrDiYGHdQ"; //Pink Floyd's uploads
    protected static final String MUSICCHANNELNAMEPLAYLIST = "UUiMhD4jzUqG-IgPzUmmytRQ"; //Queen's uploads

    @Test
    @DisplayName("Tests if method gets correct upload playlists")
    void getChannelUploads() {
        YoutubeDownloader downloader = new YoutubeDownloader();
        assertDoesNotThrow(() -> {
            assertEquals(CHANNELIDPLAYLIST, downloader.getChannelUploads(CHANNELID).details().playlistId(), "playlist id should be " + CHANNELIDPLAYLIST);
            assertEquals(CHANNELNAMEPLAYLIST, downloader.getChannelUploads(CHANNELNAME).details().playlistId(), "playlist id should be " + CHANNELNAMEPLAYLIST);
            assertEquals(MUSICCHANNELIDPLAYLIST, downloader.getChannelUploads(MUSICCHANNELID).details().playlistId(), "playlist id should be " + MUSICCHANNELIDPLAYLIST);
            assertEquals(MUSICCHANNELNAMEPLAYLIST, downloader.getChannelUploads(MUSICCHANNELNAME).details().playlistId(), "playlist id should be " + MUSICCHANNELNAMEPLAYLIST);
        }, "should not throw any exceptions");
    }

    @Test
    @DisplayName("Tests if not existing channels throw correct exceptions")
    void getChannelUploadsExceptions() {
        YoutubeDownloader downloader = new YoutubeDownloader();
        assertThrows(YoutubeException.BadPageException.class, () ->
                downloader.getChannelUploads(NOTEXISTINGCHANNELID), "should throw BadPageException");
        assertThrows(java.io.FileNotFoundException.class, () ->
                downloader.getChannelUploads(NOTEXISTINGCHANNELNAME), "should throw FileNotFoundException");

    }

}
