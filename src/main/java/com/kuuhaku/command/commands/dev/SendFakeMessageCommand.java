package com.kuuhaku.command.commands.dev;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import net.dv8tion.jda.api.entities.*;
import org.json.JSONObject;

public class SendFakeMessageCommand extends Command {

	public SendFakeMessageCommand() {
		super("fake", "Envia uma mensagem de teste para o aplicativo", Category.DEVS);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		JSONObject jo = new JSONObject();
		jo.put("userID", "572413282653306901");
		jo.put("content", String.join(" ", args));

		Main.getInfo().getClient().emit("chatevent", jo.toString());
	}
}
