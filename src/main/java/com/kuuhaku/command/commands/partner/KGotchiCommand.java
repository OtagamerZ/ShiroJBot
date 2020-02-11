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

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.mysql.AccountDAO;
import com.kuuhaku.controller.sqlite.KGotchiDAO;
import com.kuuhaku.handlers.games.kawaigotchi.Food;
import com.kuuhaku.handlers.games.kawaigotchi.FoodMenu;
import com.kuuhaku.handlers.games.kawaigotchi.Kawaigotchi;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.*;

import java.io.IOException;

public class KGotchiCommand extends Command {

	public KGotchiCommand(String name, String description, Category category) {
		super(name, description, category);
	}

	public KGotchiCommand(String name, String[] aliases, String description, Category category) {
		super(name, aliases, description, category);
	}

	public KGotchiCommand(String name, String usage, String description, Category category) {
		super(name, usage, description, category);
	}

	public KGotchiCommand(String name, String[] aliases, String usage, String description, Category category) {
		super(name, aliases, usage, description, category);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		Account acc = AccountDAO.getAccount(author.getId());
		Kawaigotchi k = KGotchiDAO.getKawaigotchi(author.getId());

		if (k == null) {
			channel.sendMessage(":x: | Você não possui um Kawaigotchi.").queue();
			return;
		} else if (args.length == 0) {
			try {
				k.view((TextChannel) channel).queue();
			} catch (IOException e) {
				Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
			}
			return;
		}

		if (Helper.containsAny(args[0], "alimentar", "feed", "darcomida", "givefood")) {
			//TODO Menu de alimentos
			Food f = FoodMenu.getFood(args[1].toLowerCase());
		} else if (Helper.containsAny(args[0], "brincar", "play")) {
			switch (k.play()) {
				case FAILED:
					channel.sendMessage("Ah, parece que " + k.getName() + " não quer brincar. Tente novamente!").queue();
					return;
				case SUCCESS:
					channel.sendMessage("Vocês brincaram por bastante tempo, " + k.getName() + " está mais feliz agora!").queue();
					return;
				case UNABLE:
					channel.sendMessage("Não parece que " + k.getName() + " possa brincar agora!").queue();
					return;
			}
		} else if (Helper.containsAny(args[0], "treinar", "train")) {
			switch (k.train()) {
				case FAILED:
					channel.sendMessage(k.getName() + " fugiu do treino, parece que está com preguiça. Tente novamente!").queue();
					return;
				case SUCCESS:
					channel.sendMessage(k.getName() + " treinou muito, agora ficou monstrão!").queue();
					return;
				case UNABLE:
					channel.sendMessage("Não parece que " + k.getName() + " possa treinar agora!").queue();
					return;
			}
		} else if (Helper.containsAny(args[0], "comprar", "buy")) {
			//TODO Menu de loja de alimentos
		}
	}
}
