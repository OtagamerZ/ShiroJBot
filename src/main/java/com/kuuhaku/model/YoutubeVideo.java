package com.kuuhaku.model;

public class YoutubeVideo {
	private final String id, title, desc, thumb, channel;

	public YoutubeVideo(String id, String title, String desc, String thumb, String channel) {
		this.id = id;
		this.title = title;
		this.desc = desc;
		this.thumb = thumb;
		this.channel = channel;
	}

	public String getUrl() {
		return "https://www.youtube.com/watch?v=" + id;
	}

	public String getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public String getDesc() {
		return desc;
	}

	public String getThumb() {
		return thumb;
	}

	public String getChannel() {
		return channel;
	}
}
