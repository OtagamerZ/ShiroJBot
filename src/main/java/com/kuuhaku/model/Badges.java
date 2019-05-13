package com.kuuhaku.model;

public class Badges {
    public static String getBadges(boolean[] conquistas) {
        String conq = "";
        conq += "Moderador! " + (conquistas[0] ? "[✔]\n" : "[✖]\n");
        conq += "Desafiando quem? " + (conquistas[1] ? "[✔]\n" : "[✖]\n");
        conq += "Isso é lootável? " + (conquistas[2] ? "[✔]\n" : "[✖]\n");
        conq += "Humpf, achei fácil! " + (conquistas[3] ? "[✔]\n" : "[✖]\n");
        conq += "Temos um Sherok Homer aqui! " + (conquistas[4] ? "[✔]\n" : "[✖]\n");
        conq += "Salvo....por pouco! " + (conquistas[5] ? "[✔]\n" : "[✖]\n");
        conq += "Ummm, então é isso, né? " + (conquistas[6] ? "[✔]\n" : "[✖]\n");
        conq += "Treinador iniciante. " + (conquistas[7] ? "[✔]\n" : "[✖]\n");
        conq += "Mestre treinador. " + (conquistas[8] ? "[✔]\n" : "[✖]\n");
        conq += "Amante de nekos. " + (conquistas[9] ? "[✔]\n" : "[✖]\n");
        conq += "Recrutador de elite! " + (conquistas[10] ? "[✔]\n" : "[✖]\n");
        conq += "Viajante das estrelas! " + (conquistas[11] ? "[✔]\n" : "[✖]\n");
        
        return conq;
    }
}
