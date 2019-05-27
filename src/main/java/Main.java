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

import com.kuuhaku.commands.*;
import com.kuuhaku.controller.Database;
import com.kuuhaku.controller.Tradutor;
import com.kuuhaku.model.Member;
import com.kuuhaku.model.*;
import net.dv8tion.jda.core.*;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.ReconnectedEvent;
import net.dv8tion.jda.core.events.ShutdownEvent;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberLeaveEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.core.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.json.JSONObject;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import redis.clients.jedis.Jedis;

import javax.persistence.NoResultException;
import javax.security.auth.login.LoginException;
import java.awt.*;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class Main extends ListenerAdapter implements JobListener, Job {
    private static JDA bot;
    private static User owner;
    private static Jedis db;
    private static TextChannel homeLog;
    private static JobDetail backup;
    private static Scheduler sched;
    private static boolean ready = false;
    private static Map<Long, DuelData> duels = new HashMap<>();
    private static List<DuelData> accDuels = new ArrayList<>();

    private static void initBot() throws LoginException {
        JDABuilder jda = new JDABuilder(AccountType.BOT);
        String token = System.getenv("BOT_TOKEN");
        jda.setToken(token);
        jda.addEventListener(new Main());
        jda.build();
        try {
            if (backup == null) {
                backup = JobBuilder.newJob(Main.class).withIdentity("backup", "1").build();
            }
            Trigger cron = TriggerBuilder.newTrigger().withIdentity("backup", "1").withSchedule(CronScheduleBuilder.cronSchedule("0 0 0/3 ? * * *")).build();
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
        try {
            List<CustomAnswers> ca = Database.getAllCustomAnswers();
            db = new Jedis("localhost");
            System.out.println("Conectado ao servidor - " + db.ping());
            if (ca != null) {
                ca.forEach(c -> db.sadd(c.getGuildID(), "{\"id\":\"" + c.getId() + "\", \"trigger\":\"" + c.getGatilho() + "\", \"answer\":\"" + c.getAnswer() + "\"}"));
            }
        } catch (Exception e) {
            e.printStackTrace();
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
        bot.getPresence().setGame(Owner.getRandomGame(bot));
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
    public void onGuildJoin(GuildJoinEvent event) {
        guildConfig gc = new guildConfig();
        gc.setGuildId(event.getGuild().getId());
        Database.sendGuildConfig(gc);
        try {
            Misc.sendPM(event.getGuild().getOwner().getUser(), "Obrigada por me adicionar ao seu servidor!");
        } catch (Exception err) {
            TextChannel dch = event.getGuild().getDefaultChannel();
            if (dch != null) {
                if (dch.canTalk()) {
                    dch.sendMessage("Obrigada por me adicionar ao seu servidor!").queue();
                }
            }
        }
    }

    @Override
    public void onGuildLeave(GuildLeaveEvent guild) {
        Database.deleteGuild(Database.getGuildConfigById(guild.getGuild().getId()));
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent user) {
        guildConfig gc = null;
        try {
            gc = Database.getGuildConfigById(user.getGuild().getId());
        } catch (NoResultException e) {
            guildConfig newGc = new guildConfig();
            newGc.setGuildId(user.getGuild().getId());
            Database.sendGuildConfig(newGc);
        }
        try {
            if (Objects.requireNonNull(gc).getCanalbv() != null) {
                Embeds.welcomeEmbed(user, gc.getMsgBoasVindas(), user.getGuild().getTextChannelById(gc.getCanalbv()));
                Map<String, Object> roles = gc.getCargoNew();
                List<Role> list = new ArrayList<>();
                roles.values().forEach(r -> list.add(user.getGuild().getRoleById(r.toString())));
                if (gc.getCargoNew().size() > 0)
                    user.getGuild().getController().addRolesToMember(user.getMember(), list).queue();
            }
        } catch (NullPointerException ignore) {
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onGuildMemberLeave(GuildMemberLeaveEvent user) {
        guildConfig gc = null;
        try {
            gc = Database.getGuildConfigById(user.getGuild().getId());
        } catch (NoResultException e) {
            guildConfig newGc = new guildConfig();
            newGc.setGuildId(user.getGuild().getId());
            Database.sendGuildConfig(newGc);
        }
        try {
            if (Objects.requireNonNull(gc).getCanaladeus() != null) {
                Embeds.byeEmbed(user, gc.getMsgAdeus(), user.getGuild().getTextChannelById(gc.getCanaladeus()));
                if (Database.getMemberById(user.getUser().getId() + user.getGuild().getId()) != null) {
                    Member m = Database.getMemberById(user.getUser().getId() + user.getGuild().getId());
                    Database.deleteMember(m);
                }
            }
        } catch (NullPointerException ignore) {
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        if (event.getUser().isBot()) return;
        User user = event.getUser();
        Message message = event.getChannel().getMessageById(event.getMessageId()).complete();
        List<User> ment = message.getMentionedUsers();
        if (duels.containsKey(event.getMessageIdLong())) {
            accDuels.add(duels.get(event.getMessageIdLong()));
            duels.remove(event.getMessageIdLong());
            event.getChannel().sendMessage("O duelo começou!\nUsem `atacar` para atacar, `defender` para defender ou `especial` para tentar utilizar seu poder especial de alinhamento.\n\n**O desafiante começa primeiro!**").queue();
        }
        if (event.getReactionEmote().getName().equals("\ud83d\udc4d")) {
            if (message.getReactions().get(0).getCount() >= 5) message.pin().queue();
        } else if (event.getReactionEmote().getName().equals("\ud83d\udc4e")) {
            if (message.getReactions().get(0).getCount() >= 5) message.delete().queue();
        }
        if (ment.size() > 1) {
            User target = ment.get(0);
            if (ment.get(1) == user && event.getReactionEmote().getName().equals("\u21aa")) {
                System.out.println("Nova reação na mensagem " + event.getMessageId() + " por " + event.getUser().getName() + " | " + event.getGuild().getName());
                MessageBuilder msg = new MessageBuilder();
                msg.setContent(ment.get(1).getAsMention());
                try {
                    if (message.getContentRaw().contains("abraçou")) {
                        Reactions.hug(bot, msg.build(), user, message.getTextChannel(), true, target);
                    } else if (message.getContentRaw().contains("deu um tapa em")) {
                        Reactions.slap(bot, msg.build(), user, message.getTextChannel(), true, target);
                    } else if (message.getContentRaw().contains("destruiu")) {
                        Reactions.smash(bot, msg.build(), user, message.getTextChannel(), true, target);
                    } else if (message.getContentRaw().contains("beijou")) {
                        Reactions.kiss(bot, msg.build(), user, message.getTextChannel(), true, target);
                    } else if (message.getContentRaw().contains("encarou")) {
                        Reactions.stare(bot, user, message.getTextChannel(), true, target);
                    } else if (message.getContentRaw().contains("dançando") || message.getContentRaw().contains("dança")) {
                        Reactions.dance(message, true);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onShutdown(ShutdownEvent event) {
        System.out.println("Iniciando sequencia de encerramento...");
        try {
            System.out.println("Guardar configurações no banco de dados...PRONTO!");
            System.out.println("Desligando instância...");
            sched.shutdown();
            System.exit(0);
        } catch (Exception e) {
            JDABuilder jda = new JDABuilder(AccountType.BOT);
            String token = System.getenv("BOT_TOKEN");
            jda.setToken(token);
            jda.addEventListener(new Main());
            try {
                jda.build();
            } catch (LoginException ex) {
                ex.printStackTrace();
            }
            System.out.println("Guardar configurações no banco de dados...ERRO!");
            System.out.println("Erro: " + e);
        }
    }

    @Override
    public void onReconnect(ReconnectedEvent event) {
        System.out.println("Voltei!");
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent message) {
        guildConfig gc = null;
        try {
            gc = Database.getGuildConfigById(message.getGuild().getId());
        } catch (NoResultException e) {
            guildConfig newGc = new guildConfig();
            newGc.setGuildId(message.getGuild().getId());
            Database.sendGuildConfig(newGc);
        }
        if (ready) {
            if (accDuels.stream().anyMatch(d -> d.getP1() == message.getAuthor() || d.getP2() == message.getAuthor())) {
                Arena.battle(accDuels, message);
            }
            try {
                if (message.getChannel().getId().equals(Objects.requireNonNull(gc).getCanalsug()) && !message.getMessage().getAuthor().isBot() && !message.getMember().hasPermission(Permission.MANAGE_CHANNEL)) {
                    message.getMessage().addReaction("\ud83d\udc4d").queue();
                    message.getMessage().addReaction("\ud83d\udc4e").queue();
                }
            } catch (
                    NullPointerException ignore) {
            }
            try {
                if (message.getAuthor().isBot() || !message.isFromType(ChannelType.TEXT)) return;

                if (gc != null && message.getTextChannel().canTalk()) {
                    try {
                        Set<String> ca = db.smembers(message.getGuild().getId());
                        List<JSONObject> caList = new ArrayList<>();
                        ca.forEach(c -> caList.add(new JSONObject(c)));
                        caList.removeIf(c -> !c.getString("trigger").equalsIgnoreCase(message.getMessage().getContentRaw()));
                        String answer = caList.get(new Random().nextInt(caList.size())).getString("answer");
                        message.getChannel().sendTyping().queue(tm -> message.getChannel().sendMessage(answer).queueAfter(answer.length() * 25, TimeUnit.MILLISECONDS));
                    } catch (Exception ignore) {
                    }
                    try {
                        if (Database.getMemberById(message.getAuthor().getId() + message.getGuild().getId()) != null && !message.getMessage().getContentRaw().startsWith(gc.getPrefix())) {
                            Member m = Database.getMemberById(message.getAuthor().getId() + message.getGuild().getId());
                            boolean lvlUp = m.addXp();
                            if (lvlUp) {
                                TextChannel tc = null;
                                try {
                                    tc = message.getGuild().getTextChannelById(gc.getCanallvl());
                                } catch (IllegalArgumentException ignore) {
                                }
                                if (tc == null) {
                                    if (gc.getLvlNotif())
                                        message.getChannel().sendTyping().queue(tm -> message.getChannel().sendMessage(message.getAuthor().getAsMention() + " subiu para o level " + Database.getMemberById(message.getAuthor().getId() + message.getGuild().getId()).getLevel() + ". GGWP!! :tada:").queue());
                                    if (gc.getCargoslvl().containsKey(Integer.toString(Database.getMemberById(message.getAuthor().getId() + message.getGuild().getId()).getLevel()))) {
                                        Member member = Database.getMemberById(message.getAuthor().getId() + message.getGuild().getId());
                                        String roleID = (String) gc.getCargoslvl().get(Integer.toString(member.getLevel()));

                                        message.getGuild().getController().addRolesToMember(message.getMember(), message.getGuild().getRoleById(roleID)).queue();
                                        message.getChannel().sendTyping().queue(tm -> message.getChannel().sendMessage(message.getAuthor().getAsMention() + " ganhou o cargo " + message.getGuild().getRoleById(roleID).getAsMention() + ". Parabéns!").queue());
                                    }
                                } else {
                                    if (gc.getLvlNotif()) {
                                        TextChannel finalTc = tc;
                                        tc.sendTyping().queue(tm -> finalTc.sendMessage(message.getAuthor().getAsMention() + " subiu para o level " + Database.getMemberById(message.getAuthor().getId() + message.getGuild().getId()).getLevel() + ". GGWP!! :tada:").queue());
                                    }
                                    if (gc.getCargoslvl().containsKey(Integer.toString(Database.getMemberById(message.getAuthor().getId() + message.getGuild().getId()).getLevel()))) {
                                        Member member = Database.getMemberById(message.getAuthor().getId() + message.getGuild().getId());
                                        String roleID = (String) gc.getCargoslvl().get(Integer.toString(member.getLevel()));

                                        message.getGuild().getController().addRolesToMember(message.getMember(), message.getGuild().getRoleById(roleID)).queue();
                                        tc.sendMessage(message.getAuthor().getAsMention() + " ganhou o cargo " + message.getGuild().getRoleById(roleID).getAsMention() + ". Parabéns!").queue();
                                    }
                                }
                            }
                            Database.sendMember(m);
                        }
                    } catch (NoResultException e) {
                        Member m = new Member();
                        m.setId(message.getAuthor().getId() + message.getGuild().getId());
                        Database.sendMember(m);
                    }
                    if (message.getMessage().getContentRaw().startsWith(gc.getPrefix())) {
                        //COMMANDS--------------------------------------------------------------------------------->
                        if (new Random().nextInt(1000) > 950) {
                            message.getChannel().sendTyping().queue(tm -> message.getChannel().sendMessage("Opa, está gostando de me utilizar em seu servidor? Caso sim, se puder votar me ajudaria **MUITO** a me tornar cada vez mais popular e ser chamada para mais servidores!\n https://discordbots.org/bot/572413282653306901").queue());
                        }
                        if (Database.getMemberById(message.getAuthor().getId() + message.getGuild().getId()) == null) {
                            Member m = new Member();
                            m.setId(message.getAuthor().getId() + message.getGuild().getId());
                            Database.sendMember(m);
                        }

                        System.out.println("Comando recebido de " + message.getAuthor().getName() + "#" + message.getAuthor().getDiscriminator() + " | " + message.getGuild().getName() + " -> " + message.getMessage().getContentDisplay());

                        String[] cmd = message.getMessage().getContentRaw().split(" ");

                        //GERAL--------------------------------------------------------------------------------->

                        if (hasPrefix(message, "ping")) {
                            message.getChannel().sendTyping().queue(tm -> message.getChannel().sendMessage("Pong! :ping_pong: " + bot.getPing() + " ms").queue());
                        } else if (hasPrefix(message, "bug")) {
                            Misc.sendPM(owner, Embeds.bugReport(message, gc.getPrefix()));
                        } else if (hasPrefix(message, "uptime")) {
                            Misc.uptime(message);
                        } else if (hasPrefix(message, "ajuda")) {
                            Misc.help(message, gc.getPrefix(), owner);
                        } else if (hasPrefix(message, "avatar")) {
                            if (message.getMessage().getMentionedUsers().size() > 0) {
                                message.getChannel().sendTyping().queue(tm -> message.getChannel().sendMessage("Avatar de " + message.getMessage().getMentionedUsers().get(0).getAsMention() + ":\n" + message.getMessage().getMentionedUsers().get(0).getAvatarUrl()).queue());
                            } else if (message.getMessage().getContentRaw().replace(gc.getPrefix() + "avatar", "").trim().equals("guild")) {
                                message.getChannel().sendTyping().queue(tm -> message.getChannel().sendMessage("Avatar do server:\n" + message.getMessage().getGuild().getIconUrl()).queue());
                            } else {
                                message.getChannel().sendTyping().queue(tm -> message.getChannel().sendMessage("Você precisa mencionar alguém!").queue());
                            }
                        } else if (hasPrefix(message, "imagem")) {
                            Misc.image(message, cmd);
                        } else if (hasPrefix(message, "pergunta")) {
                            Misc.yesNo(message);
                        } else if (hasPrefix(message, "traduza")) {
                            if (cmd.length > 2) {
                                if (cmd[1].contains(">")) {
                                    String from = cmd[1].split(">")[0];
                                    String to = cmd[1].split(">")[1];
                                    message.getChannel().sendTyping().queue(tm -> {
                                        try {
                                            message.getChannel().sendMessage(Tradutor.translate(from, to, message.getMessage().getContentRaw().replace(cmd[0], "").replace(cmd[1], ""))).queue();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    });
                                } else {
                                    message.getChannel().sendTyping().queue(tm -> message.getChannel().sendMessage("Você precisa especificar de qual pra qual idioma devo traduzir (`de`>`para`)").queue());
                                }
                            } else {
                                message.getChannel().sendTyping().queue(tm -> message.getChannel().sendMessage("Você não me disse praticamente nada!").queue());
                            }
                        } else if (hasPrefix(message, "escolha")) {
                            Misc.choose(message, cmd[0]);
                        } else if (hasPrefix(message, "anime")) {
                            try {
                                Embeds.animeEmbed(message, cmd[0]);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else if (hasPrefix(message, "perfil")) {
                            Embeds.levelEmbed(message, Database.getMemberById(message.getAuthor().getId() + message.getGuild().getId()), Database.getTags());
                        } else if (hasPrefix(message, "fundo")) {
                            Misc.setBg(message, cmd, Database.getMemberById(message.getAuthor().getId() + message.getGuild().getId()));
                        } else {
                            final Consumer<Void> otgmzCmd = tm -> message.getChannel().sendMessage(":x: Você está no servidor errado, este comando é exclusivo do servidor OtagamerZ!").queue();
                            if (hasPrefix(message, "conquista")) {
                                if (message.getGuild().getId().equals("421495229594730496")) {
                                    Misc.badges(message);
                                } else {
                                    message.getChannel().sendTyping().queue(otgmzCmd);
                                }
                            } else if (hasPrefix(message, "conquistas")) {
                                if (message.getGuild().getId().equals("421495229594730496")) {
                                    try {
                                        Embeds.myBadgesEmbed(message, Database.getMemberById(message.getAuthor().getId() + message.getGuild().getId()));
                                    } catch (IOException e) {
                                        System.out.println(e.toString());
                                    }
                                } else {
                                    message.getChannel().sendTyping().queue(otgmzCmd);
                                }
                            } else {
                                final Consumer<Void> noUser = tm -> message.getChannel().sendMessage(":x: Você precisa mencionar um usuário!").queue();
                                if (hasPrefix(message, "vemca")) {
                                    if (message.getMessage().getMentionedUsers().size() != 0) {
                                        Reactions.hug(bot, message.getMessage(), message.getMessage().getMentionedUsers().get(0), message.getTextChannel(), false, message.getAuthor());
                                    } else {
                                        message.getChannel().sendTyping().queue(noUser);
                                    }
                                } else if (hasPrefix(message, "meee")) {
                                    Reactions.facedesk(message.getMessage());
                                } else if (hasPrefix(message, "dançar")) {
                                    Reactions.dance(message.getMessage(), false);
                                } else if (hasPrefix(message, "sqn")) {
                                    Reactions.nope(message.getMessage());
                                } else if (hasPrefix(message, "corre")) {
                                    Reactions.run(message.getMessage());
                                } else if (hasPrefix(message, "baka")) {
                                    Reactions.blush(message.getMessage());
                                } else if (hasPrefix(message, "kkk")) {
                                    Reactions.laugh(message.getMessage());
                                } else if (hasPrefix(message, "triste")) {
                                    Reactions.sad(message.getMessage());
                                } else if (hasPrefix(message, "tapa")) {
                                    if (message.getMessage().getMentionedUsers().size() != 0) {
                                        Reactions.slap(bot, message.getMessage(), message.getMessage().getMentionedUsers().get(0), message.getTextChannel(), false, message.getAuthor());
                                    } else {
                                        message.getChannel().sendTyping().queue(noUser);
                                    }
                                } else if (hasPrefix(message, "cafunhe")) {
                                    if (message.getMessage().getMentionedUsers().size() != 0) {
                                        Reactions.pat(message.getMessage().getMentionedUsers().get(0), message.getTextChannel(), false, message.getAuthor());
                                    } else {
                                        message.getChannel().sendTyping().queue(noUser);
                                    }
                                } else if (hasPrefix(message, "chega")) {
                                    if (message.getMessage().getMentionedUsers().size() != 0) {
                                        Reactions.smash(bot, message.getMessage(), message.getMessage().getMentionedUsers().get(0), message.getTextChannel(), false, message.getAuthor());
                                    } else {
                                        message.getChannel().sendTyping().queue(noUser);
                                    }
                                } else if (hasPrefix(message, "encarar")) {
                                    if (message.getMessage().getMentionedUsers().size() != 0) {
                                        Reactions.stare(bot, message.getMessage().getMentionedUsers().get(0), message.getTextChannel(), false, message.getAuthor());
                                    } else {
                                        message.getChannel().sendTyping().queue(noUser);
                                    }
                                } else if (hasPrefix(message, "beijar")) {
                                    if (message.getMessage().getMentionedUsers().size() != 0) {
                                        Reactions.kiss(bot, message.getMessage(), message.getMessage().getMentionedUsers().get(0), message.getTextChannel(), false, message.getAuthor());
                                    } else {
                                        message.getChannel().sendTyping().queue(noUser);
                                    }
                                } else if (hasPrefix(message, "embed")) {
                                    try {
                                        Embeds.makeEmbed(message, message.getMessage().getContentRaw().replace(gc.getPrefix() + "embed ", ""));
                                    } catch (Exception e) {
                                        message.getChannel().sendTyping().queue(tm -> message.getChannel().sendMessage("Ops, me parece que o link imagem não está correto, veja bem se incluiu tudo!").queue());
                                    }
                                }
                            }
                        }

                        //DONO--------------------------------------------------------------------------------->

                        if (message.getAuthor() == owner) {
                            if (hasPrefix(message, "restart")) {
                                message.getChannel().sendTyping().queue(tm -> message.getChannel().sendMessage("Sayonara, Nii-chan!").queue());
                                bot.shutdown();
                            } else if (hasPrefix(message, "servers")) {
                                Owner.getServers(bot, message);
                            } else if (hasPrefix(message, "perms")) {
                                Owner.listPerms(bot, message);
                            } else if (hasPrefix(message, "leave")) {
                                Owner.leave(bot, message);
                            } else if (hasPrefix(message, "dar")) {
                                try {
                                    Database.getMemberById(message.getMessage().getMentionedUsers().get(0).getId() + message.getGuild().getId()).giveBadge(cmd[2]);
                                    message.getChannel().sendTyping().queue(tm -> message.getChannel().sendMessage("Parabéns, " + message.getMessage().getMentionedUsers().get(0).getAsMention() + " completou a conquista Nº " + cmd[2]).queue());
                                } catch (Exception e) {
                                    guildConfig finalGc = gc;
                                    message.getChannel().sendTyping().queue(tm -> message.getChannel().sendMessage(":x: Ué, não estou conseguindo marcar a conquista como completa. Tenha certeza de digitar o comando neste formato: " + finalGc.getPrefix() + "dar [MEMBRO] [Nº]").queue());
                                }
                            } else if (hasPrefix(message, "tirar")) {
                                try {
                                    Database.getMemberById(message.getMessage().getMentionedUsers().get(0).getId() + message.getGuild().getId()).removeBadge(cmd[2]);
                                    message.getChannel().sendTyping().queue(tm -> message.getChannel().sendMessage("Meeee, " + message.getMessage().getMentionedUsers().get(0).getAsMention() + " teve a conquista Nº " + cmd[2] + " retirada de sua posse!").queue());
                                } catch (Exception e) {
                                    guildConfig finalGc1 = gc;
                                    message.getChannel().sendTyping().queue(tm -> message.getChannel().sendMessage(":x: Ué, não estou conseguindo marcar a conquista como incompleta. Tenha certeza de digitar o comando neste formato: " + finalGc1.getPrefix() + "tirar [MEMBRO] [Nº]").queue());
                                }
                            } else {
                                final Consumer<Void> bakaNiiChan = tm -> message.getChannel().sendMessage("Nii-chan bobo, você precisa mencionar um usuário!").queue();
                                if (hasPrefix(message, "giveStaff")) {
                                    if (message.getMessage().getMentionedUsers() != null) {
                                        Tags t = Objects.requireNonNull(Database.getTags()).getOrDefault(message.getMessage().getMentionedUsers().get(0).getId(), null);
                                        if (t != null) {
                                            t.setStaff();
                                            Database.sendTag(t);
                                        } else {
                                            t = new Tags();
                                            t.setId(message.getMessage().getMentionedUsers().get(0).getId());
                                            t.setStaff();
                                            Database.sendTag(t);
                                        }
                                    } else {
                                        message.getChannel().sendTyping().queue(bakaNiiChan);
                                    }
                                } else if (hasPrefix(message, "givePartner")) {
                                    if (message.getMessage().getMentionedUsers() != null) {
                                        Tags t = Objects.requireNonNull(Database.getTags()).getOrDefault(message.getMessage().getMentionedUsers().get(0).getId(), null);
                                        if (t != null) {
                                            t.setPartner();
                                            Database.sendTag(t);
                                        } else {
                                            t = new Tags();
                                            t.setId(message.getMessage().getMentionedUsers().get(0).getId());
                                            t.setPartner();
                                            Database.sendTag(t);
                                        }
                                    } else {
                                        message.getChannel().sendTyping().queue(bakaNiiChan);
                                    }
                                } else if (hasPrefix(message, "giveToxic")) {
                                    if (message.getMessage().getMentionedUsers() != null) {
                                        Tags t = Objects.requireNonNull(Database.getTags()).getOrDefault(message.getMessage().getMentionedUsers().get(0).getId(), null);
                                        if (t != null) {
                                            t.setToxic();
                                            Database.sendTag(t);
                                        } else {
                                            t = new Tags();
                                            t.setId(message.getMessage().getMentionedUsers().get(0).getId());
                                            t.setToxic();
                                            Database.sendTag(t);
                                        }
                                    } else {
                                        message.getChannel().sendTyping().queue(bakaNiiChan);
                                    }
                                } else if (hasPrefix(message, "test")) {
                                    try {
                                        message.getChannel().sendFile(new ProfileTest().makeProfile(message.getMember()), "test.jpg").queue();
                                    } catch (FontFormatException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }

                        //ADMIN--------------------------------------------------------------------------------->
                        if (message.getMember().hasPermission(Permission.MANAGE_CHANNEL)) {
                            if (hasPrefix(message, "definir")) {
                                Admin.config(cmd, message, gc);
                            } else if (hasPrefix(message, "configs")) {
                                Embeds.configsEmbed(message, gc);
                            } else if (hasPrefix(message, "punir")) {
                                if (message.getMessage().getMentionedUsers() != null) {
                                    Member m = Database.getMemberById(message.getMessage().getMentionedUsers().get(0).getId() + message.getGuild().getId());
                                    m.resetXp();
                                    Database.sendMember(m);
                                    message.getChannel().sendTyping().queue(tm -> message.getChannel().sendMessage(message.getMessage().getMentionedUsers().get(0).getAsMention() + " teve seus XP e leveis resetados!").queue());
                                } else {
                                    message.getChannel().sendTyping().queue(tm -> message.getChannel().sendMessage(":x: Você precisa me dizer de quem devo resetar o XP.").queue());
                                }
                            } else if (hasPrefix(message, "alertar")) {
                                if (message.getMessage().getMentionedUsers() != null && cmd.length >= 3) {
                                    Admin.addWarn(message, message.getMessage().getContentRaw().replace(gc.getPrefix(), ""));
                                } else {
                                    message.getChannel().sendTyping().queue(tm -> message.getChannel().sendMessage(":x: Você precisa mencionar um usuário e dizer o motivo do alerta.").queue());
                                }
                            } else if (hasPrefix(message, "perdoar")) {
                                if (message.getMessage().getMentionedUsers() != null && cmd.length >= 3) {
                                    Admin.takeWarn(message);
                                } else {
                                    message.getChannel().sendTyping().queue(tm -> message.getChannel().sendMessage(":x: Você precisa mencionar um usuário e dizer o Nº do alerta a ser removido.").queue());
                                }
                            } else if (hasPrefix(message, "rcargolvl")) {
                                if (cmd.length == 2) {
                                    Map<String, Object> cargos = gc.getCargoslvl();
                                    cargos.remove(cmd[1]);
                                    gc.setCargoslvl(new JSONObject(cargos));
                                    message.getChannel().sendTyping().queue(tm -> message.getChannel().sendMessage("Retirada a recompensa de cargo do level " + cmd[1] + " com sucesso!").queue());
                                } else {
                                    message.getChannel().sendTyping().queue(tm -> message.getChannel().sendMessage("Opa, algo deu errado, lembre-se de especificar apenas o level.").queue());
                                }
                            } else if (hasPrefix(message, "lvlnotif")) {
                                if (gc.getLvlNotif()) {
                                    message.getChannel().sendTyping().queue(tm -> message.getChannel().sendMessage("Não irei mais avisar quando um membro passar de nível!").queue());
                                    gc.setLvlNotif(false);
                                } else {
                                    message.getChannel().sendTyping().queue(tm -> message.getChannel().sendMessage("Agora irei avisar quando um membro passar de nível!").queue());
                                    gc.setLvlNotif(true);
                                }
                            } else if (hasPrefix(message, "ouçatodos")) {
                                gc.setAnyTell(!gc.isAnyTell());
                                guildConfig finalGc2 = gc;
                                message.getChannel().sendTyping().queue(tm -> message.getChannel().sendMessage(finalGc2.isAnyTell() ? "Irei ouvir novas respostas da comunidade agora!" : "Só irei ouvir novas respostas de um moderador!").queue());
                            }
                        }

                        //CHATBOT--------------------------------------------------------------------------------->
                        if (message.getMember().hasPermission(Permission.MANAGE_CHANNEL) || gc.isAnyTell()) {
                            if (hasPrefix(message, "fale")) {
                                if (message.getMessage().getContentRaw().contains(";") && cmd.length > 1) {
                                    CustomAnswers ca = new CustomAnswers();
                                    String com = message.getMessage().getContentRaw().replace(gc.getPrefix() + "fale", "").trim();

                                    ca.setGuildID(message.getGuild().getId());
                                    ca.setGatilho(com.split(";")[0]);
                                    ca.setAnswer(com.split(";")[1]);
                                    message.getChannel().sendTyping().queue(tm -> message.getChannel().sendMessage("Quando alguém falar `" + com.split(";")[0] + "` irei responder `" + com.split(";")[1] + "`.").queue());
                                    Database.sendCustomAnswer(ca);
                                } else {
                                    message.getChannel().sendTyping().queue(tm -> message.getChannel().sendMessage("Você não me passou argumentos suficientes!").queue());
                                }
                            } else if (hasPrefix(message, "nãofale")) {
                                if (cmd.length > 1) {
                                    try {
                                        if (Database.getCustomAnswerById(Long.valueOf(cmd[1])) != null) {
                                            CustomAnswers ca = Database.getCustomAnswerById(Long.valueOf(cmd[1]));
                                            message.getChannel().sendTyping().queue(tm -> message.getChannel().sendMessage("Não irei mais responder `" + ca.getAnswer() + "` quando alguém disser `" + ca.getGatilho() + "`.").queue());
                                            Database.deleteCustomAnswer(ca);
                                        } else {
                                            message.getChannel().sendTyping().queue(tm -> message.getChannel().sendMessage("Esta resposta não existe!").queue());
                                        }
                                    } catch (NumberFormatException e) {
                                        message.getChannel().sendTyping().queue(tm -> message.getChannel().sendMessage("Você não me passou um ID válido!").queue());
                                    }
                                } else {
                                    message.getChannel().sendTyping().queue(tm -> message.getChannel().sendMessage("Você precisa me passar um ID para que eu possa excluir uma resposta!").queue());
                                }
                            } else if (hasPrefix(message, "falealista")) {
                                if (cmd.length > 1)
                                    Embeds.answerList(message, Objects.requireNonNull(Database.getAllCustomAnswers()).stream().filter(a -> a.getGuildID().equals(message.getGuild().getId())).collect(Collectors.toList()));
                                else
                                    message.getChannel().sendTyping().queue(tm -> message.getChannel().sendMessage("Você precisa me dizer uma página (serão mostradas 5 respostas por página)!").queue());
                            }
                        }

                        //BEYBLADE--------------------------------------------------------------------------------->
                        if (hasPrefix(message, "binfo")) {
                            Beyblade bb = Database.getBeyblade(message.getAuthor().getId());
                            if (bb == null)
                                message.getChannel().sendTyping().queue(tm -> message.getChannel().sendMessage("Você não possui uma beyblade!").queue());
                            else {
                                Embeds.beybladeEmbed(message, bb);
                            }
                        } else if (hasPrefix(message, "bcomeçar")) {
                            Arena.start(message, cmd);
                        } else if (hasPrefix(message, "bcor")) {
                            Arena.setCor(message, cmd);
                        } else if (hasPrefix(message, "bnome")) {
                            Arena.setName(message, cmd);
                        } else if (hasPrefix(message, "bduelar")) {
                            Arena.duel(message, duels);
                        } else if (hasPrefix(message, "brank")) {
                            Embeds.bRankEmbed(bot, message);
                        } else if (hasPrefix(message, "bmelhorar")) {
                            if (cmd.length > 1) {
                                Arena.upgrade(message, cmd);
                            }
                        } else if (hasPrefix(message, "bshop")) {
                            Embeds.shopEmbed(message, Objects.requireNonNull(Database.getBeyblade(message.getAuthor().getId())), gc.getPrefix());
                        } else if (hasPrefix(message, "balinhamento")) {
                            if (cmd.length > 1) {
                                Arena.chooseHouse(message, cmd, Database.getBeyblade(message.getAuthor().getId()));
                            } else {
                                guildConfig finalGc3 = gc;
                                message.getChannel().sendTyping().queue(tm -> message.getChannel().sendMessage("__**Alinhamento é o que define seu estilo de combate:**__\n- Tigres são focados em uma **velocidade** extrema.\n- Dragões são focados em um **poder** incomparável.\n- Ursos são focados em uma **defesa** impenetrável.\n\n" +
                                        "Cada alinhamento possui especiais diferentes, que poderão virar um duelo, a **primeira vez** que você escolher um alinhamento custará **150 pontos de combate**. " +
                                        "Após, **qualquer troca de alinhamento custará 300 pontos de combate**.\n" +
                                        "\nPara escolher tigre, digite `" + finalGc3.getPrefix() + "balinhamento tigre`" +
                                        "\nPara escolher dragão, digite `" + finalGc3.getPrefix() + "balinhamento dragão`" +
                                        "\nPara escolher urso, digite `" + finalGc3.getPrefix() + "balinhamento urso`").queue());
                            }
                        } else if (hasPrefix(message, "bespecial")) {
                            Embeds.specialEmbed(message, Objects.requireNonNull(Database.getBeyblade(message.getAuthor().getId())));
                        }
                    }
                }
            } catch (NullPointerException | InsufficientPermissionException |
                    IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static boolean hasPrefix(MessageReceivedEvent message, String cmd) {
        guildConfig gc = null;
        try {
            gc = Database.getGuildConfigById(message.getGuild().getId());
        } catch (NoResultException e) {
            guildConfig newGc = new guildConfig();
            newGc.setGuildId(message.getGuild().getId());
            Database.sendGuildConfig(newGc);
        }
        return message.getMessage().getContentRaw().split(" ")[0].equalsIgnoreCase(Objects.requireNonNull(gc).getPrefix() + cmd);
    }
}
