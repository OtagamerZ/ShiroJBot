/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2023  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.command.staff;

import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Syntax;
import com.kuuhaku.model.common.XStringBuilder;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONObject;
import kotlin.Pair;
import net.dv8tion.jda.api.JDA;
import org.apache.commons.lang3.time.StopWatch;
import org.intellij.lang.annotations.Language;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Command(
		name = "eval",
		category = Category.DEV
)
@Syntax("<code:text:r>")
public class CompileCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		Utils.sendLoading(data, locale.get("str/compiling"), m -> {
			Pair<String, Long> out = executeSnippet(
					locale, event,
					args.getString("code").replaceAll("```(?:.*\n)?", "").trim()
			);

			if (out.getSecond() > -1) {
				return m.editMessage("""
						```
						(%s ms) Out -> %s
						```
						""".formatted(out.getSecond(), out.getFirst().replace("`", "'"))
				);
			} else {
				return m.editMessage("""
						```
						Err -> %s
						```
						""".formatted(out.getFirst().replace("`", "'"))
				);
			}
		});
	}

	public Pair<String, Long> executeSnippet(I18N locale, MessageData.Guild event, @Language("Groovy") String code) {
		StopWatch time = new StopWatch();

		try {
			Future<?> fut = CompletableFuture.supplyAsync(() -> {
				time.start();
				Object out = Utils.exec(getClass().getSimpleName(), code, Map.of("msg", event.message()));
				time.stop();

				return out;
			});

			return new Pair<>(String.valueOf(fut.get(1, TimeUnit.MINUTES)), time.getTime(TimeUnit.MILLISECONDS));
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
	}
}
