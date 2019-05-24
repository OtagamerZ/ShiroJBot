/*
 * Copyright (C) 2019 Yago Garcia Sanches Gimenez / KuuHaKu
 *
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
 *     along with Shiro J Bot.  If not, see https://www.gnu.org/licenses/
 */

package com.kuuhaku.controller;

import com.kuuhaku.model.*;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Database {
    private static EntityManagerFactory emf;


    private static EntityManager getEntityManager() {
        Map<String, String> props = new HashMap<>();
        props.put("javax.persistence.jdbc.user", System.getenv("DB_LOGIN"));
        props.put("javax.persistence.jdbc.password", System.getenv("DB_PASS"));

        if (emf == null) emf = Persistence.createEntityManagerFactory("shiro", props);

        emf.getCache().evictAll();

        return emf.createEntityManager();
    }

    public static void sendAllGuildConfigs(Collection<guildConfig> gc) {
        EntityManager em = getEntityManager();

        em.getTransaction().begin();
        gc.forEach(em::merge);
        em.getTransaction().commit();
        em.close();
    }

    @SuppressWarnings("unchecked")
    public static Map<String, guildConfig> getGuildConfigs() {
        List<guildConfig> lgc;

        try {
            EntityManager em = getEntityManager();
            Query q = em.createQuery("SELECT c FROM guildConfig c", guildConfig.class);
            lgc = q.getResultList();
            em.close();

            return lgc.stream().collect(Collectors.toMap(guildConfig::getGuildId, g -> g));
        } catch (Exception e) {
            System.out.println("Erro ao recuperar configurações: " + e);
            return null;
        }
    }

    public static Member getMemberById(String t) {
        Member m;
        EntityManager em = getEntityManager();
        Query q = em.createQuery("SELECT c FROM Member c WHERE id LIKE ?1", Member.class);
        q.setParameter(1, t);
        m = (Member) q.getSingleResult();
        em.close();

        return m;
    }

    public static void sendMember(Member m) {
        EntityManager em = getEntityManager();
        
        em.getTransaction().begin();
        em.merge(m);
        em.getTransaction().commit();
        em.close();
    }

    public static void deleteMember(Member m) {
        EntityManager em = getEntityManager();
        em.getTransaction().begin();
        em.remove(em.getReference(m.getClass(), m.getId()));
        em.getTransaction().commit();
        em.close();
    }

    public static void sendCustomAnswer(CustomAnswers ca) {
        EntityManager em = getEntityManager();

        em.getTransaction().begin();
        em.merge(ca);
        em.getTransaction().commit();
        em.close();
    }

    @SuppressWarnings("unchecked")
    public static List<CustomAnswers> getCustomAnswer(String t) {
        List<CustomAnswers> ca;

        try {
            EntityManager em = getEntityManager();
            Query q = em.createQuery("SELECT c FROM CustomAnswers c WHERE gatilho LIKE ?1", CustomAnswers.class);
            q.setParameter(1, t);
            ca = q.getResultList();
            em.close();

            return ca;
        } catch (Exception e) {
            System.out.println("Erro ao recuperar resposta: " + e);
            return null;
        }
    }

    public static CustomAnswers getCustomAnswerById(Long t) {
        CustomAnswers ca;
        EntityManager em = getEntityManager();
        Query q = em.createQuery("SELECT c FROM CustomAnswers c WHERE id = ?1", CustomAnswers.class);
        q.setParameter(1, t);
        ca = (CustomAnswers) q.getSingleResult();
        em.close();

        return ca;
    }

    public static void deleteCustomAnswer(CustomAnswers ca) {
        EntityManager em = getEntityManager();
        em.getTransaction().begin();
        em.remove(em.getReference(ca.getClass(), ca.getId()));
        em.getTransaction().commit();
        em.close();
    }

    @SuppressWarnings("unchecked")
    public static List<CustomAnswers> getAllCustomAnswers() {
        List<CustomAnswers> ca;

        try {
            EntityManager em = getEntityManager();
            Query q = em.createQuery("SELECT c FROM CustomAnswers c", CustomAnswers.class);
            ca = q.getResultList();
            em.close();

            return ca;
        } catch (Exception e) {
            System.out.println("Erro ao recuperar respostas: " + e);
            return null;
        }
    }

    public static Beyblade getBeyblade(String id) {
        Beyblade bb;

        try {
            EntityManager em = getEntityManager();
            Query q = em.createQuery("SELECT c FROM Beyblade c WHERE id = ?1", Beyblade.class);
            q.setParameter(1, id);
            bb = (Beyblade) q.getSingleResult();
            em.close();

            bb.setS(Special.getSpecial(bb.getSpecial()));
            return bb;
        } catch (Exception e) {
            System.out.println("Erro ao recuperar beyblade: " + e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static List<Beyblade> getBeybladeList() {
        List<Beyblade> bb;

        try {
            EntityManager em = getEntityManager();
            Query q = em.createQuery("SELECT c FROM Beyblade c", Beyblade.class);
            bb = (List<Beyblade>) q.getResultList();
            em.close();

            return bb;
        } catch (Exception e) {
            System.out.println("Erro ao recuperar beyblades: " + e);
            return null;
        }
    }

    public static void sendBeyblade(Beyblade bb) {
        EntityManager em = getEntityManager();

        em.getTransaction().begin();
        em.merge(bb);
        em.getTransaction().commit();
        em.close();
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Tags> getTags() {
        List<Tags> t;

        try {
            EntityManager em = getEntityManager();
            Query q = em.createQuery("SELECT t FROM Tags t", Tags.class);
            t = q.getResultList();
            em.close();

            return t.stream().collect(Collectors.toMap(Tags::getId, v -> v));
        } catch (Exception e) {
            System.out.println("Erro ao recuperar tags: " + e);
            return null;
        }
    }

    public static void sendAllTags(Collection<Tags> t) {
        EntityManager em = getEntityManager();

        em.getTransaction().begin();
        t.forEach(em::merge);
        em.getTransaction().commit();
        em.close();
    }
}
