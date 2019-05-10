import com.kuuhaku.commands.Admin;
import com.kuuhaku.commands.Embeds;
import com.kuuhaku.commands.Misc;
import com.kuuhaku.commands.Owner;
import com.kuuhaku.controller.Database;
import com.kuuhaku.model.guildConfig;
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
import java.time.OffsetDateTime;
import java.util.Map;

public class Main extends ListenerAdapter implements JobListener, Job {
    private static JDA bot;
    private static User owner;
    private static TextChannel homeLog;
    private static Map<String, guildConfig> gc;
    private static JobDetail backup;
    private static Scheduler sched;

    private static void initBot() throws LoginException {
        JDABuilder jda = new JDABuilder(AccountType.BOT);
        String token = System.getenv("BOT_TOKEN");
        jda.setToken(token);
        jda.addEventListener(new Main());
        jda.build();
        gc = Database.getConfigs();
        try {
            if (backup == null) {
                backup = JobBuilder.newJob(Main.class).withIdentity("backup", "1").build();
            }
            Trigger cron = TriggerBuilder.newTrigger().withIdentity("manha", "1").withSchedule(CronScheduleBuilder.cronSchedule("0 0 12am,3am,6am,9am,12pm,3pm,6pm,9pm * * ?")).build();
            SchedulerFactory sf = new StdSchedulerFactory();
            try {
                sched = sf.getScheduler();
                sched.scheduleJob(backup, cron);
            } catch (Exception ignore){
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
            Database.sendAllConfigs(gc.values());
            System.out.println("Guardar configurações no banco de dados...PRONTO!");
        } catch (Exception e) {
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
    }

    @Override
    public void onGuildLeave(GuildLeaveEvent guild) {
        gc.remove(guild.getGuild().getId());
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent user) {
        try {
            if (gc.get(user.getGuild().getId()).getCanalbv() != null) {
                user.getGuild().getTextChannelById(gc.get(user.getGuild().getId()).getCanalbv()).sendMessage(gc.get(user.getGuild().getId()).getMsgBoasVindas(user)).queue();
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onGuildMemberLeave(GuildMemberLeaveEvent user) {
        try {
            if (gc.get(user.getGuild().getId()).getCanalbv() != null) {
                user.getGuild().getTextChannelById(gc.get(user.getGuild().getId()).getCanalbv()).sendMessage(gc.get(user.getGuild().getId()).getMsgAdeus(user)).queue();
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onShutdown(ShutdownEvent event) {
        System.out.println("Iniciando sequencia de encerramento...");
        try {
            Database.sendAllConfigs(gc.values());
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

            if (message.getMessage().getContentRaw().equals("!init") && gc.get(message.getGuild().getId()) == null) {
                guildConfig gct = new guildConfig();
                gct.setGuildId(message.getGuild().getId());
                gc.put(message.getGuild().getId(), gct);
                for (int i = 0; i < message.getGuild().getTextChannels().size(); i++) {
                    if (message.getGuild().getTextChannels().get(i).canTalk()) {
                        message.getGuild().getTextChannels().get(i).sendMessage("Seu servidor está prontinho, estarei a partir de agora ouvindo seus comandos!").queue();
                        break;
                    }
                }
            } else if (message.getMessage().getContentRaw().equals("!init") && gc.get(message.getGuild().getId()) != null) {
                message.getChannel().sendMessage("As configurações deste servidor ja foram inicializadas!").queue();
            }
            if (gc.get(message.getGuild().getId()) != null) {
                if (!message.getMessage().getContentRaw().startsWith(gc.get(message.getGuild().getId()).getPrefix()))
                    return;

                System.out.println("Comando recebido de " + message.getAuthor().getName() + "#" + message.getAuthor().getDiscriminator() + " | " + message.getMessage().getContentDisplay());

                String[] cmd = message.getMessage().getContentRaw().split(" ");

                //GERAL--------------------------------------------------------------------------------->

                if (cmd[0].equals(gc.get(message.getGuild().getId()).getPrefix() + "ping")) {
                    message.getChannel().sendMessage("Pong! :ping_pong: " + bot.getPing() + " ms").queue();
                } else if (cmd[0].equals(gc.get(message.getGuild().getId()).getPrefix() + "bug")) {
                    owner.openPrivateChannel().queue(channel -> channel.sendMessage(Embeds.bugReport(message, gc.get(message.getGuild().getId()).getPrefix())).queue());
                } else if (cmd[0].equals(gc.get(message.getGuild().getId()).getPrefix() + "uptime")) {
                    message.getChannel().sendMessage("Hummm...acho que estou acordada a " + Misc.uptime() + " segundos!").queue();
                } else if (cmd[0].equals(gc.get(message.getGuild().getId()).getPrefix() + "ajuda")) {
                    Misc.help(message, gc.get(message.getGuild().getId()).getPrefix(), owner);
                } else if (cmd[0].equals(gc.get(message.getGuild().getId()).getPrefix() + "prefixo")) {
                    message.getChannel().sendMessage("Estou atualmente respondendo comandos que começam com __**" + gc.get(message.getGuild().getId()).getPrefix() + "**__").queue();
                } else if (cmd[0].equals(gc.get(message.getGuild().getId()).getPrefix() + "imagem")) {
                    Misc.image(cmd, message);
                } else if (cmd[0].equals(gc.get(message.getGuild().getId()).getPrefix() + "pergunta")) {
                    message.getChannel().sendMessage(Misc.yesNo()).queue();
                } else if (cmd[0].equals(gc.get(message.getGuild().getId()).getPrefix() + "escolha")) {
                    try {
                        message.getChannel().sendMessage("Eu escolho essa opção: " + Misc.choose(cmd[1].split(";"))).queue();
                    } catch (ArrayIndexOutOfBoundsException e) {
                        message.getChannel().sendMessage("Você não me deu opções, bobo!").queue();
                    }
                }

                //DONO--------------------------------------------------------------------------------->

                if (message.getAuthor() == owner) {
                    if (cmd[0].equals(gc.get(message.getGuild().getId()).getPrefix() + "restart")) {
                        message.getChannel().sendMessage("Sayonara, Nii-chan!").queue();
                        bot.shutdown();
                    } else if (cmd[0].equals(gc.get(message.getGuild().getId()).getPrefix() + "servers")) {
                        message.getChannel().sendMessage("Servidores que participo:\n" + Owner.getServers(bot)).queue();
                    } else if (cmd[0].equals(gc.get(message.getGuild().getId()).getPrefix() + "map")) {
                        Owner.getMap(message, gc);
                    } else if (cmd[0].equals(gc.get(message.getGuild().getId()).getPrefix() + "broadcast")) {
                        Owner.broadcast(gc, bot, String.join("", message.getMessage().getContentRaw().split(gc.get(message.getGuild().getId()).getPrefix() + "broadcast")), message.getTextChannel());
                    } else if (cmd[0].equals(gc.get(message.getGuild().getId()).getPrefix() + "listPerms")) {
                        try {
                            message.getChannel().sendMessage(Owner.listPerms(bot.getGuildById(cmd[1]))).queue();
                        } catch (ArrayIndexOutOfBoundsException e) {
                            message.getChannel().sendMessage("Você esqueceu de me dizer o ID do servidor, Nii-chan!").queue();
                        }
                    } else if (cmd[0].equals(gc.get(message.getGuild().getId()).getPrefix() + "leave")) {
                        try {
                            message.getChannel().sendMessage("Ok, já saí daquele servidor, Nii-chan!").queue();
                            Owner.leave(bot.getGuildById(cmd[1]));
                        } catch (ArrayIndexOutOfBoundsException e) {
                            message.getChannel().sendMessage("Você esqueceu de me dizer o ID do servidor, Nii-chan!").queue();
                        }
                    }
                }

                //ADMIN--------------------------------------------------------------------------------->

                if (message.getMember().hasPermission(Permission.MANAGE_SERVER)) {
                    if (cmd[0].equals(gc.get(message.getGuild().getId()).getPrefix() + "definir")) {
                        Admin.config(cmd, message, gc.get(message.getGuild().getId()));
                    } else if (cmd[0].equals(gc.get(message.getGuild().getId()).getPrefix() + "configs")) {
                        message.getChannel().sendMessage(Embeds.configsEmbed(gc.get(message.getGuild().getId()), message)).queue();
                    }
                }
            } else {
                message.getChannel().sendMessage("Por favor, digite __**!init**__ para inicializar as configurações da Shiro em seu servidor!").queue();
            }
        } catch (NullPointerException | InsufficientPermissionException ignored) {
        }
    }
}
