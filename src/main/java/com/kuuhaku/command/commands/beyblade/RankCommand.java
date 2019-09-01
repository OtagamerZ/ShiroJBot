package com.kuuhaku.command.commands.beyblade;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.MySQL;
import com.kuuhaku.model.Beyblade;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.Event;

import java.awt.*;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class RankCommand extends Command {

    public RankCommand() {
        super("brank", new String[]{"branking", "btop10"}, "Mostra o ranking de Beyblades.", Category.BEYBLADE);
    }

    @Override
    public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
        channel.sendMessage("<a:Loading:598500653215645697> Buscando dados...").queue(m -> {
			try {
				List<Beyblade> rank = MySQL.getBeybladeList();
				assert rank != null;
				rank.sort(Comparator.comparing(Beyblade::getKDA));
				Collections.reverse(rank);
				Beyblade champ = rank.get(0);
				rank.remove(0);
				EmbedBuilder eb = new EmbedBuilder();
				StringBuilder sb = new StringBuilder();

				eb.setTitle(":bar_chart: TOP 10 Beyblades");
				eb.setThumbnail("https://www.pngkey.com/png/full/21-217733_free-png-trophy-png-images-transparent-winner-trophy.png");
				eb.setColor(Color.decode(champ.getColor()));
				for (int i = 0; i < rank.size() && i < 10; i++) {
					sb.append(i + 2).append(" - ").append(rank.get(i).getName()).append(" (").append(Main.getInfo().getUserByID(rank.get(i).getId()).getName()).append(") | ").append(rank.get(i).getWins()).append("/").append(rank.get(i).getLoses()).append("\n");
				}
				eb.addField("1 - " + champ.getName() + " (" + Main.getInfo().getUserByID(champ.getId()).getName() + ") | " + champ.getWins() + "/" + champ.getLoses(), sb.toString(), false);

				m.delete().queue();
				channel.sendMessage(eb.build()).queue();
			} catch (Exception e) {
				m.editMessage(":x: | Erro ao gerar placares. Este módulo está depreciado, um novo virá para substituí-lo, aguarde.").queue();
			}
        });
    }
}
