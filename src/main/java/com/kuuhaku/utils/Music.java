/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2020  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.utils;

import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.Page;
import com.github.ygimenez.type.PageType;
import com.kuuhaku.Main;
import com.kuuhaku.handlers.music.GuildMusicManager;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.I18n;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Music {
    private static void play(VoiceChannel vc, TextChannel channel, Guild guild, GuildMusicManager musicManager, AudioTrack track) {
        try {
            guild.getAudioManager().openAudioConnection(vc);

            musicManager.scheduler.channel = channel;
            musicManager.scheduler.queue(track);
        } catch (InsufficientPermissionException e) {
            channel.sendMessage("❌ | Este canal de voz está cheio, para que eu possa me conectar à ele eu preciso da permissão para mover usuários (expulsar um membro do canal também ajuda :slight_smile:).").queue();
        }
    }

    public static void skipTrack(TextChannel channel) {
        GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild(), channel);
        musicManager.scheduler.nextTrack();

        if (musicManager.player.getPlayingTrack() == null) {
            channel.sendMessage("A fila de músicas foi terminada").queue();
            return;
        }
        channel.sendMessage("Música pulada. Tocando agora: " + musicManager.player.getPlayingTrack().getInfo().title).queue();
    }

    public static void pauseTrack(TextChannel channel) {
        GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild(), channel);

        if (musicManager.player.isPaused()) {
            channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_music-paused")).queue();
            return;
        }
        musicManager.scheduler.pauseTrack();

        channel.sendMessage("Música pausada.").queue();
    }

    public static void resumeTrack(TextChannel channel) {
        GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild(), channel);

        if (!musicManager.player.isPaused()) {
            channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_music-not-paused")).queue();
            return;
        }
        musicManager.scheduler.resumeTrack();

        channel.sendMessage("Música despausada.").queue();
    }

    public static void clearQueue(TextChannel channel) {
        GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild(), channel);

        musicManager.scheduler.clear();
        musicManager.player.destroy();
        channel.getGuild().getAudioManager().closeAudioConnection();

        channel.sendMessage("Fila limpa com sucesso.").queue(s -> Helper.spawnAd(channel));
    }

    public static void trackInfo(TextChannel channel) {
        GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild(), channel);

        if (musicManager.player.getPlayingTrack() == null) {
            channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_no-music")).queue();
            return;
        }

        try {
            AudioTrack at = musicManager.player.getPlayingTrack();

            EmbedBuilder eb = new EmbedBuilder();

            String thumb = "https://img.youtube.com/vi/" + at.getInfo().uri.substring(at.getInfo().uri.indexOf("v=")).replace("v=", "") + "/maxresdefault.jpg";
            eb.setTitle(musicManager.player.getPlayingTrack().getInfo().title, at.getInfo().uri);
            eb.setImage(thumb);
            eb.setColor(Helper.colorThief(thumb));
            eb.addField("Postado por:", at.getInfo().author, true);
            eb.addField("Duração:", String.valueOf(Helper.round(((double) at.getDuration() / 1000) / 60, 2)).replace(".", ":"), true);

            channel.sendMessage(eb.build()).queue();
        } catch (IOException e) {
            channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_music-data")).queue();
        }
    }

    public static void queueInfo(TextChannel channel) {
        GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild(), channel);

        if (musicManager.player.getPlayingTrack() == null) {
            channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_empty-queue")).queue();
            return;
        }

        List<Page> pages = new ArrayList<>();
        List<MessageEmbed.Field> f = new ArrayList<>();

        EmbedBuilder eb = new ColorlessEmbedBuilder();

        LinkedList<AudioTrack> queue = new LinkedList<>(musicManager.scheduler.queue());
        queue.addFirst(musicManager.player.getPlayingTrack());

        for (int i = 0; i < queue.size(); i++) {
            if (i > 0)
                f.add(new MessageEmbed.Field((i + 1) + " - " + queue.get(i).getInfo().title, "Requisitado por " + ((User) queue.get(i).getUserData()).getAsMention(), false));
            else
                f.add(new MessageEmbed.Field("Tocando agora - " + queue.get(i).getInfo().title, "Requisitado por " + ((User) queue.get(i).getUserData()).getAsMention(), false));
        }

        for (int i = 0; i < Math.ceil(f.size() / 10f); i++) {
            eb.clear();
            List<MessageEmbed.Field> subF = f.subList(-10 + (10 * (i + 1)), Math.min(10 * (i + 1), f.size()));
            for (MessageEmbed.Field field : subF) {
                eb.addField(field);
            }

            eb.setTitle("Fila de músicas:");
            eb.setFooter("Tempo estimado da fila: " + Helper.toDuration(musicManager.scheduler.queue().stream().mapToLong(AudioTrack::getDuration).sum()), null);

            pages.add(new Page(PageType.EMBED, eb.build()));
        }

        channel.sendMessage((MessageEmbed) pages.get(0).getContent()).queue(s -> Pages.paginate(s, pages, 1, TimeUnit.MINUTES, 5));
    }

    public static void setVolume(TextChannel channel, int volume) {
        GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild(), channel);

        if (volume <= 100 && volume > 0) musicManager.player.setVolume(volume);
        else {
            channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_invalid-volume")).queue();
            return;
        }

        channel.sendMessage("Volume trocado para " + volume + "%.").queue();
    }

    public static void loadAndPlay(final Member m, final TextChannel channel, final String trackUrl) {
        GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild(), channel);

        Main.getInfo().getApm().loadItemOrdered(musicManager, trackUrl, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                if (Objects.requireNonNull(m.getVoiceState()).inVoiceChannel()) {
                    channel.sendMessage("Musíca adicionada com sucesso à fila: " + track.getInfo().title).queue();

                    track.setUserData(m.getUser());
                    play(m.getVoiceState().getChannel(), channel, channel.getGuild(), musicManager, track);
                } else
                    channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_not-in-voice-channel")).queue();
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                if (Objects.requireNonNull(m.getVoiceState()).inVoiceChannel()) {
                    List<AudioTrack> tracks = playlist.getTracks();
                    Collections.shuffle(tracks);
                    tracks = tracks.subList(0, Math.min(tracks.size(), 10));

                    for (AudioTrack p : tracks) {
                        p.setUserData(m.getUser());
                        play(m.getVoiceState().getChannel(), channel, channel.getGuild(), musicManager, p);
                    }

                    channel.sendMessage("Playlist adicionada com sucesso à fila (max. 10 músicas por playlist, escolhidas aleatoriamente): " + playlist.getName()).queue();
                } else
                    channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_not-in-voice-channel")).queue();
            }

            @Override
            public void noMatches() {
                channel.sendMessage("❌ | Nenhuma música encontrada com o link " + trackUrl).queue();
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                channel.sendMessage("❌ | Erro ao tocar a música: " + exception.getMessage()).queue();
            }
        });
    }

    public static synchronized GuildMusicManager getGuildAudioPlayer(Guild guild, TextChannel channel) {
        long guildId = guild.getIdLong();
        GuildMusicManager musicManager = Main.getInfo().getGmms().get(guildId);

        if (musicManager == null) {
            musicManager = new GuildMusicManager(Main.getInfo().getApm(), channel);
            Main.getInfo().addGmm(guildId, musicManager);
        }

        guild.getAudioManager().setSendingHandler(musicManager.getSendHandler());

        return musicManager;
    }
}
