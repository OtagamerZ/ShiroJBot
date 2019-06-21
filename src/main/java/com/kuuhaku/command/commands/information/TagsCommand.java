/*
 * This file is part of Shiro J Bot.
 *
 *     Shiro J Bot is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Shiro J Bot is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Shiro J Bot.  If not, see <https://www.gnu.org/licenses/>
 */

package com.kuuhaku.command.commands.information;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.MySQL;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.PrivilegeLevel;
import com.kuuhaku.utils.TagIcons;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;

import javax.persistence.NoResultException;

public class TagsCommand extends Command {

    public TagsCommand() {
        super("tags", new String[]{"emblemas", "insignias"}, "Mostra os emblemas disponíveis.", Category.PARTNER);
    }

    @Override
    public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
        try {
            if (!MySQL.getTagById(author.getId()).isPartner() && !Helper.hasPermission(member, PrivilegeLevel.DEV)) {
                channel.sendMessage(":x: | Este comando é exlusivo para parceiros!").queue();
                return;
            }
        } catch (NoResultException e) {
            channel.sendMessage(":x: | Este comando é exlusivo para parceiros!").queue();
            return;
        }

        EmbedBuilder eb = new EmbedBuilder();

        eb.setTitle(":label: Emblemas do chat global");
        eb.addField(TagIcons.getTag(TagIcons.DEV) + " Desenvolvedor", "Desenvolvedores da Shiro/Jibril", false);
        eb.addField(TagIcons.getTag(TagIcons.EDITOR) + " Redator", "Redatores da Shiro/Jibril", false);
        eb.addField(TagIcons.getTag(TagIcons.PARTNER) + " Parceiro", "Parceiros de desenvolvimento da Shiro/Jibril", false);
        eb.addField(TagIcons.getTag(TagIcons.MODERATOR) + " Moderador", "Equipe administrativa do servidor de onde a mensagem foi enviada", false);
        eb.addField(TagIcons.getTag(TagIcons.CHAMPION) + " Campeão", "Usuário que está no ranking Nº 1 das Beyblades", false);
        eb.addField(TagIcons.getTag(TagIcons.VETERAN) + " Veterano", "Membro com nível maior ou igual a 20", false);
        eb.addField(TagIcons.getTag(TagIcons.VERIFIED) + " Verificado", "Usuário com conduta e identidade verificada", false);
        eb.addField(TagIcons.getTag(TagIcons.TOXIC) + " Tóxico", "Usuário com péssima conduta", false);

        channel.sendMessage(eb.build()).queue();
    }
}
