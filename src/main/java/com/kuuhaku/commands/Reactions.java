package com.kuuhaku.commands;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.io.File;

public class Reactions {
    public static void hug(JDA bot, MessageReceivedEvent message) {
        File file = new File("src/ReactionFiles/hug/" + (int) (Math.random() * 5 + 1) + ".gif");
        String msg = "";
        switch ((int) (Math.random() * 3)) {
            case 0:
                msg = "Awnn, que abraço fofo! :blush:";
                break;
            case 1:
                msg = "Já shippo os dois ein!";
                break;
            case 2:
                msg = "Ai sim ein, tem que ir pra cima mesmo!";
                break;
        }

        if (message.getMessage().getMentionedUsers().get(0) == bot.getSelfUser()) {
            if (message.getAuthor().getId().equals("350836145921327115")) {
                switch ((int) (Math.random() * 3)) {
                    case 0:
                        msg = ("Arigatou, Nii-chan!");
                        break;
                    case 1:
                        msg = ("N..n..não precisava, Nii-chan!");
                        break;
                    case 2:
                        msg = ("Ni..Ni..Nii-chan no baka!");
                        break;
                }
                message.getChannel().sendMessage(msg).addFile(file).queue();
            } else {
                switch ((int) (Math.random() * 3)) {
                    case 0:
                        msg = ("Moshi moshi, FBI-sama!");
                        break;
                    case 1:
                        msg = ("B..b..baka!");
                        break;
                    case 2:
                        msg = ("Paraaa, to ocupada jogando agora!");
                        break;
                }
                message.getChannel().sendMessage(msg).addFile(file).queue();
            }
        } else {
            msg = (message.getAuthor().getAsMention() + " abraçou " + message.getMessage().getMentionedUsers().get(0).getAsMention() + " - " + msg);
            message.getChannel().sendMessage(msg).addFile(file).queue();
        }
    }

    public static void cuddle(JDA bot, MessageReceivedEvent message) {
        File file = new File("src/ReactionFiles/cuddle/" + (int) (Math.random() * 5 + 1) + ".gif");
        String msg = "";
        switch ((int) (Math.random() * 3)) {
            case 0:
                msg = "Cuti cuti cuti cuti!";
                break;
            case 1:
                msg = "Calma, deixa ele(a) respirar!";
                break;
            case 2:
                msg = "Eu ein!";
                break;
        }

        if (message.getMessage().getMentionedUsers().get(0) == bot.getSelfUser()) {
            if (message.getAuthor().getId().equals("350836145921327115")) {
                switch ((int) (Math.random() * 3)) {
                    case 0:
                        msg = ("Arigatou, Nii-chan!");
                        break;
                    case 1:
                        msg = ("N..n..não precisava, Nii-chan!");
                        break;
                    case 2:
                        msg = ("Ni..Ni..Nii-chan no baka!");
                        break;
                }
                message.getChannel().sendMessage(msg).addFile(file).queue();
            } else {
                switch ((int) (Math.random() * 3)) {
                    case 0:
                        msg = ("Moshi moshi, FBI-sama!");
                        break;
                    case 1:
                        msg = ("B..b..baka!");
                        break;
                    case 2:
                        msg = ("Paraaa, to ocupada jogando agora!");
                        break;
                }
                message.getChannel().sendMessage(msg).addFile(file).queue();
            }
        } else {
            msg = (message.getAuthor().getAsMention() + " deu um abraço kawaii em " + message.getMessage().getMentionedUsers().get(0).getAsMention() + " - " + msg);
            message.getChannel().sendMessage(msg).addFile(file).queue();
        }
    }

    public static void facedesk(MessageReceivedEvent message) {
        File file = new File("src/ReactionFiles/facedesk/" + (int) (Math.random() * 5 + 1) + ".gif");
        String msg = "";
        switch ((int) (Math.random() * 3)) {
            case 0:
                msg = "Eita, calmaaa!";
                break;
            case 1:
                msg = "Vish, para que vai dar dor de cabeça!";
                break;
            case 2:
                msg = "Calma, a culpa não foi sua!";
                break;
        }
        msg = (message.getAuthor().getAsMention() + " bateu a cara - " + msg);
        message.getChannel().sendMessage(msg).addFile(file).queue();
    }

    public static void nope(MessageReceivedEvent message) {
        File file = new File("src/ReactionFiles/nope/" + (int) (Math.random() * 5 + 1) + ".gif");
        String msg = "";
        switch ((int) (Math.random() * 3)) {
            case 0:
                msg = "Não tão rápido!";
                break;
            case 1:
                msg = "Só que não!";
                break;
            case 2:
                msg = "Hoje não!";
                break;
        }
        msg = (message.getAuthor().getAsMention() + " esquivou-se - " + msg);
        message.getChannel().sendMessage(msg).addFile(file).queue();
    }

    private static void nope(MessageReceivedEvent message, User author) {
        File file = new File("src/ReactionFiles/nope/" + (int) (Math.random() * 5 + 1) + ".gif");
        String msg = "";
        switch ((int) (Math.random() * 3)) {
            case 0:
                msg = "Não tão rápido!";
                break;
            case 1:
                msg = "Só que não!";
                break;
            case 2:
                msg = "Hoje não!";
                break;
        }
        msg = (author.getAsMention() + " esquivou-se - " + msg);
        message.getChannel().sendMessage(msg).addFile(file).queue();
    }

    public static void run(MessageReceivedEvent message) {
        File file = new File("src/ReactionFiles/run/" + (int) (Math.random() * 5 + 1) + ".gif");
        String msg = "";
        switch ((int) (Math.random() * 3)) {
            case 0:
                msg = "Corre maluco!";
                break;
            case 1:
                msg = "Sai da frenteee!";
                break;
            case 2:
                msg = "Sebo nas canelas!";
                break;
        }
        msg = (message.getAuthor().getAsMention() + " saiu correndo - " + msg);
        message.getChannel().sendMessage(msg).addFile(file).queue();
    }

    public static void slap(JDA bot, MessageReceivedEvent message) {
        File file = new File("src/ReactionFiles/slap/" + (int) (Math.random() * 5 + 1) + ".gif");
        String msg = "";
        switch ((int) (Math.random() * 3)) {
            case 0:
                msg = "Kono BAKAAAA!!";
                break;
            case 1:
                msg = "Calma, deixa ele(a) respirar!";
                break;
            case 2:
                msg = "Toma essa!";
                break;
        }

        if (message.getMessage().getMentionedUsers().get(0) == bot.getSelfUser()) {
            if (message.getAuthor().getId().equals("350836145921327115")) {
                switch ((int) (Math.random() * 3)) {
                    case 0:
                        msg = ("Nii-chan no BAKA!");
                        break;
                    case 1:
                        msg = ("Eu te avisei!");
                        break;
                    case 2:
                        msg = ("EU TO JOGANDO!");
                        break;
                }
                message.getChannel().sendMessage(msg).addFile(file).queue();
            } else {
                message.getChannel().sendMessage(msg).addFile(file).queue();
                nope(message, bot.getSelfUser());
            }
        } else {
            msg = (message.getAuthor().getAsMention() + " deu um tapa em " + message.getMessage().getMentionedUsers().get(0).getAsMention() + " - " + msg);
            message.getChannel().sendMessage(msg).addFile(file).queue();
        }
    }

    public static void smash(JDA bot, MessageReceivedEvent message) {
        File file = new File("src/ReactionFiles/smash/" + (int) (Math.random() * 5 + 1) + ".gif");
        String msg = "";
        switch ((int) (Math.random() * 3)) {
            case 0:
                msg = "Omae wa mou...SHINDEIRU!!";
                break;
            case 1:
                msg = "Morreee!";
                break;
            case 2:
                msg = "Agora deu!";
                break;
        }

        if (message.getMessage().getMentionedUsers().get(0) == bot.getSelfUser()) {
            if (message.getAuthor().getId().equals("350836145921327115")) {
                switch ((int) (Math.random() * 3)) {
                    case 0:
                        msg = ("O que eu fiz??");
                        break;
                    case 1:
                        msg = ("Vai ter volta!");
                        break;
                    case 2:
                        msg = ("Calma, eu só caí uns elozinhos com a sua conta!");
                        break;
                }
                message.getChannel().sendMessage(msg).addFile(file).queue();
            } else {
                message.getChannel().sendMessage(msg).addFile(file).queue();
                nope(message, bot.getSelfUser());
            }
        } else {
            msg = (message.getAuthor().getAsMention() + " destruiu " + message.getMessage().getMentionedUsers().get(0).getAsMention() + " - " + msg);
            message.getChannel().sendMessage(msg).addFile(file).queue();
        }
    }

    public static void stare(JDA bot, MessageReceivedEvent message) {
        File file = new File("src/ReactionFiles/stare/" + (int) (Math.random() * 5 + 1) + ".gif");
        String msg = "";
        switch ((int) (Math.random() * 3)) {
            case 0:
                msg = "Shiii~~";
                break;
            case 1:
                msg = "Observa~~";
                break;
            case 2:
                msg = "...";
                break;
        }

        if (message.getMessage().getMentionedUsers().get(0) == bot.getSelfUser()) {
            switch ((int) (Math.random() * 3)) {
                case 0:
                    msg = ("Que foi?");
                    break;
                case 1:
                    msg = ("Hum?");
                    break;
                case 2:
                    msg = ("Nani?");
                    break;
            }
            message.getChannel().sendMessage(msg).addFile(file).queue();
        } else {
            msg = (message.getAuthor().getAsMention() + " encarou " + message.getMessage().getMentionedUsers().get(0).getAsMention() + " - " + msg);
            message.getChannel().sendMessage(msg).addFile(file).queue();
        }
    }
}
