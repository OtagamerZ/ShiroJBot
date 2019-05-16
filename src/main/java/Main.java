import com.kuuhaku.commands.*;
import com.kuuhaku.controller.Database;
import com.kuuhaku.model.Member;
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
import org.json.JSONObject;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

public class Main extends ListenerAdapter implements JobListener, Job {
    private static JDA bot;
    private static User owner;
    private static TextChannel homeLog;
    private static Map<String, guildConfig> gcMap = new HashMap<>();
    private static Map<String, Member> memberMap = new HashMap<>();
    private static JobDetail backup;
    private static Scheduler sched;
    private static boolean ready = false;

    private static void initBot() throws LoginException {
        JDABuilder jda = new JDABuilder(AccountType.BOT);
        String token = System.getenv("BOT_TOKEN");
        jda.setToken(token);
        jda.addEventListener(new Main());
        jda.build();
        gcMap = Database.getGuildConfigs();
        memberMap = Database.getMembersData();
        try {
            if (backup == null) {
                backup = JobBuilder.newJob(Main.class).withIdentity("backup", "1").build();
            }
            Trigger cron = TriggerBuilder.newTrigger().withIdentity("backup", "1").withSchedule(CronScheduleBuilder.cronSchedule("0 0 0/1 1/1 * ? *")).build();
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
            ready = true;
        } catch (Exception e) {
            System.out.println("Erro ao inicializar bot: " + e);
        }
    }

    @Override
    public void execute(JobExecutionContext context) {
        try {
            Database.sendAllGuildConfigs(gcMap.values());
            Database.sendAllMembersData(memberMap.values());
            System.out.println("Guardar configurações no banco de dados...PRONTO!");
            bot.getPresence().setGame(Owner.getRandomGame(bot));
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
                Embeds.welcomeEmbed(user, gcMap.get(user.getGuild().getId()).getMsgBoasVindas(), user.getGuild().getTextChannelById(gcMap.get(user.getGuild().getId()).getCanalbv()));
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onGuildMemberLeave(GuildMemberLeaveEvent user) {
        try {
            if (gcMap.get(user.getGuild().getId()).getCanalbv() != null) {
                Embeds.byeEmbed(user, gcMap.get(user.getGuild().getId()).getMsgAdeus(), user.getGuild().getTextChannelById(gcMap.get(user.getGuild().getId()).getCanalbv()));
                if (memberMap.get(user.getUser().getId() + user.getGuild().getId()) != null)
                    memberMap.remove(user.getUser().getId() + user.getGuild().getId());
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onShutdown(ShutdownEvent event) {
        System.out.println("Iniciando sequencia de encerramento...");
        try {
            Database.sendAllGuildConfigs(gcMap.values());
            Database.sendAllMembersData(memberMap.values());
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
        if (ready) {
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
                    if (memberMap.get(message.getAuthor().getId() + message.getGuild().getId()) != null && !message.getMessage().getContentRaw().startsWith(gcMap.get(message.getGuild().getId()).getPrefix())) {
                        boolean lvlUp;
                        lvlUp = memberMap.get(message.getAuthor().getId() + message.getGuild().getId()).addXp();
                        if (lvlUp) {
                            if (gcMap.get(message.getGuild().getId()).getLvlNotif()) message.getChannel().sendMessage(message.getAuthor().getAsMention() + " subiu para o level " + memberMap.get(message.getAuthor().getId() + message.getGuild().getId()).getLevel() + ". GGWP!! :tada:").queue();
                            if (gcMap.get(message.getGuild().getId()).getCargoslvl().containsKey(Integer.toString(memberMap.get(message.getAuthor().getId() + message.getGuild().getId()).getLevel()))) {
                                Member member = memberMap.get(message.getAuthor().getId() + message.getGuild().getId());
                                String roleID = (String) gcMap.get(message.getGuild().getId()).getCargoslvl().get(Integer.toString(member.getLevel()));

                                message.getGuild().getController().addRolesToMember(message.getMember(), message.getGuild().getRoleById(roleID)).queue();
                            }
                        }
                    }
                    if (message.getMessage().getContentRaw().startsWith(gcMap.get(message.getGuild().getId()).getPrefix())) {

                        if (memberMap.get(message.getAuthor().getId() + message.getGuild().getId()) == null) {
                            Member m = new Member();
                            m.setId(message.getAuthor().getId() + message.getGuild().getId());
                            memberMap.put(message.getAuthor().getId() + message.getGuild().getId(), m);
                        }

                        System.out.println("Comando recebido de " + message.getAuthor().getName() + "#" + message.getAuthor().getDiscriminator() + " | " + message.getGuild().getName() + " -> " + message.getMessage().getContentDisplay());

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
                        } else if (hasPrefix(message, "xp")) {
                            try {
                                Embeds.levelEmbed(message, memberMap.get(message.getAuthor().getId() + message.getGuild().getId()), gcMap.get(message.getGuild().getId()).getPrefix());
                            } catch (IOException e) {
                                System.out.println(e.toString());
                            }
                        } else if (hasPrefix(message, "conquista")) {
                            if (message.getGuild().getId().equals("421495229594730496")) {
                                Misc.badges(message);
                            } else {
                                message.getChannel().sendMessage(":x: Você está no servidor errado, este comando é exclusivo do servidor OtagamerZ!").queue();
                            }
                        } else if (hasPrefix(message, "conquistas")) {
                            if (message.getGuild().getId().equals("421495229594730496")) {
                                try {
                                    Embeds.myBadgesEmbed(message, memberMap.get(message.getAuthor().getId() + message.getGuild().getId()));
                                } catch (IOException e) {
                                    System.out.println(e.toString());
                                }
                            } else {
                                message.getChannel().sendMessage(":x: Você está no servidor errado, este comando é exclusivo do servidor OtagamerZ!").queue();
                            }
                        } else if (hasPrefix(message, "abraçar")) {
                            if (message.getMessage().getMentionedUsers().size() != 0) {
                                Reactions.hug(bot, message);
                            } else {
                                message.getChannel().sendMessage(":x: Você precisa mencionar um usuário!").queue();
                            }
                        } else if (hasPrefix(message, "meee")) {
                            Reactions.facedesk(message);
                        } else if (hasPrefix(message, "vemca")) {
                            if (message.getMessage().getMentionedUsers().size() != 0) {
                                Reactions.cuddle(bot, message);
                            } else {
                                message.getChannel().sendMessage(":x: Você precisa mencionar um usuário!").queue();
                            }
                        } else if (hasPrefix(message, "sqn")) {
                            Reactions.nope(message);
                        } else if (hasPrefix(message, "corre")) {
                            Reactions.run(message);
                        } else if (hasPrefix(message, "tapa")) {
                            if (message.getMessage().getMentionedUsers().size() != 0) {
                                Reactions.slap(bot, message);
                            } else {
                                message.getChannel().sendMessage(":x: Você precisa mencionar um usuário!").queue();
                            }
                        } else if (hasPrefix(message, "chega")) {
                            if (message.getMessage().getMentionedUsers().size() != 0) {
                                Reactions.smash(bot, message);
                            } else {
                                message.getChannel().sendMessage(":x: Você precisa mencionar um usuário!").queue();
                            }
                        } else if (hasPrefix(message, "encarar")) {
                            if (message.getMessage().getMentionedUsers().size() != 0) {
                                Reactions.stare(bot, message);
                            } else {
                                message.getChannel().sendMessage(":x: Você precisa mencionar um usuário!").queue();
                            }
                        } else if (hasPrefix(message, "embed")) {
                            try {
                                Embeds.makeEmbed(message, message.getMessage().getContentRaw().replace(gcMap.get(message.getGuild().getId()).getPrefix() + "embed ", ""));
                            } catch (Exception e) {
                                message.getChannel().sendMessage("Ops, me parece que o link imagem não está correto, veja bem se incluiu tudo!").queue();
                            }
                        }

                        //DONO--------------------------------------------------------------------------------->

                        if (message.getAuthor() == owner) {
                            if (hasPrefix(message, "restart")) {
                                message.getChannel().sendMessage("Sayonara, Nii-chan!").queue();
                                bot.shutdown();
                            } else if (hasPrefix(message, "servers")) {
                                Owner.getServers(bot, message);
                            } else if (hasPrefix(message, "gmap")) {
                                Owner.getGuildMap(message, gcMap);
                            } else if (hasPrefix(message, "mmap")) {
                                Owner.getMemberMap(message, memberMap);
                            } else if (hasPrefix(message, "broadcast")) {
                                Owner.broadcast(gcMap, bot, message.getMessage().getContentRaw().replace(gcMap.get(message.getGuild().getId()).getPrefix() + "broadcast ", ""), message.getTextChannel());
                            } else if (hasPrefix(message, "perms")) {
                                Owner.listPerms(bot, message);
                            } else if (hasPrefix(message, "leave")) {
                                Owner.leave(bot, message);
                            } else if (hasPrefix(message, "dar")) {
                                try {
                                    memberMap.get(message.getMessage().getMentionedUsers().get(0).getId() + message.getGuild().getId()).giveBadge(cmd[2]);
                                    message.getChannel().sendMessage("Parabéns, " + message.getMessage().getMentionedUsers().get(0).getAsMention() + " completou a conquista Nº " + cmd[2]).queue();
                                } catch (Exception e) {
                                    message.getChannel().sendMessage(":x: Ué, não estou conseguindo marcar a conquista como completa. Tenha certeza de digitar o comando neste formato: " + gcMap.get(message.getGuild().getId()).getPrefix() + "dar [MEMBRO] [Nº]").queue();
                                }
                            } else if (hasPrefix(message, "tirar")) {
                                try {
                                    memberMap.get(message.getMessage().getMentionedUsers().get(0).getId() + message.getGuild().getId()).removeBadge(cmd[2]);
                                    message.getChannel().sendMessage("Meeee, " + message.getMessage().getMentionedUsers().get(0).getAsMention() + " teve a conquista Nº " + cmd[2] + " retirada de sua posse!").queue();
                                } catch (Exception e) {
                                    message.getChannel().sendMessage(":x: Ué, não estou conseguindo marcar a conquista como incompleta. Tenha certeza de digitar o comando neste formato: " + gcMap.get(message.getGuild().getId()).getPrefix() + "tirar [MEMBRO] [Nº]").queue();
                                }
                            }
                        }

                        //ADMIN--------------------------------------------------------------------------------->
                        if (message.getMember().hasPermission(Permission.MANAGE_CHANNEL)) {
                            if (hasPrefix(message, "definir")) {
                                Admin.config(cmd, message, gcMap.get(message.getGuild().getId()));
                            } else if (hasPrefix(message, "configs")) {
                                Embeds.configsEmbed(message, gcMap.get(message.getGuild().getId()));
                            } else if (hasPrefix(message, "punir")) {
                                if (message.getMessage().getMentionedUsers() != null) {
                                    memberMap.get(message.getMessage().getMentionedUsers().get(0).getId() + message.getGuild().getId()).resetXp();
                                    message.getChannel().sendMessage(message.getMessage().getMentionedUsers().get(0).getAsMention() + " teve seus XP e leveis resetados!").queue();
                                } else {
                                    message.getChannel().sendMessage(":x: Você precisa me dizer de quem devo resetar o XP.").queue();
                                }
                            } else if (hasPrefix(message, "alertar")) {
                                if (message.getMessage().getMentionedUsers() != null && cmd.length >= 3) {
                                    Admin.addWarn(message, message.getMessage().getContentRaw().replace(gcMap.get(message.getGuild().getId()).getPrefix(), ""), memberMap);
                                } else {
                                    message.getChannel().sendMessage(":x: Você precisa mencionar um usuário e dizer o motivo do alerta.").queue();
                                }
                            } else if (hasPrefix(message, "perdoar")) {
                                if (message.getMessage().getMentionedUsers() != null && cmd.length >= 3) {
                                    Admin.takeWarn(message, memberMap);
                                } else {
                                    message.getChannel().sendMessage(":x: Você precisa mencionar um usuário e dizer o Nº do alerta a ser removido.").queue();
                                }
                            } else if (hasPrefix(message, "remover cargolvl")) {
                                if (!message.getMessage().getContentRaw().replace(gcMap.get(message.getGuild().getId()).getPrefix() + "remover cargolvl", "").trim().equals("")) {
                                    Object cargos = gcMap.get(message.getGuild().getId()).getCargoslvl().remove(message.getMessage().getContentRaw().replace(gcMap.get(message.getGuild().getId()).getPrefix() + "remover cargolvl", "").trim());
                                    gcMap.get(message.getGuild().getId()).setCargoslvl((JSONObject) cargos);
                                } else {
                                    message.getChannel().sendMessage("Opa, algo deu errado, lembre-se de especificar apenas o level.").queue();
                                }
                            } else if (hasPrefix(message, "lvlnotif")) {
                                if (gcMap.get(message.getGuild().getId()).getLvlNotif()) {
                                    message.getChannel().sendMessage("Não irei mais avisar quando um membro passar de nível!").queue();
                                    gcMap.get(message.getGuild().getId()).setLvlNotif(false);
                                } else {
                                    message.getChannel().sendMessage("Agora irei avisar quando um membro passar de nível!").queue();
                                    gcMap.get(message.getGuild().getId()).setLvlNotif(true);
                                }
                            }
                        }
                    }
                } else if (message.getTextChannel().canTalk()) {
                    message.getChannel().sendMessage("Por favor, digite __**!init**__ para inicializar as configurações da Shiro em seu servidor!").queue();
                }
            } catch (NullPointerException | InsufficientPermissionException | IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static boolean hasPrefix(MessageReceivedEvent message, String cmd) {
        return message.getMessage().getMentionedUsers().contains(bot.getSelfUser()) || message.getMessage().getContentRaw().split(" ")[0].equalsIgnoreCase(gcMap.get(message.getGuild().getId()).getPrefix() + cmd);
    }
}
