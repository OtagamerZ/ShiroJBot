package com.kuuhaku.command.commands.misc;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;
import org.json.JSONException;
import org.json.JSONObject;

import java.awt.*;
import java.io.IOException;

public class EmbedCommand extends Command {

	public EmbedCommand() {
		super("embed", "<JSON>", "Cria um embed. Os campos do JSON são `title`, `color`, `thumbnail`, `body`, `fields[name, value]` e `footer`", Category.MISC);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		channel.sendMessage("<a:Loading:598500653215645697> Construindo embed...").queue(m -> {
			try {
				JSONObject json = new JSONObject(String.join(" ", args));

				EmbedBuilder eb = new EmbedBuilder();

				if (json.has("title")) eb.setTitle(json.getString("title"));
				if (json.has("color")) eb.setColor(Color.decode(json.getString("color")));
				if (json.has("thumbnail")) eb.setThumbnail(json.getString("thumbnail"));
				if (json.has("body")) eb.setDescription(json.getString("body"));

				if (json.has("fields")) json.getJSONArray("fields").forEach(j -> {
					try {
						JSONObject jo = (JSONObject) j;
						eb.addField(jo.getString("name"), jo.getString("value"), true);
					} catch (Exception ignore) {
					}
				});

				if (json.has("footer")) eb.setFooter(json.getString("footer"), null);

				m.delete().queue();
				channel.sendMessage(eb.build()).queue();
			} catch (JSONException ex) {
				m.editMessage(":x: | JSON em formato inválido, recomendo utilizar este site para checar se está tudo correto: https://jsonlint.com/.").queue();
			} catch (Exception e) {
				m.editMessage(":x: | Erro ao construir embed, talvez você não tenha passado nenhum argumento.").queue();
			}
		});
	}
}
