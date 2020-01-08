package com.kuuhaku.controller.mysql;

import com.kuuhaku.Main;
import com.kuuhaku.controller.sqlite.MemberDAO;
import com.kuuhaku.method.Pages;
import com.kuuhaku.model.Member;
import com.kuuhaku.model.Page;
import com.kuuhaku.type.PageType;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;

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

        com.kuuhaku.model.Votes v = new com.kuuhaku.model.Votes();
        v.addArgs(guild, user, target, vote);

        em.getTransaction().begin();
        em.merge(v);
        em.getTransaction().commit();

        em.close();

        Member m = MemberDAO.getMemberById(user.getId() + guild.getId());
        m.vote();

        MemberDAO.updateMemberConfigs(m);
    }

	@SuppressWarnings("unchecked")
    public static void getVotes(Guild guild, TextChannel channel) {
        EntityManager em = Manager.getEntityManager();

        Query q = em.createQuery("SELECT v FROM Votes v WHERE guildID = ?1 AND vote != 0", com.kuuhaku.model.Votes.class);
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

        List<com.kuuhaku.model.Votes> votes = (List<com.kuuhaku.model.Votes>) q.getResultList();
        HashMap<String, result> voteMap = new HashMap<>();

        votes.forEach(v -> {
        	String user = v.getVotedUserID();
        	if (voteMap.containsKey(user)) {
				voteMap.get(user).votes += v.getVote();
			} else {
        		voteMap.put(v.getVotedUserID(), new result(v.getVotedUser(), v.getVote()));
			}
		});

		List<result> results = new ArrayList<>(voteMap.values());
		results.sort(Comparator.comparing(result::getVotes));

        List<Page> pages = new ArrayList<>();
        EmbedBuilder eb = new EmbedBuilder();
        List<MessageEmbed.Field> f = new ArrayList<>();

		results.forEach(v -> f.add(new MessageEmbed.Field(v.name, "Pontuação: " + v.votes, false)));

		for (int i = 0; i < Math.ceil(f.size() / 10f); i++) {
			eb.clear();
			List<MessageEmbed.Field> subF = f.subList(-10 + (10 * (i + 1)), Math.min(10 * (i + 1), f.size()));
			subF.forEach(eb::addField);

			eb.setTitle("Pontuação de usuários deste servidor");
			eb.setColor(Helper.getRandomColor());
			eb.setFooter("Página " + (i + 1) + ". Mostrando " + (-10 + 10 * (i + 1)) + " - " + (Math.min(10 * (i + 1), f.size())) + " usuários.", null);

			pages.add(new Page(PageType.TEXT, eb.build()));
		}

		channel.sendMessage((Message) pages.get(0).getContent()).queue(s -> Pages.paginate(Main.getInfo().getAPI(), s, pages, 60, TimeUnit.SECONDS));
    }
}
