import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

public class Main extends ListenerAdapter {
    public static void main(String[]args) {
        JDABuilder bot = new JDABuilder(AccountType.BOT).setToken(System.getenv("BOT_TOKEN"));
        bot.addEventListener();

    }

    @Override
    public void onGuildJoin(GuildJoinEvent event) {

    }

    @Override
    public void onGuildLeave(GuildLeaveEvent event) {

    }

    @Override
    public void onMessageReceived(MessageReceivedEvent m) {

    }
}
