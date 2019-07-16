package com.kuuhaku.command.commands.partner;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;
import org.joor.Reflect;

import java.util.function.Supplier;

public class CompileCommand extends Command {
	public CompileCommand() {
		super("compilar", new String[]{"compile"}, "Executa um código Java.", Category.PARTNER);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		String code = String.join(" ", args);
		channel.sendMessage("<a:Loading:598500653215645697> | Compilando...").queue(m -> {
			try {
				if (!code.contains("return")) throw new Exception("Código sem retorno");
				Supplier<String> compCode = Reflect.compile(
						String.valueOf(System.currentTimeMillis()),
						"import java.util.*;" +
								"import java.awt.*;" +
								"import com.kuuhaku.utils.Sandbox;" +
								"import static com.kuuhaku.Main.env;" +
								"public class Dynamic extends Sandbox implements java.util.function.Supplier<String> {\n" +
								"	public String get() {\n" +
								"		super.msg = env.msg;" +
								code +
								"\n" +
								"	}\n" +
								"}").create().get();

				m.editMessage("<:Verified:591425071772467211> | Compilado com sucesso!").queue(n ->
						m.getChannel().sendMessage("<a:Loading:598500653215645697> | Executando...").queue(d ->
								d.editMessage("-> " + compCode.get()).queue()));
			} catch (Exception e) {
				m.editMessage(":x: | Erro ao compilar: " + e.toString()).queue();
			}
		});
	}

}
