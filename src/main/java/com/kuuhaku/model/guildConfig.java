package com.kuuhaku.model;

import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberLeaveEvent;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.io.*;
import java.util.Map;

@Entity
public class guildConfig {
    @Id
    private String guildID;
    private String prefix = "!";
    private String msgBoasVindas = "Seja bem-vindo(a) %user%!";
    private String msgAdeus = "Ahh...%user% deixou este servidor!";
    private String canalbv = null, canalav = null, canalmsc = null, cargowarn = null;
    private byte[] members;

    public guildConfig() {

    }

    public String getGuildId() {
        return guildID;
    }

    public void setGuildId(String id) {
        this.guildID = id;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getMsgBoasVindas(GuildMemberJoinEvent newUser) {
        return newUser != null ? msgBoasVindas.replace("%user%", newUser.getMember().getAsMention()) : msgBoasVindas;
    }

    public void setMsgBoasVindas(String msgBoasVindas) {
        this.msgBoasVindas = msgBoasVindas;
    }


    public String getMsgAdeus(GuildMemberLeaveEvent oldUser) {
        return oldUser != null ? msgAdeus.replace("%user%", oldUser.getMember().getAsMention()) : msgAdeus;
    }

    public void setMsgAdeus(String msgAdeus) {
        this.msgAdeus = msgAdeus;
    }


    public String getCanalbv() {
        return canalbv;
    }

    public void setCanalbv(String canalbv) {
        this.canalbv = canalbv;
    }

    public String getCanalav() {
        return canalav;
    }

    public void setCanalav(String canalav) {
        this.canalav = canalav;
    }

    public String getCanalmsc() {
        return canalmsc;
    }

    public void setCanalmsc(String canalmsc) {
        this.canalmsc = canalmsc;
    }

    public String getCargowarn() {
        return cargowarn;
    }

    public void setCargowarn(String cargowarn) {
        this.cargowarn = cargowarn;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Member> getMembers() throws IOException, ClassNotFoundException {
        ByteArrayInputStream bais = new ByteArrayInputStream(members);
        Map<String, Member> map = null;

        try (ObjectInputStream ois = new ObjectInputStream(bais)) {
            map = (Map<String, Member>) ois.readObject();
            return map;
        } finally {
            setMembers(map);
        }
    }

    private void setMembers(Map<String, Member> members) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(members);
            this.members = baos.toByteArray();
        }
    }
}
