package com.kuuhaku.command.commands.partner;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.MySQL;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.PrivilegeLevel;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.Event;

import javax.persistence.NoResultException;
import java.awt.*;

public class TetCommand extends Command {

	public TetCommand() {
		super("tet", "Chama o Tet para ser a mensageira em seu servidor.", Category.PARTNER);
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

		eb.setThumbnail("https://www.pacific.edu/Images/library/Renovation%20Renderings/LogoMakr_2mPTly.png");
		eb.setTitle("Olá, obrigado por apoiar meu desenvolvimento!");
		eb.setDescription("Para chamar o Tet para seu servidor, utilize este link:\n"+System.getenv("TET_LINK"));
		eb.setColor(Color.green);

		author.openPrivateChannel().queue(c -> c.sendMessage(eb.build()).queue());
	}

}
