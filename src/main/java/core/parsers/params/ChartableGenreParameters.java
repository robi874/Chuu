package core.parsers.params;

import dao.entities.ChartMode;
import dao.entities.LastFMData;
import dao.entities.TimeFrameEnum;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class ChartableGenreParameters extends ChartParameters {
    private final GenreParameters genreParameters;

    public ChartableGenreParameters(MessageReceivedEvent e, String lastfmID, long discordId, ChartMode chartMode, TimeFrameEnum timeFrameEnum, int x, int y, GenreParameters genreParameters, LastFMData lastFMData) {
        super(e, lastfmID, discordId, chartMode, lastFMData, timeFrameEnum, x, y);
        this.genreParameters = genreParameters;
    }

    public ChartableGenreParameters(MessageReceivedEvent e, String lastfmID, long discordId, TimeFrameEnum timeFrameEnum, int x, int y, boolean writeTitles, boolean writePlays, boolean isList, ChartMode chartMode, GenreParameters genreParameters, LastFMData lastFMData) {
        super(e, lastfmID, discordId, timeFrameEnum, x, y, writeTitles, writePlays, isList, chartMode, lastFMData);
        this.genreParameters = genreParameters;
    }

    public GenreParameters getGenreParameters() {
        return genreParameters;
    }
}
