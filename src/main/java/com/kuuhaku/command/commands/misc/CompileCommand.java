package com.kuuhaku.command.commands.misc;

import bsh.Interpreter;
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.utils.BannedVars;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.LogLevel;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.Event;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class CompileCommand extends Command {
	public CompileCommand() {
		super("compilar", new String[]{"compile"}, "Executa um código Java.", Category.MISC);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		channel.sendMessage("<a:Loading:598500653215645697> | Compilando...").queue(m -> {
			Future<?> execute = new Future<Object>() {
				@Override
				public boolean cancel(boolean mayInterruptIfRunning) {
					m.editMessage(":x: | Tempo limite de execução atingido.").queue();
					return true;
				}

				@Override
				public boolean isCancelled() {
					return false;
				}

				@Override
				public boolean isDone() {
					return false;
				}

				@Override
				public Object get() {
					Main.getInfo().getPool().submit(() -> {
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
							m.editMessage("<:Verified:591425071772467211> | Compilado com sucesso!").queue(n ->
									m.getChannel().sendMessage("<a:Loading:598500653215645697> | Executando...").queue(d ->
											d.editMessage("-> " + out.toString()).queue()));
							message.delete().queue();
							channel.sendMessage("<:Verified:591425071772467211> | Tempo de execução: " + (System.currentTimeMillis() - start) + " ms").queue();
						} catch (Exception e) {
							m.editMessage(":x: | Erro ao compilar: ```" + e.toString().replace("`", "´") + "```").queue();
						}
					});
					return null;
				}

				@Override
				public Object get(long timeout, @NotNull TimeUnit unit) {
					Main.getInfo().getPool().submit(() -> {
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
							m.editMessage("<:Verified:591425071772467211> | Compilado com sucesso!").queue(n ->
									m.getChannel().sendMessage("<a:Loading:598500653215645697> | Executando...").queue(d ->
											d.editMessage("-> " + out.toString()).queue()));
							message.delete().queue();
							channel.sendMessage("<:Verified:591425071772467211> | Tempo de execução: " + (System.currentTimeMillis() - start) + " ms").queue();
						} catch (Exception e) {
							m.editMessage(":x: | Erro ao compilar: ```" + e.toString().replace("`", "´") + "```").queue();
						}
					});
					return null;
				}
			};
			try {
				System.out.println("Iniciando execução");
				execute.get(5, TimeUnit.SECONDS);
			} catch (InterruptedException | ExecutionException e) {
				Helper.log(this.getClass(), LogLevel.ERROR, e + " | " + e.getStackTrace()[0]);
			} catch (TimeoutException e) {
				m.editMessage(":x: | A fila de execução está cheia.").queue();
			}
			Main.getInfo().getScheduler().schedule(() -> execute.cancel(true), 10, TimeUnit.SECONDS);
		});
	}


}
