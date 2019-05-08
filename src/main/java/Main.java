import com.kuuhaku.commands.Admin;
import com.kuuhaku.commands.Embeds;
import com.kuuhaku.commands.Misc;
import com.kuuhaku.commands.Owner;
import com.kuuhaku.model.guildConfig;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.ReconnectedEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberLeaveEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import javax.security.auth.login.LoginException;

public class Main extends ListenerAdapter {
    private JDA bot;
    private User owner;
    private guildConfig gc;

    public static void main(String[] args) throws LoginException {
        JDABuilder jda = new JDABuilder(AccountType.BOT);
        String token = "NTcyNzg0MzA1MTM5NDgyNjg2.XNINxw.QngMw67gNsAJFtclOhb73PSAb1I";
        jda.setToken(token);
        jda.addEventListener(new Main());
        jda.build();
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent user) {
        if (gc.getCanalbv() != null) {
            gc.getCanalbv().sendMessage(gc.getMsgBoasVindas(user)).queue();
        }
    }

    @Override
    public void onGuildMemberLeave(GuildMemberLeaveEvent user) {
        if (gc.getCanalbv() != null) {
            gc.getCanalbv().sendMessage(gc.getMsgAdeus(user)).queue();
        }
    }

    @Override
    public void onReady(ReadyEvent event) {
        System.out.println("Estou pronta!");
        bot = event.getJDA();
        owner = bot.getUserById("350836145921327115");
        gc = new guildConfig();
    }

    @Override
    public void onReconnect(ReconnectedEvent event) {
        System.out.println("Voltei!");
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent message) {
        if (message.getAuthor().isBot()) return;

        System.out.println("Comando recebido de " + message.getAuthor().getName() + "#" + message.getAuthor().getDiscriminator() + " | " + message.getMessage().getContentDisplay());

        String[] cmd = message.getMessage().getContentRaw().split(" ");

        //GERAL--------------------------------------------------------------------------------->

        if (cmd[0].equals(gc.getPrefix() + "ping")) {
            message.getChannel().sendMessage("Pong! :ping_pong: " + bot.getPing() + " ms").queue();
        } else if (cmd[0].equals(gc.getPrefix() + "bug")) {
            owner.openPrivateChannel().queue(channel -> channel.sendMessage(Embeds.bugReport(message, gc.getPrefix())).queue());
        } else if (cmd[0].equals(gc.getPrefix() + "uptime")) {
            message.getChannel().sendMessage("Hummm...acho que estou acordada a ").queue();
        } else if (cmd[0].equals(gc.getPrefix() + "ajuda")) {
            Misc.help(message, gc.getPrefix(), owner);
        } else if (cmd[0].equals(gc.getPrefix() + "prefixo")) {
            message.getChannel().sendMessage("Estou atualmente respondendo comandos que come\u00e7am com __**" + gc.getPrefix() + "**__").queue();
        } else if (cmd[0].equals(gc.getPrefix() + "imagem")) {
            Misc.image(cmd, message);
        } else if (cmd[0].equals(gc.getPrefix() + "pergunta")) {
            message.getChannel().sendMessage(Misc.yesNo()).queue();
        } else if (cmd[0].equals(gc.getPrefix() + "escolha")) {
            try {
                message.getChannel().sendMessage(Misc.choose(cmd[1].split(";"))).queue();
            } catch (ArrayIndexOutOfBoundsException e) {
                message.getChannel().sendMessage("Voc\u00ea n\u00e3o me deu op\u00e7\u00f5es, bobo!").queue();
            }
        }

        //DONO--------------------------------------------------------------------------------->

        if (message.getAuthor() == owner) {
            if (cmd[0].equals(gc.getPrefix() + "kill")) {
                message.getChannel().sendMessage("Sayonara, Nii-chan!").queue();
                System.exit(0);
            } else if (cmd[0].equals(gc.getPrefix() + "servers")) {
                message.getChannel().sendMessage("Servidores que participo:\n" + Owner.getServers(bot)).queue();
            } else if (cmd[0].equals(gc.getPrefix() + "broadcast")) {
                Owner.broadcast(bot, String.join("", message.getMessage().getContentRaw().split(gc.getPrefix() + "broadcast")), message.getTextChannel());
            }
        }

        //ADMIN--------------------------------------------------------------------------------->

        if (message.getMember().hasPermission(Permission.MANAGE_SERVER)) {
            if (cmd[0].equals(gc.getPrefix() + "definir")) {
                Admin.config(cmd, message, gc);
            } else if (cmd[0].equals(gc.getPrefix() + "configs")) {
                message.getChannel().sendMessage(Embeds.configsEmbed(gc)).queue();
            }
        }
    }
}
