package com.kuuhaku.command.commands.beyblade;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.MySQL;
import com.kuuhaku.model.Beyblade;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;

public class RedeemCommand extends Command {

	public RedeemCommand() {
		super("bredeem", new String[]{"bresgatar", "bdaily"}, "Coleta os pontos diários por voto.", Category.BEYBLADE);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		channel.sendMessage(":hourglass: Buscando dados...").queue(m -> {
			Beyblade b = MySQL.getBeybladeById(author.getId());

			if (b == null) {
				channel.sendMessage(":x: | Você não possui uma Beyblade.").queue();
				return;
			}

			if (b.hasVoted()) {
				m.editMessage(":x: | Você já votou hoje, você poderá votar novamente em " + b.getVoteTime()).queue();
				return;
			}

			Main.getInfo().getDBL().getVotingMultiplier().whenComplete((mult, e) -> {
				if (mult.isWeekend()) {
					Main.getInfo().getDBL().hasVoted(author.getId()).whenComplete((voted, ex) -> {
						if (voted) {
							m.editMessage("Obrigado " + member.getEffectiveName() + ", seu voto é muito importante para mim, aqui está sua recompensa!\n\n:diamond_shape_with_a_dot_inside: **50 pontos de combate** (Votos valem o dobro em finais de semana!)").queue();
							b.addPoints(50);
							b.setVoteTime(System.currentTimeMillis());
							MySQL.sendBeybladeToDB(b);

						} else {
							m.editMessage("Você ainda não votou! Vote para receber :diamond_shape_with_a_dot_inside: **50 pontos de combate** (Votos valem o dobro em finais de semana!).\nhttps://discordbots.org/bot/572413282653306901\n(Obs: Caso ja tenha votado, aguarde mais alguns minutos e tente novamente usar este comando)").queue();
						}
					});
				} else {
					Main.getInfo().getDBL().hasVoted(author.getId()).whenComplete((voted, ex) -> {
						if (voted) {
							m.editMessage("Obrigado " + member.getEffectiveName() + ", seu voto é muito importante para mim, aqui está sua recompensa!\n\n:diamond_shape_with_a_dot_inside: **25 pontos de combate**").queue();
							b.addPoints(25);
							b.setVoteTime(System.currentTimeMillis());
							MySQL.sendBeybladeToDB(b);

						} else {
							m.editMessage("Você ainda não votou! Vote para receber :diamond_shape_with_a_dot_inside: **25 pontos de combate**.\nhttps://discordbots.org/bot/572413282653306901\n(Obs: Caso ja tenha votado, aguarde mais alguns minutos e tente novamente usar este comando)").queue();
						}
					});
				}
			});
		});
	}
}
