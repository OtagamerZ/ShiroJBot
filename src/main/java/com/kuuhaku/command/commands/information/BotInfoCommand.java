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

package com.kuuhaku.command.commands.information;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.mysql.TagDAO;
import com.kuuhaku.controller.sqlite.MemberDAO;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;

import java.time.format.DateTimeFormatter;

public class BotInfoCommand extends Command {

	public BotInfoCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public BotInfoCommand(@NonNls String name, @NonNls String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public BotInfoCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public BotInfoCommand(String name, String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		EmbedBuilder eb = new EmbedBuilder();

		eb.setTitle(":dividers: Dados sobre a Shiro J. Bot");
		eb.setThumbnail(Main.getInfo().getAPI().getSelfUser().getAvatarUrl());
		eb.addField(":triangular_flag_on_post: Projeto inicial por:", Main.getInfo().getUserByID(Main.getInfo().getNiiChan()).getAsTag(), true);
		StringBuilder sb = new StringBuilder();
		Main.getInfo().getDevelopers().forEach(d -> sb.append(Main.getInfo().getUserByID(d).getAsTag()).append(", "));
		eb.addField(":tools: Desenvolvida por:", sb.toString(), true);
		eb.addField(":calendar_spiral: Criada em:", Main.getInfo().getSelfUser().getTimeCreated().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), true);
		eb.addField(":handshake: Apoiada por:", TagDAO.getPartnerAmount() + " servidores", true);
		eb.addField(":earth_americas: Estou em:", Main.getInfo().getAPI().getGuilds().size() + " servidores", true);
		eb.addField(":speech_balloon: Conheço:", Main.getInfo().getAPI().getUsers().size() + " usuários (" + MemberDAO.getAllMembers().size() + " cadastrados)", true);
		eb.addField(":dividers: Versão:", Main.getInfo().getVersion(), true);
		eb.addField(":envelope: Link de convite:", "https://top.gg/bot/572413282653306901", true);
		eb.setImage("https://discordbots.org/api/widget/572413282653306901.png?usernamecolor=b463ff&topcolor=000000&middlecolor=1a1d23&datacolor=b463ff");

		channel.sendMessage(eb.build()).queue();
	}
}
