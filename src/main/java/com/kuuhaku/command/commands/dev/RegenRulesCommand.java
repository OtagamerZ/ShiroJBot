package com.kuuhaku.command.commands.dev;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.Event;

import java.io.IOException;

public class RegenRulesCommand extends Command {

	public RegenRulesCommand() {
		super("rrules", new String[]{"makerules"}, "Regenera as regras do servidor de suporte.", Category.DEVS);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		message.delete().queue(s -> {
			try {
				if (guild.getId().equals("421495229594730496")) channel.sendFile(Helper.getImage("https://i.imgur.com/JQ3LvGK.png"), "title.png").queue();
				channel.sendFile(Helper.getImage("https://i.imgur.com/9dfpeel.png"), "welcome.png").queue();
				channel.sendMessage(guild.getId().equals("421495229594730496") ? "Seja bem-vindo(a) ao meu servidor oficial de suporte, qualquer duvida que tenha sobre como me utilizar será esclarecida por um de nossos membros, fique à vontade e lembre-se de sempre relatar quando achar algo suspeito" : "Seja bem-vindo(a) ao servidor " + guild.getName() + ", fique à vontade e lembre-se de sempre relatar quando achar algo suspeito").queue();
				channel.sendFile(Helper.getImage("https://i.imgur.com/aCYUW1G.png"), "rules.png").queue();
				channel.sendMessage("**1 - É proibido qualquer ato de SPAM, flood ou bullying**\n" +
						"Por razões obvias, tais coisas poluem o servidor e prejudicam a comunidade e é considerado um ato grave para medidas de punição.\n" +
						"Infratores serão punidos de acordo com seus atos.\n\n" +
						"**2 - Proibido postar qualquer conteúdo pornográfico ou racista**\n" +
						"Ninguem gosta de racismo, e este servidor não é um lugar para coisas NSFW, então mantenha-os no seu HD. (Inclui avatares)\n\n" +
						"**3 - Atenha-se ao tópico**\n" +
						"Existem vários canais para diversos assuntos, não há necessidade de entupir tudo em um canal só.\n\n" +
						"**4 - Não peça cargos, xp ou coisas do tipo**\n" +
						"Eu sou a única que gerencia isso, e meu Nii-chan não irá favorecer ninguem!\n" +
						"Pronto pessoal, isso é tudo!\n" +
						(guild.getId().equals("421495229594730496") ? "Divirta-se e, caso tenha lido as regras você pode utilizar o comando `s!arespostaé RESPOSTA` para completar a seguinte frase e ganhar um emblema único:\n" +
						"Infratores serão `_______ __ ______ ___ ____ ____`\n\n" +
						"É proibido compartilhar qual é a resposta, se não não teria graça!" : "Caso precise de ajuda, ou queira ajudar no meu desenvolvimento, venha para nosso servidor de suporte: https://discord.gg/9sgkzna")).queue();
				channel.sendFile(Helper.getImage("https://i.imgur.com/U9lTSWD.png"), "partners.png").queue();
			} catch (IOException e) {
				Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
			}
		});
	}
}
