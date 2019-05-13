import com.kuuhaku.commands.Admin;
import com.kuuhaku.commands.Embeds;
import com.kuuhaku.commands.Misc;
import com.kuuhaku.commands.Owner;
import com.kuuhaku.controller.Database;
import com.kuuhaku.model.guildConfig;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.ReconnectedEvent;
import net.dv8tion.jda.core.events.ShutdownEvent;
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberLeaveEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Map;

public class Main extends ListenerAdapter implements JobListener, Job {
    private static JDA bot;
    private static User owner;
    private static TextChannel homeLog;
    private static Map<String, guildConfig> gcMap;
    private static JobDetail backup;
    private static Scheduler sched;
    private static final AudioPlayerManager apm = new DefaultAudioPlayerManager();

    private static void initBot() throws LoginException {
        JDABuilder jda = new JDABuilder(AccountType.BOT);
        String token = System.getenv("BOT_TOKEN");
        jda.setToken(token);
        jda.addEventListener(new Main());
        jda.build();
        gcMap = Database.getGuildConfigs();
        AudioSourceManagers.registerRemoteSources(apm);
        try {
            if (backup == null) {
                backup = JobBuilder.newJob(Main.class).withIdentity("backup", "1").build();
            }
            Trigger cron = TriggerBuilder.newTrigger().withIdentity("manha", "1").withSchedule(CronScheduleBuilder.cronSchedule("0 0 0/1 1/1 * ? *")).build();
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
    }

    public static void main(String[] args) {
        try {
            initBot();
            System.out.println("Estou pronta!");
        } catch (Exception e) {
            System.out.println("Erro ao inicializar bot: " + e);
        }
    }

    @Override
    public void execute(JobExecutionContext context) {
        try {
            Database.sendAllGuildConfigs(gcMap.values());
            System.out.println("Guardar configurações no banco de dados...PRONTO!");
            bot.getPresence().setGame(Owner.getRandomGame(bot));
        } catch (Exception e) {
            execute(context);
            System.out.println("Guardar configurações no banco de dados...ERRO!\nErro: " + e);
        }
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public void jobToBeExecuted(JobExecutionContext context) {
        System.out.println("Iniciando backup automático...");
        homeLog.sendMessage("Iniciando backup automático...").queue();
    }

    @Override
    public void jobExecutionVetoed(JobExecutionContext context) {

    }

    @Override
    public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
        OffsetDateTime odt = OffsetDateTime.now();
        System.out.println("Backup executado com sucesso em " + odt.minusHours(3));
        homeLog.sendMessage("Backup executado com sucesso em " + odt.minusHours(3)).queue();
    }

    @Override
    public void onReady(ReadyEvent event) {
        bot = event.getJDA();
        owner = bot.getUserById("350836145921327115");
        homeLog = bot.getGuildById("421495229594730496").getTextChannelById("573861751884349479");
        bot.getPresence().setGame(Owner.getRandomGame(bot));
    }

    @Override
    public void onGuildLeave(GuildLeaveEvent guild) {
        gcMap.remove(guild.getGuild().getId());
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent user) {
        try {
            if (gcMap.get(user.getGuild().getId()).getCanalbv() != null) {
                user.getGuild().getTextChannelById(gcMap.get(user.getGuild().getId()).getCanalbv()).sendMessage(gcMap.get(user.getGuild().getId()).getMsgBoasVindas(user)).queue();
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onGuildMemberLeave(GuildMemberLeaveEvent user) {
        try {
            if (gcMap.get(user.getGuild().getId()).getCanalbv() != null) {
                user.getGuild().getTextChannelById(gcMap.get(user.getGuild().getId()).getCanalbv()).sendMessage(gcMap.get(user.getGuild().getId()).getMsgAdeus(user)).queue();
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onShutdown(ShutdownEvent event) {
        System.out.println("Iniciando sequencia de encerramento...");
        try {
            Database.sendAllGuildConfigs(gcMap.values());
            System.out.println("Guardar configurações no banco de dados...PRONTO!");
            System.out.println("Desligando instância...");
            sched.shutdown();
            System.exit(0);
        } catch (Exception e) {
            System.out.println("Guardar configurações no banco de dados...ERRO!");
            System.out.println("Erro: " + e);
            try {
                initBot();
            } catch (LoginException le) {
                System.out.println("Erro ao inicializar bot: " + le);
            }
        }
    }

    @Override
    public void onReconnect(ReconnectedEvent event) {
        System.out.println("Voltei!");
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent message) {
        try {
            if (message.getAuthor().isBot() || !message.isFromType(ChannelType.TEXT)) return;

            if (message.getMessage().getContentRaw().equals("!init") && gcMap.get(message.getGuild().getId()) == null) {
                guildConfig gct = new guildConfig();
                gct.setGuildId(message.getGuild().getId());
                gcMap.put(message.getGuild().getId(), gct);
                for (int i = 0; i < message.getGuild().getTextChannels().size(); i++) {
                    if (message.getGuild().getTextChannels().get(i).canTalk()) {
                        message.getGuild().getTextChannels().get(i).sendMessage("Seu servidor está prontinho, estarei a partir de agora ouvindo seus comandos!").queue();
                        break;
                    }
                }
            } else if (message.getMessage().getContentRaw().equals("!init") && gcMap.get(message.getGuild().getId()) != null) {
                message.getChannel().sendMessage("As configurações deste servidor ja foram inicializadas!").queue();
            }
            if (gcMap.get(message.getGuild().getId()) != null && message.getTextChannel().canTalk()) {

                System.out.println("Comando recebido de " + message.getAuthor().getName() + "#" + message.getAuthor().getDiscriminator() + " | " + message.getMessage().getContentDisplay());

                String[] cmd = message.getMessage().getContentRaw().split(" ");

                //GERAL--------------------------------------------------------------------------------->

                if (hasPrefix(message, "ping")) {
                    message.getChannel().sendMessage("Pong! :ping_pong: " + bot.getPing() + " ms").queue();
                } else if (hasPrefix(message, "bug")) {
                    owner.openPrivateChannel().queue(channel -> channel.sendMessage(Embeds.bugReport(message, gcMap.get(message.getGuild().getId()).getPrefix())).queue());
                } else if (hasPrefix(message, "uptime")) {
                    Misc.uptime(message);
                } else if (hasPrefix(message, "ajuda")) {
                    Misc.help(message, gcMap.get(message.getGuild().getId()).getPrefix(), owner);
                } else if (hasPrefix(message, "prefixo")) {
                    message.getChannel().sendMessage("Estou atualmente respondendo comandos que começam com __**" + gcMap.get(message.getGuild().getId()).getPrefix() + "**__").queue();
                } else if (hasPrefix(message, "imagem")) {
                    Misc.image(message, cmd);
                } else if (hasPrefix(message, "pergunta")) {
                    Misc.yesNo(message);
                } else if (hasPrefix(message, "escolha")) {
                    Misc.choose(message, cmd[0]);
                } else if (hasPrefix(message, "anime")) {
                    try {
                        Embeds.animeEmbed(message, cmd[0]);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                //DONO--------------------------------------------------------------------------------->

                if (message.getAuthor() == owner) {
                    if (hasPrefix(message, "restart")) {
                        message.getChannel().sendMessage("Sayonara, Nii-chan!").queue();
                        bot.shutdown();
                    } else if (hasPrefix(message, "servers")) {
                        Owner.getServers(bot, message);
                    } else if (hasPrefix(message, "map")) {
                        Owner.getMap(message, gcMap);
                    } else if (hasPrefix(message, "broadcast")) {
                        Owner.broadcast(gcMap, bot, message.getMessage().getContentRaw().replace(gcMap.get(message.getGuild().getId()).getPrefix() + "broadcast ", ""), message.getTextChannel());
                    } else if (hasPrefix(message, "perms")) {
                        Owner.listPerms(bot, message);
                    } else if (hasPrefix(message, "leave")) {
                        Owner.leave(bot, message);
                    }
                }

                //ADMIN--------------------------------------------------------------------------------->

                if (message.getMember().hasPermission(Permission.MANAGE_SERVER)) {
                    if (hasPrefix(message, "definir")) {
                        Admin.config(cmd, message, gcMap.get(message.getGuild().getId()));
                    } else if (hasPrefix(message, "configs")) {
                        Embeds.configsEmbed(message, gcMap.get(message.getGuild().getId()));
                    }
                }
            } else if (message.getTextChannel().canTalk()) {
                message.getChannel().sendMessage("Por favor, digite __**!init**__ para inicializar as configurações da Shiro em seu servidor!").queue();
            }
        } catch (NullPointerException | InsufficientPermissionException e) {
            e.printStackTrace();
        }
    }

    private static boolean hasPrefix(MessageReceivedEvent message, String cmd) {
        return message.getMessage().getContentRaw().split(" ")[0].equalsIgnoreCase(gcMap.get(message.getGuild().getId()).getPrefix() + cmd);
    }
}
