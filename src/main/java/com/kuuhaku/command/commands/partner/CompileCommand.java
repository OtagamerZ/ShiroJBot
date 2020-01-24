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

package com.kuuhaku.command.commands.partner;

import bsh.Interpreter;
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.utils.BannedVars;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.*;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class CompileCommand extends Command {

	public CompileCommand(String name, String description, Category category) {
		super(name, description, category);
	}

	public CompileCommand(String name, String[] aliases, String description, Category category) {
		super(name, aliases, description, category);
	}

	public CompileCommand(String name, String usage, String description, Category category) {
		super(name, usage, description, category);
	}

	public CompileCommand(String name, String[] aliases, String usage, String description, Category category) {
		super(name, aliases, usage, description, category);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		channel.sendMessage("<a:Loading:598500653215645697> | Compilando...").queue(m -> {
			try {
				Future<?> execute = Main.getInfo().getPool().submit(() -> {
					final long start = System.currentTimeMillis();
					try {
						String code = String.join(" ", args);
						if (!code.contains("out")) throw new IllegalArgumentException("Código sem retorno.");
						else if (code.contains("```") && !code.contains("```java")) {
							throw new IllegalArgumentException("Bloco de código com começo incorreto");
						} else if (Arrays.stream(BannedVars.vars).parallel().anyMatch(code::contains))
							throw new IllegalAccessException("Código com métodos proibidos.");
						code = code.replace("```java", "").replace("```", "");
						Interpreter i = new Interpreter();
						i.set("msg", message);
						i.set("code", String.join(" ", args));
						i.eval(code);
						Object out = i.get("out");
						m.getChannel().sendMessage("<a:Loading:598500653215645697> | Executando...").queue(d ->
								d.editMessage("-> " + out.toString()).queue());
						message.delete().queue();
						m.editMessage("<:Verified:591425071772467211> | Tempo de execução: " + (System.currentTimeMillis() - start) + " ms").queue();
					} catch (Exception e) {
						m.editMessage(":x: | Erro ao compilar: ```" + e.toString().replace("`", "´") + "```").queue();
					}
					return null;
				});
				try {
					execute.get(10, TimeUnit.SECONDS);
				} catch (InterruptedException | ExecutionException e) {
					Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
				} catch (TimeoutException e) {
					execute.cancel(true);
					m.editMessage(":x: | Tempo limite de execução esgotado.").queue();
				}
			} catch (Exception e) {
				m.editMessage(":x: | Erro ao compilar: ```" + e.toString().replace("`", "´") + "```").queue();
			}
		});
	}


}
