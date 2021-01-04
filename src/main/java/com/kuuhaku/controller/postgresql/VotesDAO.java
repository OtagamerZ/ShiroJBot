/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2020  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.controller.postgresql;

import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.Page;
import com.github.ygimenez.type.PageType;
import com.kuuhaku.controller.sqlite.MemberDAO;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.DevRating;
import com.kuuhaku.model.persistent.Member;
import com.kuuhaku.model.persistent.Votes;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class VotesDAO {
	public static void voteUser(Guild guild, User user, User target, boolean vote) {
		EntityManager em = Manager.getEntityManager();

		Votes v = new Votes();
		v.addArgs(guild, user, target, vote);

		em.getTransaction().begin();
		em.merge(v);
		em.getTransaction().commit();

		em.close();

		List<Member> m = MemberDAO.getMemberByMid(user.getId());
		for (Member member : m) {
			member.vote();
			MemberDAO.updateMemberConfigs(member);
		}
	}

	@SuppressWarnings("unchecked")
	public static void getVotes(Guild guild, TextChannel channel) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT v FROM Votes v WHERE guildID = ?1 AND vote != 0", Votes.class);
		q.setParameter(1, guild.getId());

		class result {
			private final String name;
			private int votes;

			private result(String name, int votes) {
				this.name = name;
				this.votes = votes;
			}

			private int getVotes() {
				return votes;
			}
		}

		List<Votes> votes = q.getResultList();
		HashMap<String, result> voteMap = new HashMap<>();

		for (Votes vote : votes) {
			String user = vote.getVotedUserID();
			if (voteMap.containsKey(user)) {
				voteMap.get(user).votes += vote.getVote();
			} else {
				voteMap.put(vote.getVotedUserID(), new result(vote.getVotedUser(), vote.getVote()));
			}
		}

		List<result> results = new ArrayList<>(voteMap.values());
		results.sort(Comparator.comparing(result::getVotes));

		List<Page> pages = new ArrayList<>();
		EmbedBuilder eb = new ColorlessEmbedBuilder();
		List<MessageEmbed.Field> f = new ArrayList<>();

		for (result v : results) {
			f.add(new MessageEmbed.Field(v.name, "Pontuação: " + v.votes, false));
		}

		for (int i = 0; i < Math.ceil(f.size() / 10f); i++) {
			eb.clear();
			List<MessageEmbed.Field> subF = f.subList(-10 + (10 * (i + 1)), Math.min(10 * (i + 1), f.size()));
			for (MessageEmbed.Field field : subF) {
				eb.addField(field);
			}

			eb.setTitle("Pontuação de usuários deste servidor");
			eb.setFooter("Página " + (i + 1) + ". Mostrando " + (-10 + 10 * (i + 1)) + " - " + (Math.min(10 * (i + 1), f.size())) + " usuários.", null);

			pages.add(new Page(PageType.TEXT, eb.build()));
		}

		if (pages.size() > 0)
			channel.sendMessage((MessageEmbed) pages.get(0).getContent()).queue(s -> Pages.paginate(s, pages, 1, TimeUnit.MINUTES, 5));
		else channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_no-votes")).queue();
	}

	public static DevRating getRating(String id) {
		EntityManager em = Manager.getEntityManager();

		try {
			return Helper.getOr(em.find(DevRating.class, id), new DevRating(id));
		} finally {
			em.close();
		}
	}

	public static void saveRating(DevRating dev) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.merge(dev);
		em.getTransaction().commit();

		em.close();
	}

	public static void evaluate(DevRating dev) {
		EntityManager em = Manager.getEntityManager();

		dev.addVote();
		em.getTransaction().begin();
		em.merge(dev);
		em.getTransaction().commit();

		em.close();
	}
}
