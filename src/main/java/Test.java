import com.github.kiulian.downloader.YoutubeDownloader;
import com.github.kiulian.downloader.downloader.request.RequestVideoInfo;
import com.github.kiulian.downloader.downloader.response.Response;

public class Test {
    public static void main(String[] args) {
        YoutubeDownloader downloader = new YoutubeDownloader();
        RequestVideoInfo request = new RequestVideoInfo("Pj8XROqFtto");

        Response response = downloader.getVideoInfo(request);

    }
}
