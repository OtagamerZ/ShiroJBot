package com.kuuhaku.command.commands.misc;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.Event;

public class LinkTesterCommand extends Command {

	public LinkTesterCommand() {
		super("link", new String[]{"try"}, "<link>", "Testa um link para ver se ele consegue burlar a detecção de links.", Category.MISC);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		if (args.length == 0) {
			channel.sendMessage(":x: | É necessário informar um link").queue();
			return;
		}

		String link = String.join(" ", args);

		String[] quotes = new String[]{
				"Peguei esse!",
				"Opa, hoje não!",
				"Quase!",
				"To ficando boa nisso!",
				"Ha! Esse já conheço",
				"Peguei no flagra!"
		};

		if (Helper.findURL(link)) {
			channel.sendMessage(quotes[
					(int) Helper.clamp(Math.round(quotes.length * Math.random()), 0, quotes.length - 1)
					]).queue();
		} else {
			channel.sendMessage("Não detectei nenhum link nesta mensagem. Caso seja um link, por favor use o comando abaixo para informar meus desenvolvedores!\n`s!bug Link não detectado - " + link + "`").queue();
		}
	}
}