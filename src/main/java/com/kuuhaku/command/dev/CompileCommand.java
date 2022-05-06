/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2022  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.command.dev;

import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Signature;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.utils.XStringBuilder;
import com.kuuhaku.utils.json.JSONObject;
import groovy.lang.GroovyShell;
import kotlin.Pair;
import net.dv8tion.jda.api.JDA;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

@Command(
		name = "eval",
		category = Category.DEV
)
@Signature("<code:text:r>")
public class CompileCommand implements Executable {
	private static final ExecutorService exec = Executors.newFixedThreadPool(2);

	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		event.channel().sendMessage(locale.get("str/compiling")).queue(m -> {
			Future<Pair<String, Long>> execute = exec.submit(() -> {
				AtomicLong time = new AtomicLong();

				try {
					String code = args.getString("code").replaceAll("```(?:.*\n)?", "").trim();

					Future<?> fut = exec.submit(() -> {
						GroovyShell gs = new GroovyShell();
						gs.setVariable("msg", event.message());

						time.set(System.currentTimeMillis());
						Object out = gs.evaluate(code);
						time.getAndUpdate(t -> System.currentTimeMillis() - t);

						return out;
					});

					return new Pair<>(String.valueOf(fut.get(1, TimeUnit.MINUTES)), time.get());
				} catch (TimeoutException e) {
					return new Pair<>(locale.get("error/timeout"), -1L);
				} catch (Exception e) {
					Throwable t = e;
					while (t.getCause() != null) {
						t = t.getCause();
					}

					XStringBuilder sb = new XStringBuilder(t.toString());
					for (StackTraceElement element : t.getStackTrace()) {
						if (element.toString().startsWith("com.kuuhaku")) {
							sb.appendNewLine("at " + element);
						}
					}

					return new Pair<>(sb.toString(), -1L);
				}
			});

			try {
				Pair<String, Long> out = execute.get();
				if (out.getSecond() > -1) {
					m.editMessage("""
							```
							(%s ms) Out -> %s
							```
							""".formatted(out.getSecond(), out.getFirst().replace("`", "'"))
					).queue();
				} else {
					m.editMessage("""
							```
							Err -> %s
							```
							""".formatted(out.getFirst().replace("`", "'"))
					).queue();
				}
			} catch (ExecutionException | InterruptedException e) {
				logger().error(e, e);
			}
		});
	}
}
