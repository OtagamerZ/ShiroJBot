/*
 * This file is part of Shiro J Bot.
 *
 *     Shiro J Bot is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Shiro J Bot is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Shiro J Bot.  If not, see <https://www.gnu.org/licenses/>
 */

package com.kuuhaku.controller;

import com.kuuhaku.Main;
import com.kuuhaku.model.CustomAnswers;
import com.kuuhaku.model.Member;
import com.kuuhaku.model.Tags;
import com.kuuhaku.model.guildConfig;
import net.dv8tion.jda.core.entities.Guild;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class SQLite {

    private static Connection con;
    private static EntityManagerFactory emf;

    public static void connect() {
        con = null;

        File DBfile = new File(Main.getInfo().getDBFileName());
        if (!DBfile.exists()) {
            System.out.println("O ficheiro usado como base de dados não foi encontrado. Entre no servidor discord oficial da Shiro para obter ajuda.");
        }

        Map<String, String> props = new HashMap<>();
        props.put("javax.persistence.jdbc.url", "jdbc:sqlite:" + DBfile.getPath());

        if (emf == null) emf = Persistence.createEntityManagerFactory("shiro_local", props);

        emf.getCache().evictAll();

        System.out.println("Ligação à base de dados estabelecida.");
    }

    private static EntityManager getEntityManager() {
        if (emf == null) connect();
        return emf.createEntityManager();
    }

    public static void disconnect() throws SQLException {
        if (con != null) {
            con.close();
            System.out.println("Ligação à base de dados desfeita.");
        }
    }

    public static String getGuildPrefix(String id) {
        EntityManager em = getEntityManager();

        Query q = em.createQuery("SELECT c FROM guildConfig c WHERE guildID = ?1", guildConfig.class);
        q.setParameter(1, id);
        guildConfig gc = (guildConfig) q.getSingleResult();
        String prefix = gc.getPrefix();

        if (prefix == null) prefix = "s!";
        em.close();

        return prefix;
    }

    public static guildConfig getGuildById(String id) {
        EntityManager em = getEntityManager();
        guildConfig gc;

        Query q = em.createQuery("SELECT g FROM guildConfig g WHERE guildID = ?1", guildConfig.class);
        q.setParameter(1, id);
        gc = (guildConfig) q.getSingleResult();

        em.close();

        return gc;
    }

    public static void addGuildToDB(Guild guild) {
        EntityManager em = getEntityManager();

        guildConfig gc = new guildConfig();
        gc.setName(guild.getName());
        gc.setGuildId(guild.getId());
        gc.setOwner(guild.getOwner().getUser().getAsTag());

        em.getTransaction().begin();
        em.merge(gc);
        em.getTransaction().commit();

        em.close();
    }

    public static void removeGuildFromDB(guildConfig gc) {
        EntityManager em = getEntityManager();

        em.getTransaction().begin();
        em.remove(em.getReference(gc.getClass(), gc.getGuildId()));
        em.getTransaction().commit();

        em.close();
    }

    public static CustomAnswers getCAByTrigger(String trigger) {
        EntityManager em = getEntityManager();
        CustomAnswers ca;

        Query q = em.createQuery("SELECT c FROM CustomAnswers c WHERE gatilho LIKE ?1", CustomAnswers.class);
        q.setParameter(1, trigger);
        ca = (CustomAnswers) q.getSingleResult();

        em.close();

        return ca;
    }

    public static void addCAtoDB(Guild g, String trigger, String answer) {
        EntityManager em = getEntityManager();

        CustomAnswers ca = new CustomAnswers();
        ca.setGuildID(g.getId());
        ca.setGatilho(trigger);
        ca.setAnswer(answer);

        em.getTransaction().begin();
        em.merge(ca);
        em.getTransaction().commit();

        em.close();
    }

    public static void removeCAFromDB(CustomAnswers ca) {
        EntityManager em = getEntityManager();

        em.getTransaction().begin();
        em.remove(em.getReference(ca.getClass(), ca.getGuildID()));
        em.getTransaction().commit();

        em.close();
    }

    public static Member getMemberById(String id) {
        EntityManager em = getEntityManager();
        Member m;

        Query q = em.createQuery("SELECT m FROM Member m WHERE id = ?1", Member.class);
        q.setParameter(1, id);
        m = (Member) q.getSingleResult();

        em.close();

        return m;
    }

    public static void addMemberToDB(net.dv8tion.jda.core.entities.Member u) {
        EntityManager em = getEntityManager();

        Member m = new Member();
        m.setId(u.getUser().getId() + u.getGuild().getId());

        em.getTransaction().begin();
        em.merge(m);
        em.getTransaction().commit();

        em.close();
    }

    public static void removeMemberFromDB(Member m) {
        EntityManager em = getEntityManager();

        em.getTransaction().begin();
        em.remove(em.getReference(m.getClass(), m.getId()));
        em.getTransaction().commit();

        em.close();
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

    public static void addUserTagsToDB(net.dv8tion.jda.core.entities.Member u) {
        EntityManager em = getEntityManager();

        Tags t = new Tags();
        t.setId(u.getUser().getId());

        em.getTransaction().begin();
        em.merge(t);
        em.getTransaction().commit();

        em.close();
    }

    public static void giveTagToxic(net.dv8tion.jda.core.entities.Member u) {
        EntityManager em = getEntityManager();

        Tags t = getTagById(u.getUser().getId());
        t.setToxic(true);

        em.getTransaction().begin();
        em.merge(t);
        em.getTransaction().commit();

        em.close();
    }

    public static void removeTagToxic(net.dv8tion.jda.core.entities.Member u) {
        EntityManager em = getEntityManager();

        Tags t = getTagById(u.getUser().getId());
        t.setToxic(false);

        em.getTransaction().begin();
        em.merge(t);
        em.getTransaction().commit();

        em.close();
    }

    public static void giveTagPartner(net.dv8tion.jda.core.entities.Member u) {
        EntityManager em = getEntityManager();

        Tags t = getTagById(u.getUser().getId());
        t.setPartner(true);

        em.getTransaction().begin();
        em.merge(t);
        em.getTransaction().commit();

        em.close();
    }

    public static void removeTagPartner(net.dv8tion.jda.core.entities.Member u) {
        EntityManager em = getEntityManager();

        Tags t = getTagById(u.getUser().getId());
        t.setPartner(false);

        em.getTransaction().begin();
        em.merge(t);
        em.getTransaction().commit();

        em.close();
    }
}