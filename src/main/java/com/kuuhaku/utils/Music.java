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

package com.kuuhaku.utils;

import com.kuuhaku.Main;
import com.kuuhaku.handlers.music.GuildMusicManager;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;

import java.io.IOException;
import java.util.Objects;

public class Music {
	private static void play(VoiceChannel vc, Guild guild, GuildMusicManager musicManager, AudioTrack track) {
		if (!guild.getAudioManager().isConnected()) guild.getAudioManager().openAudioConnection(vc);

		musicManager.scheduler.queue(track);
	}

	public static void skipTrack(TextChannel channel) {
		GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());
		musicManager.scheduler.nextTrack();

		if (musicManager.player.getPlayingTrack() == null) {
			channel.sendMessage("A fila de músicas foi terminada").queue();
			return;
		}
		channel.sendMessage("Música pulada. Tocando agora: " + musicManager.player.getPlayingTrack().getInfo().title).queue();
	}

	public static void pauseTrack(TextChannel channel) {
		GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());

		if (musicManager.player.isPaused()) {
			channel.sendMessage(":x: | A música já está pausada.").queue();
			return;
		}
		musicManager.scheduler.pauseTrack();

		channel.sendMessage("Música pausada.").queue();
	}

	public static void resumeTrack(TextChannel channel) {
		GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());

		if (!musicManager.player.isPaused()) {
			channel.sendMessage(":x: | A música não está pausada.").queue();
			return;
		}
		musicManager.scheduler.resumeTrack();

		channel.sendMessage("Música despausada.").queue();
	}

	public static void clearQueue(TextChannel channel) {
		GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());

		musicManager.scheduler.clear();
		musicManager.player.destroy();
		channel.getGuild().getAudioManager().closeAudioConnection();

		channel.sendMessage("Fila limpa com sucesso.").queue(s -> Helper.spawnAd(channel));
	}

	public static void trackInfo(TextChannel channel) {
		GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());

		if (musicManager.player.getPlayingTrack() == null) {
			channel.sendMessage(":x: | Não há nenhuma musica tocando no momento.").queue();
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
			channel.sendMessage(":x: | Erro ao recuperar dados da música.").queue();
		}
	}

	public static void queueInfo(TextChannel channel) {
		GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());

		if (musicManager.player.getPlayingTrack() == null) {
			channel.sendMessage(":x: | Não há nenhuma musica na fila no momento.").queue();
			return;
		}

		EmbedBuilder eb = new EmbedBuilder();

		eb.setTitle("Fila de músicas:");
		musicManager.scheduler.queue().forEach(t -> eb.addField(t.getPosition() + t.getInfo().title, "Requisitado por " + ((User) t.getUserData()).getAsMention(), false));
		eb.setFooter("Tempo estimado da fila: " + String.valueOf(Helper.round((musicManager.scheduler.queue().stream().mapToDouble(AudioTrack::getDuration).sum() / 1000) / 60, 2)).replace(".", ":"), null);

		channel.sendMessage(eb.build()).queue();
	}

	public static void setVolume(TextChannel channel, int volume) {
		GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());

		if (volume <= 100 && volume > 0) musicManager.player.setVolume(volume);
		else {
			channel.sendMessage(":x: | O volume deve ser um valor inteiro entre 0 e 100").queue();
			return;
		}

		channel.sendMessage("Volume trocado para " + volume + "%.").queue();
	}

	public static void loadAndPlay(final Member m, final TextChannel channel, final String trackUrl) {
		GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());

		Main.getInfo().getApm().loadItemOrdered(musicManager, trackUrl, new AudioLoadResultHandler() {
			@Override
			public void trackLoaded(AudioTrack track) {
				channel.sendMessage("Musíca adicionada com sucesso à fila: " + track.getInfo().title).queue();

				if (Objects.requireNonNull(m.getVoiceState()).inVoiceChannel()) {
					track.setUserData(m.getUser());
					play(m.getVoiceState().getChannel(), channel.getGuild(), musicManager, track);
				} else channel.sendMessage(":x: | Você não está conectado em um canal de voz.").queue();
			}

			@Override
			public void playlistLoaded(AudioPlaylist playlist) {
				AudioTrack firstTrack = playlist.getSelectedTrack();

				if (firstTrack == null) {
					firstTrack = playlist.getTracks().get(0);
				}

				channel.sendMessage("Musíca adicionada com sucesso à fila: " + playlist.getName()).queue();

				if (Objects.requireNonNull(m.getVoiceState()).inVoiceChannel()) {
					firstTrack.setUserData(m.getUser());
					play(m.getVoiceState().getChannel(), channel.getGuild(), musicManager, firstTrack);
				} else channel.sendMessage(":x: | Você não está conectado em um canal de voz.").queue();
			}

			@Override
			public void noMatches() {
				channel.sendMessage(":x: | Nenhuma música encontrada com o link " + trackUrl).queue();
			}

			@Override
			public void loadFailed(FriendlyException exception) {
				channel.sendMessage(":x: | Erro ao tocar a música: " + exception.getMessage()).queue();
			}
		});
	}

	public static synchronized GuildMusicManager getGuildAudioPlayer(Guild guild) {
		long guildId = guild.getIdLong();
		GuildMusicManager musicManager = Main.getInfo().getGmms().get(guildId);

		if (musicManager == null) {
			musicManager = new GuildMusicManager(Main.getInfo().getApm());
			Main.getInfo().addGmm(guildId, musicManager);
		}

		guild.getAudioManager().setSendingHandler(musicManager.getSendHandler());

		return musicManager;
	}
}
