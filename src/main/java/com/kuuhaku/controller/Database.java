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

import com.kuuhaku.model.Beyblade;
import com.kuuhaku.model.CustomAnswers;
import com.kuuhaku.model.Member;
import com.kuuhaku.model.guildConfig;

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
        System.out.println("Dados salvos com sucesso!");
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

    public static void sendAllMembersData(Collection<Member> gc) {
        EntityManager em = getEntityManager();

        em.getTransaction().begin();
        gc.forEach(em::merge);
        em.getTransaction().commit();
        em.close();
        System.out.println("Membros salvos com sucesso!");
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Member> getMembersData() {
        List<Member> lgc;

        try {
            EntityManager em = getEntityManager();
            Query q = em.createQuery("SELECT c FROM Member c", Member.class);
            lgc = q.getResultList();
            System.out.println(lgc);
            em.close();

            return lgc.stream().collect(Collectors.toMap(Member::getId, m -> m));
        } catch (Exception e) {
            System.out.println("Erro ao recuperar membros: " + e);
            return null;
        }
    }

    public static void sendAllCustomAnswers(Collection<CustomAnswers> ca) {
        EntityManager em = getEntityManager();

        em.getTransaction().begin();
        ca.forEach(em::merge);
        em.getTransaction().commit();
        em.close();
        System.out.println("Respostas salvas com sucesso!");
    }

    @SuppressWarnings("unchecked")
    public static List<CustomAnswers> getCustomAnswers() {
        List<CustomAnswers> ca;

        try {
            EntityManager em = getEntityManager();
            Query q = em.createQuery("SELECT c FROM CustomAnswers c", CustomAnswers.class);
            ca = q.getResultList();
            System.out.println(ca);
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
            Query q = em.createQuery("SELECT c FROM Beyblade WHERE id = :id c", Beyblade.class);
            q.setParameter(":id", id);
            bb = (Beyblade) q.getSingleResult();
            em.close();

            return bb;
        } catch (Exception e) {
            System.out.println("Erro ao recuperar beyblade: " + e);
            return null;
        }
    }
}
