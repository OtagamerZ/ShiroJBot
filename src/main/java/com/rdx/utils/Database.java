package com.rdx.utils;

public class Database {

    /*
    public void createCon() throws SQLException, ClassNotFoundException{
        Class.forName("org.sqlite.JDBC");
        Connection con = DriverManager.getConnection("jdbc:sqlite:shiro.sqlite");
        con = DriverManager.getConnection("jdbc:sqlite:shiro.sqlite");
        System.out.println("✅ | Conectado à base de dados com sucesso.");
    }

    public ResultSet executeQuery(String query) throws SQLException {
            Statement state = con.createStatement();
            ResultSet rs = state.executeQuery(query);
            return rs;
    }
    */

    /*public void listGuilds() {
        try {
            this.stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM guilds");

            String spc = " | ";

            int id = rs.getInt("id");
            //----//
            String guild_name = rs.getString("guild_name");
            int guild_id = rs.getInt("guild_id");
            String guild_owner = rs.getString("guild_owner");
            int guild_ownerID = rs.getInt("guild_ownerID");
            String icon_url = rs.getString("icon_url");
            String prefix = rs.getString("prefix");
            String joined_date = rs.getString("joined_date");
            //----//
            String logs_channel = rs.getString("logs_channel");
            //----//
            Boolean plugin__welcome = rs.getBoolean("plugin__welcome");
            String welcome_channel = rs.getString("welcome_channel");
            String welcome_message = rs.getString("welcome_message");
            //----//
            Boolean plugin__goodbye = rs.getBoolean("plugin__goodbye");
            String goodbye_channel = rs.getString("goodbye_channel");
            String goodbye_message = rs.getString("goodbye_message");

            while(rs.next()) {
                System.out.println(id + spc + guild_name + spc + guild_id + spc + guild_owner + spc + guild_ownerID + spc + prefix + spc + joined_date + spc + logs_channel
                        + spc + plugin__welcome + spc + welcome_channel + spc + welcome_message + spc + plugin__goodbye + spc + goodbye_channel + spc + goodbye_message);
                System.out.println("Icon: " + icon_url);
            }
        } catch (Exception err) { err.printStackTrace(); }
    }*/

    /*
    public ResultSet getGuildPrefix(Guild guild) throws SQLException {
        String query = "SELECT prefix FROM guilds WHERE guild_id = '" + guild.getId() + "';";
        Statement state = con.createStatement();
        ResultSet rs = state.executeQuery(query);
        System.out.println(rs);
        return rs;
    }

    public void closeConnection() {
        try {
            con.close();
        } catch (Exception err) { err.printStackTrace(); }
    }
    */
}
