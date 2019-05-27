package com.rdx.utils;

import com.rdx.Main;
import net.dv8tion.jda.core.entities.Guild;

import java.io.File;
import java.sql.*;

public class SQLite {

    private static Connection con;
    private static Statement statement;

    public static void connect() throws SQLException {
        con = null;

        File DBfile = new File(Main.getInfo().getDBFileName());
        if(!DBfile.exists()) { System.out.println("❌ | O ficheiro usado como base de dados não foi encontrado. Entre no servidor discord oficial da Shiro para obter ajuda.");}

        String url = "jdbc:sqlite:" + DBfile.getPath();
        con = DriverManager.getConnection(url);
        statement = con.createStatement();
        System.out.println("✅ | Ligação à base de dados estabelecida.");
    }

    public static void disconnect() throws SQLException {
        if (con != null) {
            con.close();
            System.out.println("✅ | Ligação à base de dados desfeita.");
        }
    }

    /*public static ResultSet onQuery(String query) throws SQLException {
        Boolean rs;
        rs = statement.executeQuery(query);

        return rs;
    }*/

    public static String getGuildPrefix(String id) throws SQLException {
        String query = "SELECT * FROM guilds WHERE guild_id = '" + id + "';";
        String prefix;
        //ResultSet rs = statement.executeQuery(query);
        //return rs.getString("prefix");
        ResultSet rs = statement.executeQuery(query);
        prefix = rs.getString("prefix");

        if(prefix == null) {
            prefix = "s!";
        } else {
            prefix = rs.getString("prefix");
        }
        return prefix;
    }

    public static void addGuildToDB(Guild guild) throws SQLException {
        String guild_name = guild.getName();
        String guild_id = guild.getId();
        String guild_owner = guild.getOwner().getUser().getAsTag();
        String guild_ownerID = guild.getOwnerId();

        String query = "INSERT INTO guilds(guild_name, guild_id, guild_owner, guild_ownerID, prefix) VALUES(?, ?, ?, ?, ?);";

        PreparedStatement pstmt = con.prepareStatement(query);
        pstmt.setString(1, guild_name);
        pstmt.setString(2, guild_id);
        pstmt.setString(3, guild_owner);
        pstmt.setString(4, guild_ownerID);
        pstmt.setString(5, Main.getInfo().getDefaultPrefix());

        pstmt.executeUpdate();
    }

    public static void removeGuildFromDB(Guild guild) throws SQLException {
        String guild_id = guild.getId();

        String query = "DELETE FROM guilds WHERE guild_id = ?;";

        PreparedStatement pstmt = con.prepareStatement(query);
        pstmt.setString(1, guild_id);

        pstmt.executeUpdate();
    }
}