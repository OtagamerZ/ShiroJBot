import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import javax.security.auth.login.LoginException;
import java.time.format.DateTimeFormatter;

public class Main extends ListenerAdapter {
    private JDA bot;
    private String prefix;

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
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent message) {
        if (message.getAuthor().isBot()) return;

        System.out.println("Comando recebido de " + message.getAuthor().getName() + "#" + message.getAuthor().getDiscriminator() + " | " + message.getMessage().getContentDisplay());

        String[] cmd = message.getMessage().getContentRaw().split(" ");

        if (cmd[0].equals(prefix + "ping")) {
            message.getChannel().sendMessage("Pong! :ping_pong: " + bot.getPing() + " ms").queue();
        } else if (cmd[0].equals(prefix + "bug")) {
            bot.getUserById("350836145921327115").openPrivateChannel().queue(channel -> channel.
                    sendMessage(message.getMessage().getCreationTime().minusHours(3).format(DateTimeFormatter.ofPattern("dd/MM/yyyy (HH:mm)")) + " | " + message.getAuthor().getName() + "#" + message.getAuthor().getDiscriminator() + "(" +
                            message.getGuild().getName() + ")" + " enviou um bug: `" + String.join(" ", message.getMessage().getContentRaw().split(prefix + "bug ")).trim() + "`").queue());
        } else if (cmd[0].equals(prefix + "uptime")) {
            message.getChannel().sendMessage("Hummm...acho que estou acordada a ");
        }
    }
}
