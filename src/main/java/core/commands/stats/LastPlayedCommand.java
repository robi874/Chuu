package core.commands.stats;

import core.commands.albums.AlbumPlaysCommand;
import core.commands.utils.CommandCategory;
import core.commands.utils.CommandUtil;
import core.exceptions.LastFmException;
import core.parsers.ArtistSongParser;
import core.parsers.OptionalEntity;
import core.parsers.Parser;
import core.parsers.params.ArtistAlbumParameters;
import dao.ChuuService;
import dao.entities.LastFMData;
import dao.entities.ScrobbledArtist;
import dao.exceptions.InstanceNotFoundException;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public class LastPlayedCommand extends AlbumPlaysCommand {
    public LastPlayedCommand(ChuuService dao) {
        super(dao);
    }

    @Override
    protected CommandCategory initCategory() {
        return CommandCategory.USER_STATS;
    }

    @Override
    public Parser<ArtistAlbumParameters> initParser() {
        ArtistSongParser artistSongParser = new ArtistSongParser(getService(), lastFM);
        artistSongParser.addOptional(new OptionalEntity("today", "to not include the current day"));
        return artistSongParser;
    }

    @Override
    public String getDescription() {
        return "Last time you scrobbled a song";
    }

    @Override
    public List<String> getAliases() {
        return List.of("lasttrack", "lastsong", "lasttr");
    }

    @Override
    public String getName() {
        return "Last song scrobbled";
    }

    @Override
    protected void doSomethingWithAlbumArtist(ScrobbledArtist artist, String song, MessageReceivedEvent e, long who, ArtistAlbumParameters params) throws LastFmException, InstanceNotFoundException {
        LastFMData lastFMData = params.getLastFMData();

        Optional<Instant> instant = getService().getLastScrobbled(artist.getArtistId(), song, params.getLastFMData().getName());
        if (instant.isEmpty()) {
            sendMessageQueue(e, "Couldn't get the last time you scrobbled **" + song + "** by _" + artist.getArtist() + "_");
            return;
        }
        String usernameString = getUserString(e, who, lastFMData.getName());
        OffsetDateTime offsetDateTime = OffsetDateTime.ofInstant(instant.get(), lastFMData.getTimeZone().toZoneId());
        String date = CommandUtil.getAmericanizedDate(offsetDateTime);
        sendMessageQueue(e, String.format("Last time that **%s** scrobbled **%s** was at %s", usernameString, CommandUtil.cleanMarkdownCharacter(song), date));
    }
}

