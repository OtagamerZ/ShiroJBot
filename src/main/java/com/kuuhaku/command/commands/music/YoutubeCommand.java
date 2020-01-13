package com.kuuhaku.command.commands.music;

import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.Page;
import com.github.ygimenez.type.PageType;
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.Youtube;
import com.kuuhaku.model.YoutubeVideo;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class YoutubeCommand extends Command {

    public YoutubeCommand() {
        super("play", new String[]{"yt", "youtube"}, "<nome>", "Busca um vídeo no YouTube.", Category.MUSICA);
    }

    @Override
    public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
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
                            List<Page> pages = new ArrayList<>();

                            for (YoutubeVideo v : videos) {
                                eb.clear();
                                eb.setTitle(v.getTitle(), v.getUrl());
                                eb.setDescription(v.getDesc());
                                eb.setThumbnail(v.getThumb());
                                eb.setColor(Helper.colorThief(v.getThumb()));
                                eb.setFooter("Link: " + v.getUrl(), null);
                                pages.add(new Page(PageType.EMBED, eb.build()));
                            }

                            channel.sendMessage((MessageEmbed) pages.get(0).getContent()).queue(msg -> {
                                Pages.paginate(Main.getInfo().getAPI(), msg, pages, 60, TimeUnit.SECONDS);
                                if (Objects.requireNonNull(member.getVoiceState()).inVoiceChannel()) {
                                    msg.addReaction(Helper.ACCEPT).queue();
                                }
                            });
                        } else m.editMessage(":x: | Nenhum vídeo encontrado").queue();
                    }catch (IOException e) {
                        m.editMessage(":x: | Erro ao buscar vídeos, meus developers já foram notificados.").queue();
                        Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
                    }
                });
            } catch (IOException e) {
                m.editMessage(":x: | Erro ao buscar vídeos, meus developers já foram notificados.").queue();
                Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
            }
        });
    }
}
