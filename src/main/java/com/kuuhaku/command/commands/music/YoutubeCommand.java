package com.kuuhaku.command.commands.music;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.Youtube;
import com.kuuhaku.model.YoutubeVideo;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.LogLevel;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.Event;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class YoutubeCommand extends Command {

    public YoutubeCommand() {
        super("play", new String[]{"yt", "youtube"}, "Busca um vídeo no YouTube.", Category.MUSICA);
    }

    @Override
    public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
        if (args.length < 1) {
            channel.sendMessage(":x: | Você precisa digitar um nome para pesquisar.").queue();
            return;
        }
        channel.sendMessage("<a:Loading:598500653215645697> Buscando videos...").queue(m -> {
            try {
                List<YoutubeVideo> videos = Youtube.getData(String.join(" ", args));
                EmbedBuilder eb = new EmbedBuilder();

                m.editMessage(":mag: Resultados da busca").queue(s -> {
                    try {
                        if (videos.stream().findFirst().isPresent()) {
                            List<MessageEmbed> pages = new ArrayList<>();

                            for (YoutubeVideo v : videos) {
                                eb.clear();
                                eb.setTitle(v.getTitle(), v.getUrl());
                                eb.setDescription(v.getDesc());
                                eb.setThumbnail(v.getThumb());
                                eb.setColor(Helper.colorThief(v.getThumb()));
                                eb.setFooter("Link: " + v.getUrl(), null);
                                pages.add(eb.build());
                            }

                            channel.sendMessage(pages.get(0)).queue(msg -> {
                                Helper.paginate(msg, pages);
                                if (Objects.requireNonNull(member.getVoiceState()).inVoiceChannel()) {
                                    msg.addReaction("\u25B6").queue();
                                }
                            });
                        } else m.editMessage(":x: | Nenhum vídeo encontrado").queue();
                    }catch (IOException e) {
                        m.editMessage(":x: | Erro ao buscar vídeos, meus developers já foram notificados.").queue();
                        Helper.log(this.getClass(), LogLevel.ERROR, e + " | " + e.getStackTrace()[0]);
                    }
                });
            } catch (IOException e) {
                m.editMessage(":x: | Erro ao buscar vídeos, meus developers já foram notificados.").queue();
                Helper.log(this.getClass(), LogLevel.ERROR, e + " | " + e.getStackTrace()[0]);
            }
        });
    }
}
