package com.kuuhaku.controller;

import com.kuuhaku.model.*;
import com.kuuhaku.model.Member;
import com.kuuhaku.utils.ExceedEnums;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;

import javax.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MySQL {

    private static EntityManagerFactory emf;

    private static EntityManager getEntityManager() {
        Map<String, String> props = new HashMap<>();
        props.put("javax.persistence.jdbc.user", System.getenv("DB_LOGIN"));
        props.put("javax.persistence.jdbc.password", System.getenv("DB_PASS"));

        if (emf == null) {
            emf = Persistence.createEntityManagerFactory("shiro_remote", props);
            Helper.logger(MySQL.class).info("✅ | Ligação à base de dados MySQL estabelecida.");
        }

        emf.getCache().evictAll();

        return emf.createEntityManager();
    }

    public static void dumpData(DataDump data) {
        EntityManager em = getEntityManager();
        em.getTransaction().begin();
        data.getCaDump().forEach(em::merge);
        data.getGcDump().forEach(em::merge);

        for (int i = 0; i < data.getmDump().size(); i++) {
            em.merge(data.getmDump().get(i));
            if (i % 20 == 0) {
                em.flush();
                em.clear();
            }
            if (i % 1000 == 0) {
                em.getTransaction().commit();
                em.clear();
                em.getTransaction().begin();
            }
        }

        em.getTransaction().commit();
        em.close();
    }

    @SuppressWarnings("unchecked")
    public static DataDump getData() {
        EntityManager em = getEntityManager();

        Query ca = em.createQuery("SELECT c FROM CustomAnswers c", CustomAnswers.class);
        Query m = em.createQuery("SELECT m FROM Member m", Member.class);
        Query gc = em.createQuery("SELECT g FROM guildConfig g", guildConfig.class);
        DataDump dump = new DataDump(ca.getResultList(), m.getResultList(), gc.getResultList());
        em.close();

        return dump;
    }

    @SuppressWarnings("unchecked")
    public static List<Member> getMembers() {
        EntityManager em = getEntityManager();

        Query q = em.createQuery("SELECT m FROM Member m", Member.class);
        List<Member> members = q.getResultList();
        em.close();

        return members;
    }

	public static void saveMemberToBD(Member m) {
		EntityManager em = getEntityManager();

		em.getTransaction().begin();
		em.merge(m);
		em.getTransaction().commit();

		em.close();
	}

    public static void permaBlock(PermaBlock p) {
        EntityManager em = getEntityManager();

        em.getTransaction().begin();
        em.merge(p);
        em.getTransaction().commit();

        em.close();
    }

    @SuppressWarnings("unchecked")
    public static List<String> blockedList() {
        EntityManager em = getEntityManager();

        try {
            Query q = em.createQuery("SELECT p.id FROM PermaBlock p", String.class);
            List<String> blocks = q.getResultList();
            em.close();
            return blocks;
        } catch (NoResultException e) {
            em.close();
            return new ArrayList<>();
        }
    }

    public static Tags getTagById(String id) {
        EntityManager em = getEntityManager();
        Tags m;

        Query q = em.createQuery("SELECT t FROM Tags t WHERE id = ?1", Tags.class);
        q.setParameter(1, id);
        m = (Tags) q.getSingleResult();

        em.close();

        return m;
    }

    public static int getPartnerAmount() {
        EntityManager em = getEntityManager();
        int size;

        Query q = em.createQuery("SELECT t FROM Tags t WHERE Partner = true", Tags.class);
        size = q.getResultList().size();

        em.close();

        return size;
    }

    public static void addUserTagsToDB(String id) {
        EntityManager em = getEntityManager();

        Tags t = new Tags();
        t.setId(id);

        em.getTransaction().begin();
        em.merge(t);
        em.getTransaction().commit();

        em.close();
    }

    public static void giveTagToxic(String id) {
        EntityManager em = getEntityManager();

        Tags t = getTagById(id);
        t.setToxic(true);

        em.getTransaction().begin();
        em.merge(t);
        em.getTransaction().commit();

        em.close();
    }

    public static void removeTagToxic(String id) {
        EntityManager em = getEntityManager();

        Tags t = getTagById(id);
        t.setToxic(false);

        em.getTransaction().begin();
        em.merge(t);
        em.getTransaction().commit();

        em.close();
    }

    public static void giveTagPartner(String id) {
        EntityManager em = getEntityManager();

        Tags t = getTagById(id);
        t.setPartner(true);

        em.getTransaction().begin();
        em.merge(t);
        em.getTransaction().commit();

        em.close();
    }

    public static void removeTagPartner(String id) {
        EntityManager em = getEntityManager();

        Tags t = getTagById(id);
        t.setPartner(false);

        em.getTransaction().begin();
        em.merge(t);
        em.getTransaction().commit();

        em.close();
    }

    public static void giveTagVerified(String id) {
        EntityManager em = getEntityManager();

        Tags t = getTagById(id);
        t.setVerified(true);

        em.getTransaction().begin();
        em.merge(t);
        em.getTransaction().commit();

        em.close();
    }

    public static void removeTagVerified(String id) {
        EntityManager em = getEntityManager();

        Tags t = getTagById(id);
        t.setVerified(false);

        em.getTransaction().begin();
        em.merge(t);
        em.getTransaction().commit();

        em.close();
    }

    public static void giveTagReader(String id) {
        EntityManager em = getEntityManager();

        Tags t = getTagById(id);
        t.setReader(true);

        em.getTransaction().begin();
        em.merge(t);
        em.getTransaction().commit();

        em.close();
    }

    public static void saveMemberWaifu(Member m, User u) {
        EntityManager em = getEntityManager();

        m.setWaifu(u);

        em.getTransaction().begin();
        em.merge(m);
        em.getTransaction().commit();

        em.close();
    }

    @SuppressWarnings("unchecked")
    public static List<Member> getExceedMembers(ExceedEnums ex) {
        EntityManager em = getEntityManager();

        Query q = em.createQuery("SELECT m FROM Member m WHERE exceed LIKE ?1", Member.class);
        q.setParameter(1, ex.getName());

        List<Member> members = (List<Member>) q.getResultList();
        em.close();

        return members;
    }

    @SuppressWarnings("unchecked")
    public static Exceed getExceed(ExceedEnums ex) {
        EntityManager em = getEntityManager();

        Query q = em.createQuery("SELECT m FROM Member m WHERE exceed LIKE ?1", Member.class);
        q.setParameter(1, ex.getName());

        List<Member> members = (List<Member>) q.getResultList();
        em.close();

        return new Exceed(ex, members.size(), members.stream().mapToLong(Member::getXp).sum());
    }

    public static ExceedEnums findWinner() {
        EntityManager em = getEntityManager();

        Query q = em.createQuery("SELECT exceed FROM Member m WHERE exceed NOT LIKE '' GROUP BY exceed ORDER BY xp DESC", String.class);
        q.setMaxResults(1);

        String winner = (String) q.getSingleResult();
        em.close();

        return ExceedEnums.getByName(winner);
    }

    public static void markWinner(ExceedEnums ex) {
        EntityManager em = getEntityManager();

        MonthWinner m = new MonthWinner();
        m.setExceed(ex.getName());

        em.getTransaction().begin();
        em.merge(m);
        em.getTransaction().commit();

        em.close();
    }

    public static String getWinner() {
        EntityManager em = getEntityManager();

        Query q = em.createQuery("SELECT w FROM MonthWinner w ORDER BY id DESC", MonthWinner.class);
        q.setMaxResults(1);
        try {
            MonthWinner winner = (MonthWinner) q.getSingleResult();
            em.close();

            if (LocalDate.now().isBefore(winner.getExpiry())) {
                return winner.getExceed();
            } else {
                return "none";
            }
        } catch (NoResultException | IndexOutOfBoundsException e) {
            em.close();
            return "none";
        }
    }

    public static PixelCanvas getCanvas() {
        EntityManager em = getEntityManager();

		Query q = em.createQuery("SELECT c FROM PixelCanvas c WHERE shelved = false", PixelCanvas.class);
		q.setMaxResults(1);

        try {
            PixelCanvas p = (PixelCanvas) q.getSingleResult();
            em.close();

            return p;
        } catch (NoResultException e) {
            em.close();

            return new PixelCanvas();
        }
    }

    public static void saveCanvas(PixelCanvas canvas) {
        EntityManager em = getEntityManager();

        em.getTransaction().begin();
        em.merge(canvas);
        em.getTransaction().commit();

        em.close();
    }

    public static void voteUser(Guild guild, User user, User target, boolean vote) {
        EntityManager em = getEntityManager();

        Votes v = new Votes();
        v.addArgs(guild, user, target, vote);

        em.getTransaction().begin();
        em.merge(v);
        em.getTransaction().commit();

        em.close();

        Member m = SQLite.getMemberById(user.getId() + guild.getId());
        m.vote();

        SQLite.saveMemberToDB(m);
    }

    @SuppressWarnings("unchecked")
    public static void getVotes(Guild guild, TextChannel channel) {
        EntityManager em = getEntityManager();

        class result {
            private String votedUserID;
        	private String votedUser;
        	private int score;
		}

        Query q = em.createQuery("SELECT votedUserID, votedUser, SUM(vote) AS votes FROM Votes v WHERE guildID = ?1 AND votes != 0 GROUP BY votedUserID", result.class);
		q.setParameter(1, guild.getId());

        List<result> votes = (List<result>) q.getResultList();
        List<MessageEmbed> pages = new ArrayList<>();
        EmbedBuilder eb = new EmbedBuilder();
        List<MessageEmbed.Field> f = new ArrayList<>();

		votes.forEach(v -> f.add(new MessageEmbed.Field(v.votedUser, "Pontuação: " + v.score, false)));

		for (int i = 0; i < Math.ceil(f.size() / 10f); i++) {
			eb.clear();
			List<MessageEmbed.Field> subF = f.subList(-10 + (10 * (i + 1)), Math.min(10 * (i + 1), f.size()));
			subF.forEach(eb::addField);

			eb.setTitle("Pontuação de usuários deste servidor");
			eb.setColor(Helper.getRandomColor());
			eb.setFooter("Página " + (i + 1) + ". Mostrando " + (-10 + 10 * (i + 1)) + " - " + (Math.min(10 * (i + 1), f.size())) + " usuários.", null);

			pages.add(eb.build());
		}

		channel.sendMessage(pages.get(0)).queue(s -> Helper.paginate(s, pages));
    }

    /*public static void saveCampaigns() throws IOException {
        EntityManager em = getEntityManager();
        ObjectOutputStream oos = new ObjectOutputStream();
        Main.getInfo().getGames().


    }*/
}
