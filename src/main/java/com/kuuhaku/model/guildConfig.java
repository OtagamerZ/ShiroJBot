package com.kuuhaku.model;

import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberLeaveEvent;

import javax.persistence.Id;

public class guildConfig {
    @Id
    private int id;
    private String prefix = "!", msgBoasVindas = "Seja bem-vindo(a) %user%!", msgAdeus = "Ahh...%user% deixou este servidor!";
    private TextChannel canalbv = null, canalav = null;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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


    public TextChannel getCanalbv() {
        return canalbv;
    }

    public void setCanalbv(TextChannel canalbv) {
        this.canalbv = canalbv;
    }

    public TextChannel getCanalav() {
        return canalav;
    }

    public void setCanalav(TextChannel canalav) {
        this.canalav = canalav;
    }
}
