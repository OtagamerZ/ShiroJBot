package com.kuuhaku.command.commands.misc;

import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.Page;
import com.github.ygimenez.type.PageType;
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.sqlite.BackupDAO;
import com.kuuhaku.controller.sqlite.CustomAnswerDAO;
import com.kuuhaku.controller.sqlite.GuildDAO;
import com.kuuhaku.model.CustomAnswers;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.PrivilegeLevel;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CustomAnswerCommand extends Command {

	public CustomAnswerCommand() {
		super("fale", "<gatilho>;<resposta>", "Configura uma resposta para o gatilho (frase) especificado.", Category.MISC);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		if (!Helper.hasPermission(member, PrivilegeLevel.MOD) && GuildDAO.getGuildById(guild.getId()).isNotAnyTell()) {
			channel.sendMessage(":x: | Este servidor não está configurado para permitir respostas customizadas da comunidade.").queue();
			return;
		} else if (args.length == 0) {
			channel.sendMessage(":x: | Você precisa definir um gatilho e uma mensagem.").queue();
			return;
		} else if (args[0].equals("lista")) {
			List<Page> pages = new ArrayList<>();

			List<CustomAnswers> ca = BackupDAO.getCADump();
			EmbedBuilder eb = new EmbedBuilder();
			ca.removeIf(a -> !a.getGuildID().equals(guild.getId()));

			for (int x = 0; x < Math.ceil(ca.size() / 10f); x++) {
				eb.clear();
				eb.setTitle(":pencil: Respostas deste servidor:");
				eb.setColor(Helper.getRandomColor());
				for (int i = -10 + (10 * (x + 1)); i < ca.size() && i < (10 * (x + 1)); i++) {
					eb.addField(ca.get(i).getId() + " - " + ca.get(i).getGatilho(), ca.get(i).getAnswer().length() > 100 ? ca.get(i).getAnswer().substring(0, 100) + "..." : ca.get(i).getAnswer(), false);
				}
				pages.add(new Page(PageType.EMBED, eb.build()));
			}

			if (pages.size() == 0) {
				channel.sendMessage("Não há nenhuma resposta cadastrada neste servidor.").queue();
				return;
			}

			channel.sendMessage((MessageEmbed) pages.get(0).getContent()).queue(s -> Pages.paginate(Main.getInfo().getAPI(), s, pages, 60, TimeUnit.SECONDS));
			return;
		} else if (StringUtils.isNumeric(args[0]) && !args[0].contains(";")) {
			List<CustomAnswers> ca = BackupDAO.getCADump();
			ca.removeIf(a -> !String.valueOf(a.getId()).equals(args[0]) || !a.getGuildID().equals(guild.getId()));
			if (ca.size() == 0) {
				channel.sendMessage(":x: | Esta resposta não existe!").queue();
				return;
			}
			CustomAnswers c = ca.get(0);

			EmbedBuilder eb = new EmbedBuilder();

			eb.setTitle(":speech_balloon: Resposta Nº " + c.getId());
			eb.addField(":arrow_right: " + c.getGatilho(), ":arrow_left: " + c.getAnswer(), false);
			eb.setColor(Helper.getRandomColor());

			channel.sendMessage(eb.build()).queue();
			return;
		}

		String txt = String.join(" ", args);

		if (txt.contains(";")) {
			if (txt.split(";")[0].length() <= 200) {
				if (txt.split(";")[1].length() <= 200) {
					CustomAnswerDAO.addCAtoDB(guild, txt.split(";")[0], txt.split(";")[1]);
					channel.sendMessage("Agora quando alguém disser `" + txt.split(";")[0] + "` irei responder `" + txt.split(";")[1] + "`.").queue();
				} else {
					channel.sendMessage(":x: | Woah, essa resposta é muito longa, não consigo decorar isso tudo!").queue();
				}
			} else {
				channel.sendMessage(":x: | Hum, esse gatilho é grande demais para eu lembrar, digite um gatilho melhor por favor!").queue();
			}
		} else {
			channel.sendMessage(":x: | O gatilho e a resposta devem estar separados por ponto e virgula (`;`).").queue();
		}
	}
}
