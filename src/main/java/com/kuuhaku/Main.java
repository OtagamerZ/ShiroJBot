/*
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
 *     along with Shiro J Bot.  If not, see <https://www.gnu.org/licenses/>
 */

package com.kuuhaku;

import com.kuuhaku.command.Command;
import com.kuuhaku.controller.MySQL;
import com.kuuhaku.controller.SQLite;
import com.kuuhaku.events.JDAEvents;
import com.kuuhaku.events.ScheduledEvents;
import com.kuuhaku.events.guild.GuildEvents;
import com.kuuhaku.events.guild.GuildUpdateEvents;
import com.kuuhaku.managers.CommandManager;
import com.kuuhaku.model.CustomAnswers;
import com.kuuhaku.model.DataDump;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import javax.persistence.NoResultException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class Main extends ListenerAdapter implements JobListener {

    private static ShiroInfo info;
    private static CommandManager cmdManager;
    private static JDA api;
    private static JobDetail backup;
    private static Scheduler sched;

    public static void main(String[] args) throws Exception {
        info = new ShiroInfo();

        cmdManager = new CommandManager();

        JDA api = new JDABuilder(AccountType.BOT).setToken(info.getToken()).build().awaitReady();
        info.setAPI(api);
        Main.api = api;

        api.addEventListener(new JDAEvents());
        api.addEventListener(new GuildEvents());
        api.addEventListener(new GuildUpdateEvents());

        info.setStartTime(Instant.now().getEpochSecond());

        SQLite.connect();
        if (SQLite.restoreData(MySQL.getData())) System.out.println("Dados recuperados com sucesso!");
        else System.out.println("Erro ao recuperar dados.");

        try {
            if (backup == null) {
                backup = JobBuilder.newJob(ScheduledEvents.class).withIdentity("backup", "1").build();
            }
            Trigger cron = TriggerBuilder.newTrigger().withIdentity("backup", "1").withSchedule(CronScheduleBuilder.cronSchedule("0 0 0/1 ? * * *")).build();
            SchedulerFactory sf = new StdSchedulerFactory();
            try {
                sched = sf.getScheduler();
                sched.scheduleJob(backup, cron);
            } catch (Exception ignore) {
            } finally {
                sched.start();
                System.out.println("Cronograma inicializado com sucesso!");
            }
        } catch (SchedulerException e) {
            System.out.println("Erro ao inicializar cronograma: " + e);
        }

        finishStartUp();
    }

    private static void finishStartUp() {
        api.getPresence().setGame(getRandomGame());
        Main.getInfo().getAPI().getGuilds().forEach(g -> {
            try {
                SQLite.getGuildById(g.getId());
            } catch (NoResultException e) {
                SQLite.addGuildToDB(g);
                System.out.println("Guild adicionada ao banco: " + g.getName());
            }
        });
        Helper.cls();
        System.out.println("Estou pronta!");
        getInfo().setReady(true);
    }

    public static Game getRandomGame() {
        List<Game> games = new ArrayList<Game>() {{
            add(Game.playing("Digite " + info.getDefaultPrefix() + "ajuda para ver meus comandos!"));
            add(Game.streaming("Na conta do meu Nii-chan sem ele saber!", "https://twitch.tv/kuuhaku_otgmz"));
            add(Game.playing("Nico nico nii!!"));
            add(Game.listening(api.getGuilds().size() + " servidores, e isso ainda é só o começo!"));
            add(Game.watching("No Game No Life pela 13ª vez, e ainda não enjoei de ver como eu atuo bem!"));
        }};

        return games.get(Helper.rng(games.size()));
    }

    public static ShiroInfo getInfo() {
        return info;
    }

    public static CommandManager getCommandManager() {
        return cmdManager;
    }

    public static void shutdown() throws SQLException {
        MySQL.dumpData(new DataDump(SQLite.getCADump(), SQLite.getMemberDump(), SQLite.getGuildDump()));
        SQLite.disconnect();
        api.shutdown();
        System.out.println("Fui desligada.");
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public void jobToBeExecuted(JobExecutionContext context) {

    }

    @Override
    public void jobExecutionVetoed(JobExecutionContext context) {

    }

    @Override
    public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
        System.out.println("Programação executada em " + context.getFireTime() + ".\nPróxima execução em " + context.getNextFireTime());
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (Main.getInfo().isReady()) {
            User author = event.getAuthor();
            Member member = event.getMember();
            Message message = event.getMessage();
            MessageChannel channel = message.getChannel();
            Guild guild = message.getGuild();
            String rawMessage = message.getContentRaw();

            String prefix = "";
            try {
                prefix = SQLite.getGuildPrefix(guild.getId());
            } catch (NoResultException ignore) {
            }

            if (Main.getInfo().isNiimode() && author == Main.getInfo().getUserByID(Main.getInfo().getNiiChan())) {
                try {
                    message.delete().queue();
                    channel.sendMessage(rawMessage).queue();
                } catch (InsufficientPermissionException ignore) {
                }
            }

            if (Main.getInfo().getSelfUser().getId().equals(author.getId()) && !Main.getInfo().isNiimode()) return;
            else if (Main.getInfo().getNiiChan().equals(author.getId()) && Main.getInfo().isNiimode()) return;
            if (author.isBot() && !Main.getInfo().getSelfUser().getId().equals(author.getId())) return;

		/*
		if(event.getPrivateChannel()!=null) {
			try {
				Helper.sendPM(author, Helper.formatMessage(Messages.PM_CHANNEL, "help", author));
			} catch (Exception e) {
				DiscordHelper.sendAutoDeleteMessage(channel, YuiHelper.formatMessage(Messages.PM_CHANNEL, "help", author));
			}
			return;
		}

		if(message.getInvites().size()>0 && Helper.getPrivilegeLevel(member) == PrivilegeLevel.USER) {
            message.delete().queue();
            try {
				Helper.sendPM(author, Messages.INVITE_SENT);
            } catch (Exception e) {
				Helper.sendPM(author, ":x: | ");
            }
            return;
        }
		*/

            Helper.battle(event);

            if (message.getContentRaw().equals(Main.getInfo().getSelfUser().getAsMention())) {
                channel.sendMessage("Para obter ajuda sobre como me utilizar use `" + prefix + "ajuda`.").queue();
                return;
            }

            String rawMsgNoPrefix = rawMessage;
            String commandName = "";
            if (rawMessage.contains(prefix)) {
                rawMsgNoPrefix = rawMessage.substring(prefix.length()).trim();
                commandName = rawMsgNoPrefix.split(" ")[0].trim();
            }

            try {
                CustomAnswers ca = SQLite.getCAByTrigger(rawMessage);
                if (!Objects.requireNonNull(ca).isMarkForDelete())
                    Helper.typeMessage(channel, Objects.requireNonNull(ca).getAnswer());
            } catch (NoResultException | NullPointerException ignore) {
            }

            boolean hasArgs = (rawMsgNoPrefix.split(" ").length > 1);
            String[] args = new String[]{};
            if (hasArgs) {
                args = rawMsgNoPrefix.substring(commandName.length()).trim().split(" ");
            }

            boolean found = false;
            for (Command command : Main.getCommandManager().getCommands()) {
                if (command.getName().equalsIgnoreCase(commandName)) {
                    found = true;
                }
                for (String alias : command.getAliases()) {
                    if (alias.equalsIgnoreCase(commandName)) {
                        found = true;
                    }
                }
                if (command.getCategory().isEnabled()) {
                    found = false;
                }

                if (found) {
                    if (!Helper.hasPermission(member, command.getCategory().getPrivilegeLevel())) {
                        channel.sendMessage(":x: | Você não tem permissão para executar este comando!").queue();
                    }
                    command.execute(author, member, rawMsgNoPrefix, args, message, channel, guild, event, prefix);
                    break;
                }
            }

            if (!found) {
                try {
                    com.kuuhaku.model.Member m = SQLite.getMemberById(member.getUser().getId() + member.getGuild().getId());
                    boolean lvlUp = m.addXp();
                    if (lvlUp) {
                        channel.sendMessage(member.getEffectiveName() + " subiu para o nível " + m.getLevel() + ". GGWP! :tada:").queue();
                    }
                    SQLite.saveMemberToDB(m);
                } catch (NoResultException e) {
                    SQLite.addMemberToDB(member);
                }
            }
        }
    }
}
