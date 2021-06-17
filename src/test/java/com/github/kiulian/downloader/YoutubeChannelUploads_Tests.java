package com.github.kiulian.downloader;

import com.github.kiulian.downloader.downloader.request.RequestChannelUploads;
import com.github.kiulian.downloader.downloader.response.Response;
import com.github.kiulian.downloader.model.playlist.PlaylistInfo;
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
            Response<PlaylistInfo> response = downloader.getChannelUploads(new RequestChannelUploads(CHANNELID));
            assertTrue(response.ok(), "get channel uploads for id " + CHANNELIDPLAYLIST + " should be ok");
            assertEquals(CHANNELIDPLAYLIST, response.data().details().playlistId(), "playlist id should be " + CHANNELIDPLAYLIST);

            Response<PlaylistInfo> response1 = downloader.getChannelUploads(new RequestChannelUploads(CHANNELNAME));
            assertTrue(response1.ok(), "get channel uploads for id " + CHANNELNAME + " should be ok");
            assertEquals(CHANNELNAMEPLAYLIST, response1.data().details().playlistId(), "playlist id should be " + CHANNELNAMEPLAYLIST);

            Response<PlaylistInfo> response2 = downloader.getChannelUploads(new RequestChannelUploads(MUSICCHANNELID));
            assertTrue(response2.ok(), "get channel uploads for id " + MUSICCHANNELID + " should be ok");
            assertEquals(MUSICCHANNELIDPLAYLIST, response2.data().details().playlistId(), "playlist id should be " + MUSICCHANNELIDPLAYLIST);

            Response<PlaylistInfo> response3 = downloader.getChannelUploads(new RequestChannelUploads(MUSICCHANNELNAME));
            assertTrue(response3.ok(), "get channel uploads for id " + MUSICCHANNELNAME + " should be ok");
            assertEquals(MUSICCHANNELNAMEPLAYLIST, response3.data().details().playlistId(), "playlist id should be " + MUSICCHANNELNAMEPLAYLIST);
        }, "should not throw any exceptions");
    }

    @Test
    @DisplayName("Tests if not existing channels throw correct exceptions")
    void getChannelUploadsExceptions() {
        YoutubeDownloader downloader = new YoutubeDownloader();

        assertDoesNotThrow(() -> {
            Response<PlaylistInfo> response = downloader.getChannelUploads(new RequestChannelUploads(NOTEXISTINGCHANNELID));
            assertFalse(response.ok(), "response should be not ok");
            assertEquals(YoutubeException.BadPageException.class, response.error().getClass(), "response error should be BadPageException");
        });

        assertDoesNotThrow(() -> {
            Response<PlaylistInfo> response = downloader.getChannelUploads(new RequestChannelUploads(NOTEXISTINGCHANNELNAME));
            assertFalse(response.ok(), "response should be not ok");
            assertEquals(YoutubeException.DownloadException.class, response.error().getClass(), "response error should be BadPageException");
        });
    }

}