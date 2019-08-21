package com.kuuhaku.command.commands.exceed;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.MySQL;
import com.kuuhaku.controller.SQLite;
import com.kuuhaku.utils.ExceedEnums;
import com.kuuhaku.utils.TagIcons;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;

public class ExceedSelectCommand extends Command {
	public ExceedSelectCommand() {
		super("exceedselect", new String[]{"exselect", "sou"}, "Escolhe seu exceed, esta escolha é permanente.", Category.EXCEED);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		channel.sendMessage("<a:Loading:598500653215645697> Analisando dados...").queue(m -> {
			com.kuuhaku.model.Member u = SQLite.getMemberByMid(author.getId());

			if (u.getExceed().isEmpty()) {
				if (args.length == 0) {
					channel.sendMessage("Os exceeds disponíveis são:" +
							"\n" + TagIcons.getExceed(ExceedEnums.IMANITY) + "**" + ExceedEnums.IMANITY.getName() + "** - Os engenhosos humanos." +
							"\n" + TagIcons.getExceed(ExceedEnums.SEIREN) + "**" + ExceedEnums.SEIREN.getName() + "** - As inusitadas sereias." +
							"\n" + TagIcons.getExceed(ExceedEnums.WEREBEAST) + "**" + ExceedEnums.WEREBEAST.getName() + "** - Os sábios bestiais." +
							"\n" + TagIcons.getExceed(ExceedEnums.LUMAMANA) + "**" + ExceedEnums.LUMAMANA.getName() + "** - Os orgulhosos centauros." +
							"\n" + TagIcons.getExceed(ExceedEnums.EXMACHINA) + "**" + ExceedEnums.EXMACHINA.getName() + "** - Os poderosos androides." +
							"\n" + TagIcons.getExceed(ExceedEnums.FLUGEL) + "**" + ExceedEnums.FLUGEL.getName() + "** - Os místicos elfos." +
							"\n\nEscolha usando " + prefix + "`exselect EXCEED`.\n__**ESTA ESCOLHA É PERMANENTE**__").queue();
					m.delete().queue();
					return;
				}
				switch (args[0].toLowerCase()) {
					case "imanity":
						u.setExceed(ExceedEnums.IMANITY.getName());
						break;
					case "seiren":
						u.setExceed(ExceedEnums.SEIREN.getName());
						break;
					case "werebeast":
						u.setExceed(ExceedEnums.WEREBEAST.getName());
						break;
					case "lumamana":
						u.setExceed(ExceedEnums.LUMAMANA.getName());
						break;
					case "ex-machina":
						u.setExceed(ExceedEnums.EXMACHINA.getName());
						break;
					case "flügel":
						u.setExceed(ExceedEnums.FLUGEL.getName());
						break;
					default:
						channel.sendMessage(":x: | Exceed inexistente.").queue();
						return;
				}
				SQLite.updateMemberSettings(u);
				channel.sendMessage("Exceed escolhido com sucesso, você agora pertence à **" + u.getExceed() + "**.").queue();
				MySQL.getExceedMembers(ExceedEnums.getByName(u.getExceed())).forEach(em ->
						Main.getInfo().getUserByID(em.getMid()).openPrivateChannel().queue(c -> {
							try {
								c.sendMessage(author.getAsTag() + " juntou-se à " + u.getExceed() + ", dê-o(a) as boas-vindas!").queue();
							} catch (Exception ignore) {
							}
						}));
				m.delete().queue();
			} else {
				m.editMessage(":x: | Você já pertence à um exceed, não é possível trocá-lo.").queue();
			}
		});
	}
}
