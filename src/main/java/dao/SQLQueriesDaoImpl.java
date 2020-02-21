package dao;

import core.Chuu;
import dao.entities.*;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class SQLQueriesDaoImpl implements SQLQueriesDao {


    @Override
    public void getGlobalRank(Connection connection, String lastfmId) {

    }

    @Override
    public UniqueWrapper<ArtistPlays> getGlobalCrowns(Connection connection, String lastfmId) {
        List<ArtistPlays> returnList = new ArrayList<>();
        long discordId;

        @Language("MariaDB") String queryString = "SELECT c.name , b.discord_id , playnumber AS orden" +
                " FROM  scrobbled_artist  a" +
                " JOIN user b ON a.lastfm_id = b.lastfm_id" +
                " JOIN artist c ON " +
                " a.artist_id = c.id" +
                " WHERE  a.lastfm_id = ?" +
                " AND playnumber > 0" +
                " AND  playnumber >= ALL" +
                "       (SELECT max(b.playnumber) " +
                " FROM " +
                "(SELECT in_a.artist_id,in_a.playnumber" +
                " FROM scrobbled_artist in_a  " +
                " JOIN " +
                " user in_b" +
                " ON in_a.lastfm_id = in_b.lastfm_id" +
                "   ) AS b" +
                " WHERE b.artist_id = a.artist_id" +
                " GROUP BY artist_id)" +
                " ORDER BY orden DESC";

        try (PreparedStatement preparedStatement = connection.prepareStatement(queryString)) {
            preparedStatement.setString(1, lastfmId);

            ResultSet resultSet = preparedStatement.executeQuery();

            if (!resultSet.next()) {
                return new UniqueWrapper<>(0, 0, lastfmId, returnList);

            } else {
                discordId = resultSet.getLong("b.discord_id");
                resultSet.beforeFirst();
            }

            while (resultSet.next()) {

                String artist = resultSet.getString("c.name");
                int plays = resultSet.getInt("orden");
                returnList.add(new ArtistPlays(artist, plays));
            }
        } catch (SQLException e) {
            Chuu.getLogger().warn(e.getMessage(), e);
            throw new RuntimeException(e);
        }
        return new UniqueWrapper<>(returnList.size(), discordId, lastfmId, returnList);

    }

    @Override
    public UniqueWrapper<ArtistPlays> getGlobalUniques(Connection connection, String lastfmId) {

        @Language("MariaDB") String queryString = "SELECT a.name, temp.playnumber, temp.lastfm_id, temp.discord_id " +
                "FROM(  " +
                "       SELECT artist_id, playnumber, a.lastfm_id ,b.discord_id" +
                "       FROM scrobbled_artist a JOIN user b " +
                "       ON a.lastfm_id = b.lastfm_id " +
                "       WHERE  a.playnumber > 2 " +
                "       GROUP BY a.artist_id " +
                "       HAVING count( *) = 1) temp " +
                " JOIN artist a ON temp.artist_id = a.id " +
                "WHERE temp.lastfm_id = ? AND temp.playnumber > 1 " +
                " ORDER BY temp.playnumber DESC ";

        try (PreparedStatement preparedStatement = connection.prepareStatement(queryString)) {
            int i = 1;
            preparedStatement.setString(i, lastfmId);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (!resultSet.next()) {
                return new UniqueWrapper<>(0, 0, lastfmId, new ArrayList<>());
            }

            List<ArtistPlays> returnList = new ArrayList<>();
            resultSet.last();
            int rows = resultSet.getRow();
            long discordId = resultSet.getLong("temp.discord_id");

            resultSet.beforeFirst();
            /* Get results. */

            while (resultSet.next()) { //&& (j < 10 && j < rows)) {
                String name = resultSet.getString("a.name");
                int count_a = resultSet.getInt("temp.playNumber");

                returnList.add(new ArtistPlays(name, count_a));

            }
            return new UniqueWrapper<>(rows, discordId, lastfmId, returnList);


        } catch (SQLException e) {
            Chuu.getLogger().warn(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public ResultWrapper<ArtistPlays> getArtistFrequencies(Connection connection, Long guildId) {
        @Language("MariaDB") String queryBody =
                "FROM  (SELECT artist_id " +
                        "FROM scrobbled_artist a" +
                        " JOIN user b  " +
                        " ON a.lastfm_id = b.lastfm_id " +
                        " JOIN user_guild c " +
                        " ON b.discord_id = c.discord_id" +
                        " WHERE c.guild_id = ?) main" +
                        " JOIN artist b ON" +
                        " main.artist_id = b.id ";

        String normalQuery = "SELECT b.name, count(*) AS orden " + queryBody + " GROUP BY b.id ORDER BY orden DESC  Limit 200";
        String countQuery = "Select count(*) " + queryBody;
        try (PreparedStatement preparedStatement2 = connection.prepareStatement(countQuery)) {
            preparedStatement2.setLong(1, guildId);

            ResultSet resultSet = preparedStatement2.executeQuery();
            if (!resultSet.next()) {
                throw new RuntimeException();
            }
            int rows = resultSet.getInt(1);
            try (PreparedStatement preparedStatement = connection.prepareStatement(normalQuery)) {
                preparedStatement.setLong(1, guildId);
                return getArtistPlaysResultWrapper(rows, preparedStatement);
            }
        } catch (SQLException e) {
            Chuu.getLogger().warn(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @NotNull
    private ResultWrapper<ArtistPlays> getArtistPlaysResultWrapper(int rows, PreparedStatement preparedStatement) throws SQLException {
        ResultSet resultSet;
        resultSet = preparedStatement.executeQuery();
        List<ArtistPlays> returnList = new ArrayList<>();
        while (resultSet.next()) {
            String name = resultSet.getString("b.name");
            int count = resultSet.getInt("orden");
            returnList.add(new ArtistPlays(name, count));
        }

        return new ResultWrapper<>(rows, returnList);
    }


    @Override
    public ResultWrapper<ArtistPlays> getGlobalArtistFrequencies(Connection connection) {
        @Language("MariaDB") String queryString =
                "FROM  scrobbled_artist a" +
                        " JOIN artist b " +
                        " ON a.artist_id = b.id ";


        String normalQuery = "SELECT b.name, count(*) AS orden " + queryString + "     GROUP BY artist_id  ORDER BY orden DESC  Limit 200";
        String countQuery = "Select count(*) " + queryString;
        int rows = 0;
        try (PreparedStatement preparedStatement2 = connection.prepareStatement(countQuery)) {

            ResultSet resultSet = preparedStatement2.executeQuery();
            if (!resultSet.next()) {
                throw new RuntimeException();
            }
            rows = resultSet.getInt(1);
            try (PreparedStatement preparedStatement = connection.prepareStatement(normalQuery)) {


                return getArtistPlaysResultWrapper(rows, preparedStatement);
            }
        } catch (SQLException e) {
            Chuu.getLogger().warn(e.getMessage(), e);
            throw new RuntimeException(e);
        }

    }

    @Override
    public UniqueWrapper<ArtistPlays> getUniqueArtist(Connection connection, Long guildID, String lastfmId) {
        @Language("MariaDB") String queryString = "SELECT * " +
                "FROM(  " +
                "       SELECT a2.name, playnumber, a.lastfm_id ,b.discord_id" +
                "       FROM scrobbled_artist a JOIN user b " +
                "       ON a.lastfm_id = b.lastfm_id " +
                "       JOIN user_guild c ON b.discord_id = c.discord_id " +
                " JOIN artist a2 ON a.artist_id = a2.id " +
                "       WHERE c.guild_id = ? AND a.playnumber > 2 " +
                "       GROUP BY a.artist_id " +
                "       HAVING count( *) = 1) temp " +
                "WHERE temp.lastfm_id = ? AND temp.playnumber > 1 " +
                " ORDER BY temp.playnumber DESC ";

        try (PreparedStatement preparedStatement = connection.prepareStatement(queryString)) {
            int i = 1;
            preparedStatement.setLong(i++, guildID);
            preparedStatement.setString(i, lastfmId);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (!resultSet.next()) {
                return new UniqueWrapper<>(0, 0, lastfmId, new ArrayList<>());
            }

            List<ArtistPlays> returnList = new ArrayList<>();
            resultSet.last();
            int rows = resultSet.getRow();
            long discordId = resultSet.getLong("temp.discord_id");

            resultSet.beforeFirst();
            /* Get results. */

            while (resultSet.next()) { //&& (j < 10 && j < rows)) {
                String name = resultSet.getString("temp.name");
                int count_a = resultSet.getInt("temp.playNumber");

                returnList.add(new ArtistPlays(name, count_a));

            }
            return new UniqueWrapper<>(rows, discordId, lastfmId, returnList);


        } catch (SQLException e) {
            Chuu.getLogger().warn(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public ResultWrapper<UserArtistComparison> similar(Connection connection, List<String> lastfMNames) {
        int MAX_IN_DISPLAY = 10;
        String userA = lastfMNames.get(0);
        String userB = lastfMNames.get(1);

        @Language("MariaDB") String queryString =
                "SELECT c.name  , a.playnumber,b.playnumber ," +
                        "((a.playnumber + b.playnumber)/(abs(a.playnumber-b.playnumber)+1)* ((a.playnumber + b.playnumber))*2.5) media ," +
                        " c.url " +
                        "FROM " +
                        "(SELECT artist_id,playnumber " +
                        "FROM scrobbled_artist " +
                        "JOIN user b ON scrobbled_artist.lastfm_id = b.lastfm_id " +
                        "WHERE b.lastfm_id = ? ) a " +
                        "JOIN " +
                        "(SELECT artist_id,playnumber " +
                        "FROM scrobbled_artist " +
                        " JOIN user b ON scrobbled_artist.lastfm_id = b.lastfm_id " +
                        " WHERE b.lastfm_id = ? ) b " +
                        "ON a.artist_id=b.artist_id " +
                        "JOIN artist c " +
                        "ON c.id=b.artist_id" +
                        " ORDER BY media DESC ";

        try (PreparedStatement preparedStatement = connection.prepareStatement(queryString)) {

            /* Fill "preparedStatement". */
            int i = 1;
            preparedStatement.setString(i++, userA);
            preparedStatement.setString(i, userB);


            /* Execute query. */
            ResultSet resultSet = preparedStatement.executeQuery();
            List<UserArtistComparison> returnList = new ArrayList<>();

            if (!resultSet.next()) {
                return new ResultWrapper<>(0, returnList);
            }
            resultSet.last();
            int rows = resultSet.getRow();
            resultSet.beforeFirst();
            /* Get results. */
            int j = 0;
            while (resultSet.next() && (j < MAX_IN_DISPLAY && j < rows)) {
                j++;
                String name = resultSet.getString("c.name");
                int count_a = resultSet.getInt("a.playNumber");
                int count_b = resultSet.getInt("b.playNumber");
                String url = resultSet.getString("c.url");
                returnList.add(new UserArtistComparison(count_a, count_b, name, userA, userB, url));
            }

            return new ResultWrapper<>(rows, returnList);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public WrapperReturnNowPlaying knows(Connection con, long artistId, long guildId, int limit) {

        @Language("MariaDB")
        String queryString =
                "SELECT a2.name, a.lastfm_id, a.playNumber, a2.url, c.discord_id " +
                        "FROM  scrobbled_artist a" +
                        " JOIN artist a2 ON a.artist_id = a2.id  " +
                        "JOIN `user` c on c.lastFm_Id = a.lastFM_ID " +
                        "JOIN user_guild d on c.discord_ID = d.discord_Id " +
                        "where d.guild_Id = ? " +
                        "and  a2.id = ? " +
                        "ORDER BY a.playNumber desc ";
        queryString = limit == Integer.MAX_VALUE ? queryString : queryString + "limit " + limit;
        try (PreparedStatement preparedStatement = con.prepareStatement(queryString)) {

            /* Fill "preparedStatement". */
            int i = 1;
            preparedStatement.setLong(i++, guildId);
            preparedStatement.setLong(i, artistId);



            /* Execute query. */

            ResultSet resultSet = preparedStatement.executeQuery();
            int rows;
            String url = "";
            String artistName = "";
            List<ReturnNowPlaying> returnList = new ArrayList<>();
            if (!resultSet.next()) {
                rows = 0;
            } else {
                resultSet.last();
                rows = resultSet.getRow();
                url = resultSet.getString("a2.url");
                artistName = resultSet.getString("a2.name");


            }
            /* Get generated identifier. */

            resultSet.beforeFirst();
            /* Get results. */
            int j = 0;
            while (resultSet.next() && (j < limit && j < rows)) {
                j++;
                String lastfmId = resultSet.getString("a.lastFM_ID");

                int playNumber = resultSet.getInt("a.playNumber");
                long discordId = resultSet.getLong("c.discord_ID");

                returnList.add(new ReturnNowPlaying(discordId, lastfmId, artistName, playNumber));
            }
            /* Return booking. */
            return new WrapperReturnNowPlaying(returnList, rows, url, artistName);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public UniqueWrapper<ArtistPlays> getCrowns(Connection connection, String lastfmId, long guildID) {
        List<ArtistPlays> returnList = new ArrayList<>();
        long discordId;

        @Language("MariaDB") String queryString = "SELECT a2.name, b.discord_id , playnumber AS orden" +
                " FROM  scrobbled_artist  a" +
                " JOIN user b ON a.lastfm_id = b.lastfm_id " +
                " JOIN artist a2 ON a.artist_id = a2.id " +
                " WHERE  b.lastfm_id = ?" +
                " AND playnumber > 0" +
                " AND  playnumber >= ALL" +
                "       (SELECT max(b.playnumber) " +
                " FROM " +
                "(SELECT in_a.artist_id,in_a.playnumber" +
                " FROM scrobbled_artist in_a  " +
                " JOIN " +
                " user in_b" +
                " ON in_a.lastfm_id = in_b.lastfm_id" +
                " NATURAL JOIN " +
                " user_guild in_c" +
                " WHERE guild_id = ?" +
                "   ) AS b" +
                " WHERE b.artist_id = a.artist_id" +
                " GROUP BY artist_id)" +
                " ORDER BY orden DESC";

        try (PreparedStatement preparedStatement = connection.prepareStatement(queryString)) {
            int i = 1;
            preparedStatement.setString(i++, lastfmId);
            preparedStatement.setLong(i, guildID);

            ResultSet resultSet = preparedStatement.executeQuery();

            if (!resultSet.next()) {
                return new UniqueWrapper<>(0, 0, lastfmId, returnList);

            } else {
                discordId = resultSet.getLong("b.discord_id");
                resultSet.beforeFirst();
            }

            while (resultSet.next()) {

                String artist = resultSet.getString("a2.name");
                int plays = resultSet.getInt("orden");
                returnList.add(new ArtistPlays(artist, plays));
            }
        } catch (SQLException e) {
            Chuu.getLogger().warn(e.getMessage(), e);
            throw new RuntimeException(e);
        }
        return new UniqueWrapper<>(returnList.size(), discordId, lastfmId, returnList);
    }

    @Override
    public List<UrlCapsule> getGuildTop(Connection connection, Long guildID, int limit) {
        //TODO LIMIT
        @Language("MariaDB") String queryString = "SELECT d.name, sum(playnumber) AS orden ,url  " +
                "FROM  scrobbled_artist a" +
                " JOIN user b" +
                " ON a.lastfm_id = b.lastfm_id" +
                " JOIN artist d " +
                " ON a.artist_id = d.id";
        if (guildID != null) {

            queryString += " JOIN  user_guild c" +
                    " ON b.discord_id=c.discord_id" +
                    " WHERE c.guild_id = ?";
        }
        queryString += " GROUP BY artist_id,url" +
                " ORDER BY orden DESC" +
                " LIMIT ?;";
        List<UrlCapsule> list = new LinkedList<>();

        try (PreparedStatement preparedStatement = connection.prepareStatement(queryString)) {
            int i = 1;
            if (guildID != null)
                preparedStatement.setLong(i++, guildID);

            preparedStatement.setInt(i, limit);

            ResultSet resultSet = preparedStatement.executeQuery();
            int count = 0;
            while (resultSet.next()) {
                String artist = resultSet.getString("d.name");
                String url = resultSet.getString("url");

                int plays = resultSet.getInt("orden");

                UrlCapsule capsule = new UrlCapsule(url, count++, artist, "", "");
                capsule.setPlays(plays);
                list.add(capsule);
            }
        } catch (SQLException e) {
            Chuu.getLogger().warn(e.getMessage(), e);
        }
        return list;
    }

    @Override
    public int userPlays(Connection con, long artistId, String whom) {
        @Language("MariaDB") String queryString = "SELECT a.playnumber " +
                "FROM scrobbled_artist a JOIN user b ON a.lastfm_id=b.lastfm_id " +
                "JOIN artist c ON a.artist_id = c.id " +
                "WHERE a.lastfm_id = ? AND c.id = ?";
        try (PreparedStatement preparedStatement = con.prepareStatement(queryString)) {
            /* Fill "preparedStatement". */
            int i = 1;
            preparedStatement.setString(i++, whom);
            preparedStatement.setLong(i, artistId);




            /* Execute query. */
            ResultSet resultSet = preparedStatement.executeQuery();
            if (!resultSet.next())
                return 0;
            return resultSet.getInt("a.playNumber");


        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<LbEntry> crownsLeaderboard(Connection connection, long guildID) {
        @Language("MariaDB") String queryString = "SELECT t2.lastfm_id,t3.discord_id,count(t2.lastfm_id) ord " +
                "FROM " +
                "( " +
                "SELECT " +
                "        a.artist_id,max(a.playnumber) plays " +
                "    FROM " +
                "         scrobbled_artist a  " +
                "    JOIN " +
                "        user b  " +
                "            ON a.lastfm_id = b.lastfm_id  " +
                "    JOIN " +
                "        user_guild c  " +
                "            ON b.discord_id = c.discord_id  " +
                "    WHERE " +
                "        c.guild_id = ?  " +
                "    GROUP BY " +
                "        a.artist_id  " +
                "  ) t " +
                "  JOIN scrobbled_artist t2  " +
                "   " +
                "  ON t.plays = t2.playnumber AND t.artist_id = t2.artist_id " +
                "  JOIN user t3  ON t2.lastfm_id = t3.lastfm_id  " +
                "    JOIN " +
                "        user_guild t4  " +
                "            ON t3.discord_id = t4.discord_id  " +
                "    WHERE " +
                "        t4.guild_id = ?  " +
                "  GROUP BY t2.lastfm_id,t3.discord_id " +
                "  ORDER BY ord DESC";

        return getLbEntries(connection, guildID, queryString, CrownsLbEntry::new, true);


    }

    @Override
    public List<LbEntry> uniqueLeaderboard(Connection connection, long guildId) {
        @Language("MariaDB") String queryString = "SELECT  " +
                "    count(temp.lastfm_id) AS ord,temp.lastfm_id,temp.discord_id " +
                "FROM " +
                "    (SELECT  " +
                "         a.lastfm_id, b.discord_id " +
                "    FROM " +
                "        scrobbled_artist a " +
                "    JOIN user b ON a.lastfm_id = b.lastfm_id " +
                "    JOIN user_guild c ON b.discord_id = c.discord_id " +
                "    WHERE " +
                "        c.guild_id = ? " +
                "            AND a.playnumber > 2 " +
                "    GROUP BY a.artist_id " +
                "    HAVING COUNT(*) = 1) temp " +
                "GROUP BY lastfm_id " +
                "ORDER BY ord DESC";

        return getLbEntries(connection, guildId, queryString, UniqueLbEntry::new, false);
    }


    @Override
    public int userArtistCount(Connection con, String whom) {
        @Language("MariaDB") String queryString = "SELECT count(*) AS numb FROM scrobbled_artist WHERE scrobbled_artist.lastfm_id=?";
        try (PreparedStatement preparedStatement = con.prepareStatement(queryString)) {
            /* Fill "preparedStatement". */
            int i = 1;
            preparedStatement.setString(i, whom);

            /* Execute query. */
            ResultSet resultSet = preparedStatement.executeQuery();
            if (!resultSet.next())
                return 0;
            return resultSet.getInt("numb");


        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<LbEntry> artistLeaderboard(Connection con, long guildID) {
        @Language("MariaDB") String queryString = "(SELECT  " +
                "        a.lastfm_id , count(*) AS ord, c.discord_id" +
                "    FROM " +
                "        scrobbled_artist a " +
                "    JOIN user b ON a.lastfm_id = b.lastfm_id " +
                "    JOIN user_guild c ON b.discord_id = c.discord_id " +
                "    WHERE " +
                "        c.guild_id = ? " +
                " GROUP BY a.lastfm_id,c.discord_id " +
                "    ORDER BY ord DESC    )";

        return getLbEntries(con, guildID, queryString, ArtistLbEntry::new, false);
    }

    @Override
    public List<LbEntry> obscurityLeaderboard(Connection connection, long guildId) {
        @Language("MariaDB") String queryString = "\n" +
                "SELECT finalmain.lastfm_id,  POW(((mytotalplays / (other_plays_on_my_artists)) * (as_unique_coefficient + 1)),\n" +
                "            0.4) AS ord , c.discord_id\n" +
                "FROM (\n" +
                "SELECT \n" +
                "    main.lastfm_id,\n" +                //OBtains total plays, and other users plays on your artist
                "    (SELECT \n" +
                "              COALESCE(SUM(a.playnumber) * (COUNT(*)), 0)\n" +
                "        FROM\n" +
                "            scrobbled_artist a\n" +
                "        WHERE\n" +
                "            lastfm_id = main.lastfm_id) AS mytotalplays,\n" +
                "    (SELECT \n" +
                "             COALESCE(SUM(a.playnumber), 1)\n" +
                "        FROM\n" +
                "            scrobbled_artist a\n" +
                "        WHERE\n" +
                "            lastfm_id != main.lastfm_id\n" +
                "                AND a.artist_id IN (SELECT \n" +
                "                    artist_id\n" +
                "                FROM\n" +
                "                    artist\n" +
                "                WHERE\n" +
                "                    lastfm_id = main.lastfm_id))AS  other_plays_on_my_artists,\n" +
                "  " +
                "  (SELECT \n" +                // Obtains uniques, percentage of uniques, and plays on uniques
                "            COUNT(*) / (SELECT \n" +
                "                        COUNT(*) + 1\n" +
                "                    FROM\n" +
                "                        scrobbled_artist a\n" +
                "                    WHERE\n" +
                "                        lastfm_id = main.lastfm_id) * (COALESCE(SUM(playnumber), 1))\n" +
                "        FROM\n" +
                "            (SELECT \n" +
                "                artist_id, playnumber, a.lastfm_id\n" +
                "            FROM\n" +
                "                scrobbled_artist a\n" +
                "            GROUP BY a.artist_id\n" +
                "            HAVING COUNT(*) = 1) temp \n" +
                "        WHERE\n" +
                "            temp.lastfm_id = main.lastfm_id\n" +
                "                AND temp.playnumber > 1\n" +
                "        ) as_unique_coefficient\n" +
                "FROM\n" +
                //"\t#full artist table, we will filter later because is somehow faster :D\n" +
                "    scrobbled_artist main\n" +
                "    \n" +
                "GROUP BY main.lastfm_id\n" +
                ") finalmain" +
                " JOIN user b\n" +
                "ON finalmain.lastfm_id = b.lastfm_id \n" +
                "JOIN user_guild c ON b.discord_id = c.discord_id \n" +
                "WHERE c.guild_id = ?" +
                " ORDER BY ord DESC";

        return getLbEntries(connection, guildId, queryString, ObscurityEntry::new, false);
    }

    @Override
    public PresenceInfo getRandomArtistWithUrl(Connection connection) {

        @Language("MariaDB") String queryString =
                "SELECT \n" +
                        "    b.name,\n" +
                        "    b.url,\n " +
                        "    discord_id,\n" +
                        "    (SELECT \n" +
                        "            SUM(playnumber)\n" +
                        "        FROM\n" +
                        "            scrobbled_artist\n" +
                        "        WHERE\n" +
                        "            artist_id = a.artist_id) AS summa\n" +
                        "FROM\n" +
                        "    scrobbled_artist a\n" +
                        "        JOIN\n" +
                        "    artist b ON a.artist_id = b.id\n" +
                        "        NATURAL JOIN\n" +
                        "    user c\n" +
                        "WHERE\n" +
                        "    b.id IN (SELECT \n" +
                        "            rando.id\n" +
                        "        FROM\n" +
                        "            (SELECT \n" +
                        "                a.id\n" +
                        "            FROM\n" +
                        "                artist a\n" +
                        "                WHERE a.url IS NOT NULL\n" +
                        "                AND a.url != ''\n" +
                        "            ORDER BY RAND()\n" +
                        "            LIMIT 1) rando)\n" +
                        "ORDER BY RAND()\n" +
                        "LIMIT 1;";
        try (PreparedStatement preparedStatement = connection.prepareStatement(queryString)) {

            ResultSet resultSet = preparedStatement.executeQuery();
            if (!resultSet.next())
                return null;

            String artist_id = resultSet.getString("name");
            String url = resultSet.getString("url");

            long summa = resultSet.getLong("summa");
            long discordId = resultSet.getLong("discord_id");
            return new PresenceInfo(artist_id, url, summa, discordId);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public StolenCrownWrapper getCrownsStolenBy(Connection connection, String ogUser, String queriedUser, long guildId) {
        List<StolenCrown> returnList = new ArrayList<>();
        long discordid;
        long discordid2;
        @Language("MariaDB") String queryString = "SELECT \n" +
                "    inn.name AS artist ,inn.orden AS ogplays , inn.discord_id AS ogid , inn2.discord_id queriedid,  inn2.orden AS queriedplays\n" +
                "FROM\n" +
                "    (SELECT \n" +
                "        a.artist_id, a2.name, b.discord_id, playnumber AS orden\n" +
                "    FROM\n" +
                "        scrobbled_artist a\n" +
                "    JOIN user b ON a.lastfm_id = b.lastfm_id\n" +
                " JOIN artist a2 ON a.artist_id = a2.id " +
                "    WHERE\n" +
                "        a.lastfm_id = ?) inn\n" +
                "        JOIN\n" +
                "    (SELECT \n" +
                "        artist_id, b.discord_id, playnumber AS orden\n" +
                "    FROM\n" +
                "        scrobbled_artist a\n" +
                "    JOIN user b ON a.lastfm_id = b.lastfm_id\n" +
                "    WHERE\n" +
                "        b.lastfm_id = ?) inn2 ON inn.artist_id = inn2.artist_id\n" +
                "WHERE\n" +
                "    (inn2.artist_id , inn2.orden) = (SELECT \n" +
                "            in_a.artist_id, MAX(in_a.playnumber)\n" +
                "        FROM\n" +
                "            scrobbled_artist in_a\n" +
                "                JOIN\n" +
                "            user in_b ON in_a.lastfm_id = in_b.lastfm_id\n" +
                "                NATURAL JOIN\n" +
                "            user_guild in_c\n" +
                "        WHERE\n" +
                "            guild_id = ?\n" +
                "                AND artist_id = inn2.artist_id)\n" +
                "        AND (inn.artist_id , inn.orden) = (SELECT \n" +
                "            in_a.artist_id, in_a.playnumber\n" +
                "        FROM\n" +
                "            scrobbled_artist in_a\n" +
                "                JOIN\n" +
                "            user in_b ON in_a.lastfm_id = in_b.lastfm_id\n" +
                "                NATURAL JOIN\n" +
                "            user_guild in_c\n" +
                "        WHERE\n" +
                "            guild_id = ?\n" +
                "                AND artist_id = inn.artist_id\n" +
                "        ORDER BY in_a.playnumber DESC\n" +
                "        LIMIT 1 , 1)\n" +
                "ORDER BY inn.orden DESC , inn2.orden DESC\n" +
                "        \n";

        try (PreparedStatement preparedStatement = connection.prepareStatement(queryString)) {
            int i = 1;
            preparedStatement.setString(i++, ogUser);
            preparedStatement.setString(i++, queriedUser);

            preparedStatement.setLong(i++, guildId);
            preparedStatement.setLong(i, guildId);

            ResultSet resultSet = preparedStatement.executeQuery();
            if (!resultSet.next()) {
                return new StolenCrownWrapper(0, 0, returnList);
            } else {
                discordid = resultSet.getLong("ogId");
                discordid2 = resultSet.getLong("queriedId");
                resultSet.beforeFirst();
            }

            while (resultSet.next()) {

                String artist = resultSet.getString("artist");
                int plays = resultSet.getInt("ogPlays");
                int plays2 = resultSet.getInt("queriedPlays");

                returnList.add(new StolenCrown(artist, plays, plays2));
            }
        } catch (SQLException e) {
            Chuu.getLogger().warn(e.getMessage(), e);
            throw new RuntimeException(e);
        }
        //Ids will be 0 if returnlist is empty;
        return new StolenCrownWrapper(discordid, discordid2, returnList);
    }

    @Override
    public UniqueWrapper<ArtistPlays> getUserAlbumCrowns(Connection connection, String lastfmId, long guildId) {

        @Language("MariaDB") String queryString = "SELECT a2.name ,a.album,a.plays,b.discord_id " +
                "FROM album_crowns a " +
                "JOIN user b ON a.discordid = b.discord_id" +
                " JOIN artist a2 ON a.artist_id = a2.id " +
                " WHERE guildid = ? AND b.lastfm_id = ? ORDER BY plays DESC";

        try (PreparedStatement preparedStatement = connection.prepareStatement(queryString)) {
            int i = 1;
            preparedStatement.setLong(i++, guildId);
            preparedStatement.setString(i, lastfmId);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (!resultSet.next()) {
                return new UniqueWrapper<>(0, 0, lastfmId, new ArrayList<>());
            }

            List<ArtistPlays> returnList = new ArrayList<>();
            resultSet.last();
            int rows = resultSet.getRow();

            long discordId = resultSet.getLong("discord_id");

            resultSet.beforeFirst();
            /* Get results. */

            while (resultSet.next()) { //&& (j < 10 && j < rows)) {
                String name = resultSet.getString("name");
                String album = resultSet.getString("album");

                int count_a = resultSet.getInt("plays");

                returnList.add(new ArtistPlays(name + " - " + album, count_a));

            }
            return new UniqueWrapper<>(rows, discordId, lastfmId, returnList);


        } catch (SQLException e) {
            Chuu.getLogger().warn(e.getMessage(), e);
        }
        return null;
    }


    @Override
    public List<LbEntry> albumCrownsLeaderboard(Connection con, long guildID) {
        @Language("MariaDB") String queryString = "SELECT \n" +
                "    b.discord_id , b.lastfm_id, COUNT(*) AS ord\n" +
                "FROM\n" +
                "    album_crowns a\n" +
                "      RIGHT JOIN\n" +
                "    user b ON a.discordid = b.discord_id\n" +
                "WHERE\n" +
                "    guildid = ?\n" +
                "GROUP BY a.discordid , b.lastfm_id\n" +
                "ORDER BY ord DESC ;";

        return getLbEntries(con, guildID, queryString, AlbumCrownLbEntry::new, false);
    }

    @Override
    public ObscuritySummary getUserObscuritPoints(Connection connection, String lastfmId) {
        @Language("MariaDB") String queryString = "\tSELECT  b, other_plays_on_my_artists, unique_coefficient,\n" +
                "\tPOW(((b/ (other_plays_on_my_artists)) * (unique_coefficient + 1)),0.4) AS total\n" +
                "\t\tFROM (\n" +
                "\n" +
                "\tSELECT (SELECT sum(a.playnumber) * count(*) FROM \n" +
                "\tscrobbled_artist a \n" +
                "\tWHERE lastfm_id = main.lastfm_id) AS b ,  \n" +
                "\t\t   (SELECT COALESCE(Sum(a.playnumber), 1) \n" +
                "\t\t\tFROM   scrobbled_artist a \n" +
                " WHERE  lastfm_id != main.lastfm_id \n" +
                "   AND a.artist_id IN (SELECT artist_id \n" +
                "   FROM   artist \n" +
                "   WHERE  lastfm_id = main.lastfm_id)) AS \n" +
                "   other_plays_on_my_artists, \n" +
                "   (SELECT Count(*) / (SELECT Count(*) + 1 \n" +
                "   FROM   scrobbled_artist a \n" +
                "\t\t\t\t\t\t\t   WHERE  lastfm_id = main.lastfm_id) * ( \n" +
                "\t\t\t\t   COALESCE(Sum(playnumber \n" +
                "\t\t\t\t\t\t\t), 1) ) \n" +
                "\t\t\tFROM   (SELECT artist_id, \n" +
                "\t\t\t\t\t\t   playnumber, \n" +
                "\t\t\t\t\t\t   a.lastfm_id \n" +
                "\t\t\t\t\tFROM   scrobbled_artist a \n" +
                "\t\t\t\t\tGROUP  BY a.artist_id \n" +
                "\t\t\t\t\tHAVING Count(*) = 1) temp \n" +
                "\t\t\tWHERE  temp.lastfm_id = main.lastfm_id \n" +
                "\t\t\t\t   AND temp.playnumber > 1) \n" +
                "\t\t   AS unique_coefficient                      \n" +
                "\tFROM   scrobbled_artist main \n" +
                "\tWHERE  lastfm_id =  ?" +
                " GROUP BY lastfm_id\n" +
                "\t\n" +
                "\t) outer_main\n";
        try (PreparedStatement preparedStatement = connection.prepareStatement(queryString)) {
            int i = 1;
            preparedStatement.setString(i, lastfmId);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (!resultSet.next()) {
                return null;

            }

            int totalPlays = resultSet.getInt("b");
            int other_plays_on_my_artists = resultSet.getInt("other_plays_on_my_artists");
            int unique_coefficient = resultSet.getInt("unique_coefficient");
            int total = resultSet.getInt("total");

            return new ObscuritySummary(totalPlays, other_plays_on_my_artists, unique_coefficient, total);


        } catch (SQLException e) {
            Chuu.getLogger().warn(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public int getRandomCount(Connection connection, Long userId) {
        @Language("MariaDB") String queryString = "SELECT \n" +
                "  count(*) as counted " +
                "FROM\n" +
                "    randomlinks \n";
        if (userId != null) {
            queryString += "WHERE discord_id = ?";

        }
        try (PreparedStatement preparedStatement = connection.prepareStatement(queryString)) {
            if (userId != null) {
                preparedStatement.setLong(1, userId);
            }
            ResultSet resultSet = preparedStatement.executeQuery();
            if (!resultSet.next()) {
                return 0;
            }

            return resultSet.getInt("counted");

        } catch (SQLException e) {
            Chuu.getLogger().error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<GlobalCrown> getGlobalKnows(Connection connection, long artistID) {
        List<GlobalCrown> returnedList = new ArrayList<>();
        @Language("MariaDB") String queryString = "SELECT  playnumber AS ord, discord_id, l.lastfm_id\n" +
                " FROM  scrobbled_artist ar\n" +
                "  	 	 JOIN user l ON ar.lastfm_id = l.lastfm_id " +
                "        WHERE  ar.artist_id = ? " +
                "        ORDER BY  playnumber DESC";


        try (PreparedStatement preparedStatement = connection.prepareStatement(queryString)) {
            int i = 1;
            preparedStatement.setLong(i, artistID);


            ResultSet resultSet = preparedStatement.executeQuery();
            int j = 1;
            while (resultSet.next()) { //&& (j < 10 && j < rows)) {


                String lastfmId = resultSet.getString("lastfm_id");
                long discordId = resultSet.getLong("discord_id");
                int crowns = resultSet.getInt("ord");

                returnedList.add(new GlobalCrown(lastfmId, discordId, crowns, j++));
            }
            return returnedList;
        } catch (SQLException e) {
            Chuu.getLogger().warn(e.getMessage(), e);
            throw new RuntimeException((e));
        }
    }

    //TriFunction is not the simplest approach but i felt like using it so :D
    @NotNull
    private List<LbEntry> getLbEntries(Connection connection, long guildId, String queryString, TriFunction<String, Long, Integer, LbEntry> fun, boolean needs_reSet) {
        List<LbEntry> returnedList = new ArrayList<>();
        try (PreparedStatement preparedStatement = connection.prepareStatement(queryString)) {
            int i = 1;
            preparedStatement.setLong(i, guildId);
            if (needs_reSet)
                preparedStatement.setLong(++i, guildId);

            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) { //&& (j < 10 && j < rows)) {
                String lastfmId = resultSet.getString("lastfm_id");
                long discordId = resultSet.getLong("discord_id");
                int crowns = resultSet.getInt("ord");

                returnedList.add(fun.apply(lastfmId, discordId, crowns));


            }
            return returnedList;
        } catch (SQLException e) {
            Chuu.getLogger().warn(e.getMessage(), e);
            throw new RuntimeException((e));
        }
    }
}



