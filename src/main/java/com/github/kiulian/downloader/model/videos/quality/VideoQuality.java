package com.github.kiulian.downloader.model.videos.quality;


public enum VideoQuality {
    unknown(0),
    noVideo(0),
    tiny(1),
    small(2), // 240p
    medium(3), // 360p
    large(4), // 480p
    hd720(5),
    hd1080(6),
    hd1440(7),
    hd2160(8),
    hd2880p(9),
    highres(10); // 3072p

    private final Integer order;

    VideoQuality(int order) {
        this.order = order;
    }

    public int compare(VideoQuality quality) {
        if (this == quality) return 0;
        return order.compareTo(quality.order);
    }

}
