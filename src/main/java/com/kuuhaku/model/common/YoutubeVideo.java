/*
 * This file is part of Shiro J Bot.
 *
 * Shiro J Bot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Shiro J Bot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Shiro J Bot.  If not, see <https://www.gnu.org/licenses/>
 */

package com.kuuhaku.model.common;

public class YoutubeVideo {
    private final String id, title, desc, thumb, channel;
    private final boolean playlist;

    public YoutubeVideo(String id, String title, String desc, String thumb, String channel, boolean playlist) {
        this.id = id;
        this.title = title;
        this.desc = desc;
        this.thumb = thumb;
        this.channel = channel;
        this.playlist = playlist;
    }

    public String getUrl() {
        return playlist ? "https://www.youtube.com/playlist?list=" + id : "https://www.youtube.com/watch?v=" + id;
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
