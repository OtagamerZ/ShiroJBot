package com.kuuhaku.controller;

import com.kuuhaku.model.CustomAnswers;
import com.kuuhaku.model.Member;
import com.kuuhaku.model.guildConfig;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MySQL {

    private static EntityManagerFactory emf;

    private static EntityManager getEntityManager() {
        Map<String, String> props = new HashMap<>();
        props.put("javax.persistence.jdbc.user", "epiz_23971060");
        props.put("javax.persistence.jdbc.password", "Yago1234!!khk");

        if (emf == null) emf = Persistence.createEntityManagerFactory("shiro_remote", props);

        emf.getCache().evictAll();

        return emf.createEntityManager();
    }

    public static void dumpData(List<CustomAnswers> ca, List<Member> m, List<guildConfig> gc) {
        EntityManager em = getEntityManager();
    }
}
