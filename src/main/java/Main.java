import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.ReconnectedEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import javax.security.auth.login.LoginException;
import java.time.format.DateTimeFormatter;

public class Main extends ListenerAdapter {
    private JDA bot;
    private String prefix;
    private User owner;
    private TextChannel canalbv, canalav;

    private void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    private void setCanalbv(TextChannel canalbv) {
        this.canalbv = canalbv;
    }

    private void setCanalav(TextChannel canalav) {
        this.canalav = canalav;
    }

    public static void main(String[] args) throws LoginException {
        JDABuilder jda = new JDABuilder(AccountType.BOT);
        String token = "NTcyNzg0MzA1MTM5NDgyNjg2.XNINxw.QngMw67gNsAJFtclOhb73PSAb1I";
        jda.setToken(token);
        jda.addEventListener(new Main());
        jda.build();
    }

    @Override
    public void onReady(ReadyEvent event) {
        System.out.println("Estou pronta!");
        this.bot = event.getJDA();
        this.prefix = "!";
        this.owner = bot.getUserById("350836145921327115");
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

        if (cmd[0].equals(prefix + "ping")) {
            message.getChannel().sendMessage("Pong! :ping_pong: " + bot.getPing() + " ms").queue();
        } else if (cmd[0].equals(prefix + "bug")) {
            owner.openPrivateChannel().queue(channel -> channel.
                    sendMessage(message.getMessage().getCreationTime().minusHours(3).format(DateTimeFormatter.ofPattern("dd/MM/yyyy (HH:mm)")) + " | " + message.getAuthor().getName() + "#" + message.getAuthor().getDiscriminator() + "(" +
                            message.getGuild().getName() + ")" + " enviou um bug: `" + String.join(" ", message.getMessage().getContentRaw().split(prefix + "bug ")).trim() + "`").queue());
        } else if (cmd[0].equals(prefix + "uptime")) {
            message.getChannel().sendMessage("Hummm...acho que estou acordada a ").queue();
        } else if (cmd[0].equals(prefix + "ajuda")) {
            message.getAuthor().openPrivateChannel().queue(channel -> channel.sendMessage("__**Precisa de ajuda? Aqui estou eu!**__\n").queue());
            message.getAuthor().openPrivateChannel().queue(channel -> channel.sendMessage(Embeds.helpEmbed(prefix)).queue());
            message.getAuthor().openPrivateChannel().queue(channel -> channel.sendMessage("Precisa de mais ajuda? Fale com meu Nii-chan " + owner.getAsMention() + " ou venha para nosso servidor de suporte: https://discord.gg/HpuF3Vr").queue());
        } else if (cmd[0].equals(prefix + "prefixo")) {
            message.getChannel().sendMessage("Estou atualmente respondendo comandos que come\u00e7am com __**" + prefix + "**__").queue();
        }

        //OWNER--------------------------------------------------------------------------------->

        if (message.getAuthor() == owner) {
            if (cmd[0].equals(prefix + "kill")) {
                message.getChannel().sendMessage("Sayonara, Nii-chan!").queue();
                System.exit(0);
            } else if (cmd[0].equals(prefix + "servers")) {
                message.getChannel().sendMessage("Servidores que participo:\n" + Owner.getServers(bot)).queue();
            } else if (cmd[0].equals(prefix + "broadcast")) {
                Owner.broadcast(bot, String.join("", message.getMessage().getContentRaw().split(prefix + "broadcast")), message.getTextChannel());
            }
        }

        ////ADMIN--------------------------------------------------------------------------------->

        if (message.getMember().hasPermission(Permission.MANAGE_SERVER)) {
            if (cmd[0].equals(prefix + "definir")) {
                try {
                    switch (cmd[1]) {
                        case "canalbv":
                            try {
                                setCanalbv(message.getMessage().getMentionedChannels().get(0));
                                message.getChannel().sendMessage("Canal de boas-vindas trocado para " + canalbv.getAsMention()).queue();
                            } catch (ArrayIndexOutOfBoundsException e) {
                                message.getChannel().sendMessage("E qual canal devo usar para mensagens de boas-vindas? N\u00falo n\u00e3o \u00e9 um canal v\u00e1lido!").queue();
                            }
                            break;
                        case "canalav":
                            try {
                                setCanalav(message.getMessage().getMentionedChannels().get(0));
                                message.getChannel().sendMessage("Canal de avisos trocado para " + canalav.getAsMention()).queue();
                            } catch (ArrayIndexOutOfBoundsException e) {
                                message.getChannel().sendMessage("E qual canal devo usar para mensagens de aviso? N\u00falo n\u00e3o \u00e9 um canal v\u00e1lido!").queue();
                            }
                            break;
                        case "prefixo":
                            try {
                                setPrefix(cmd[2]);
                                message.getChannel().sendMessage("Prefixo trocado para __**" + prefix + "**__").queue();
                            } catch (ArrayIndexOutOfBoundsException e) {
                                message.getChannel().sendMessage("Faltou me dizer o prefixo, bobo!").queue();
                            }
                            break;
                        default:
                            message.getChannel().sendMessage("N\u00e3o conhe\u00e7o esse comando, certeza que digitou corretamente?").queue();
                            break;
                    }
                } catch (ArrayIndexOutOfBoundsException e) {
                    message.getChannel().sendMessage("Voc\u00ea precisa me dizer o qu\u00ea devo definir").queue();
                }
            } else if (cmd[0].equals(prefix + "configs")) {
                //PLACEHOLDER
            }
        }
    }
}
