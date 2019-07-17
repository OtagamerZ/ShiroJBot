package com.kuuhaku.command.commands.misc;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;

public class EmbedCommand extends Command {

	public EmbedCommand() {
		super("embed", "<título;conteudo;imagem>", "Cria um embed.", Category.MISC);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
        String format = String.join(" ", args);
		try {
			if (format.split(";").length < 3) {
				channel.sendMessage(":x: | Dados insuficientes, por favor separe o título, o conteúdo e a imagem com `;`.").queue();
				return;
			} else if (format.split(";")[0].length() > 256) {
				channel.sendMessage(":x: | Título muito grande (Max. 256 caracteres).").queue();
				return;
			} else if (format.split(";")[1].length() > 2000) {
				channel.sendMessage(":x: | Conteúdo muito grande (Max. 2000 caracteres).").queue();
				return;
			}

			EmbedBuilder eb = new EmbedBuilder();

			eb.setAuthor("Feito por " + member.getEffectiveName());
			eb.setTitle(format.split(";")[0]);
			eb.setDescription(format.split(";")[1]);
			eb.setThumbnail(format.split(";")[2]);

			channel.sendMessage(eb.build()).queue();
		} catch (Exception e) {
			channel.sendMessage(":x: | Erro ao tentar acessar o link da imagem.").queue();
		}
	}

}
