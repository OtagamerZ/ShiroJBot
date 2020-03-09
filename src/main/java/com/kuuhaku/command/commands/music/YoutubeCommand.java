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

package com.kuuhaku.command.commands.music;

import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.Page;
import com.github.ygimenez.type.PageType;
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.Youtube;
import com.kuuhaku.model.common.YoutubeVideo;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.Music;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class YoutubeCommand extends Command {

    public YoutubeCommand(String name, String description, Category category, boolean requiresMM) {
        super(name, description, category, requiresMM);
    }

    public YoutubeCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
        super(name, aliases, description, category, requiresMM);
    }

    public YoutubeCommand(String name, String usage, String description, Category category, boolean requiresMM) {
        super(name, usage, description, category, requiresMM);
    }

    public YoutubeCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category, boolean requiresMM) {
        super(name, aliases, usage, description, category, requiresMM);
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
                                eb.setAuthor("Para ouvir essa música, conecte-se à um canal de voz e clique no botão ✅");
                                eb.setTitle(v.getTitle(), v.getUrl());
                                eb.setDescription(v.getDesc());
                                eb.setThumbnail(v.getThumb());
                                eb.setColor(Helper.colorThief(v.getThumb()));
                                eb.setFooter("Link: " + v.getUrl(), v.getUrl());
                                pages.add(new Page(PageType.EMBED, eb.build()));
                            }

                            channel.sendMessage((MessageEmbed) pages.get(0).getContent()).queue(msg -> {
                                Pages.paginate(Main.getInfo().getAPI(), msg, pages, 60, TimeUnit.SECONDS);
                                if (Objects.requireNonNull(member.getVoiceState()).inVoiceChannel()) {
                                    Pages.buttonize(Main.getInfo().getAPI(), msg, Collections.singletonMap(Helper.ACCEPT, (mb, ms) -> {
                                        Music.loadAndPlay(member, (TextChannel) channel, Objects.requireNonNull(channel.retrieveMessageById(msg.getId()).complete().getEmbeds().get(0).getFooter()).getIconUrl());
                                        msg.delete().queue();
                                    }), true, 60, TimeUnit.SECONDS);
                                }
                            });
                        } else m.editMessage(":x: | Nenhum vídeo encontrado").queue();
                    } catch (IOException e) {
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
