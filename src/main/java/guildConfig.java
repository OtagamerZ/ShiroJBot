import net.dv8tion.jda.core.entities.TextChannel;

import javax.persistence.Id;

public class guildConfig {
    @Id
    private int id;
    private String prefix, msgBoasVindas;
    private TextChannel canalbv, canalav;

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

    public String getMsgBoasVindas() {
        return msgBoasVindas;
    }

    public void setMsgBoasVindas(String msgBoasVindas) {
        this.msgBoasVindas = msgBoasVindas;
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
