package com.kuuhaku.command.commands.partner;

import bsh.Interpreter;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;

public class CompileCommand extends Command {
	public CompileCommand() {
		super("compilar", new String[]{"compile"}, "Executa um código Java.", Category.PARTNER);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		String code = String.join(" ", args);
		channel.sendMessage("<a:Loading:598500653215645697> | Compilando...").queue(m -> {
			try {
				if (!code.contains("out")) throw new Exception("Código sem retorno");
				Interpreter i = new Interpreter();
				i.set("msg", message);
				i.eval(code);
				Object out = i.get("out");
				m.editMessage("<:Verified:591425071772467211> | Compilado com sucesso!").queue(n ->
						m.getChannel().sendMessage("<a:Loading:598500653215645697> | Executando...").queue(d ->
								d.editMessage("-> " + out.toString()).queue()));
			} catch (Exception e) {
				m.editMessage(":x: | Erro ao compilar: " + e.toString()).queue();
			}
		});
	}

}
