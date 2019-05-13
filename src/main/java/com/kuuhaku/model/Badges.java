package com.kuuhaku.model;

public class Badges {
    public static String getBadges(boolean[] conquistas) {
        String conq = "";
        conq += "0 - Moderador! " + (conquistas[0] ? "[:white_check_mark: ]\n" : "[:x:]\n");
        conq += "1 - Desafiando quem? " + (conquistas[1] ? "[:white_check_mark:]\n" : "[:x:]\n");
        conq += "2 - Isso é lootável? " + (conquistas[2] ? "[:white_check_mark:]\n" : "[:x:]\n");
        conq += "3 - Humpf, achei fácil! " + (conquistas[3] ? "[:white_check_mark:]\n" : "[:x:]\n");
        conq += "4 - Temos um Sherok Homer aqui! " + (conquistas[4] ? "[:white_check_mark:]\n" : "[:x:]\n");
        conq += "5 - Salvo....por pouco! " + (conquistas[5] ? "[:white_check_mark:]\n" : "[:x:]\n");
        conq += "6 - Ummm, então é isso, né? " + (conquistas[6] ? "[:white_check_mark:]\n" : "[:x:]\n");
        conq += "7 - Treinador iniciante. " + (conquistas[7] ? "[:white_check_mark:]\n" : "[:x:]\n");
        conq += "8 - Mestre treinador. " + (conquistas[8] ? "[:white_check_mark:]\n" : "[:x:]\n");
        conq += "9 - Amante de nekos. " + (conquistas[9] ? "[:white_check_mark:]\n" : "[:x:]\n");
        conq += "10 - Recrutador de elite! " + (conquistas[10] ? "[:white_check_mark:]\n" : "[:x:]\n");
        conq += "11 - Viajante das estrelas! " + (conquistas[11] ? "[:white_check_mark:]\n" : "[:x:]\n");

        return conq;
    }
}
