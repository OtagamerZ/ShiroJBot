package com.kuuhaku.command.commands.information;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.Tradutor;
import com.kuuhaku.controller.Youtube;
import com.kuuhaku.model.Anime;
import com.kuuhaku.model.YoutubeVideo;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.LogLevel;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;
import org.json.JSONObject;

import java.awt.*;
import java.io.IOException;
import java.util.List;

public class YoutubeCommand extends Command {

    public YoutubeCommand() {
        super("video", new String[]{"yt", "youtube"}, "Busca um vídeo no YouTube.", Category.INFO);
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
                            for (YoutubeVideo v : videos) {
                                eb.setTitle(v.getTitle(), v.getUrl());
                                eb.setDescription(v.getDesc());
                                eb.setThumbnail(videos.get(0).getThumb());
                                eb.setColor(Helper.colorThief(v.getThumb()));
                                eb.setFooter("Link: " + v.getUrl(), null);
                                channel.sendMessage(eb.build()).queue();
                            }
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
