package core.parsers.params;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class ArtistUrlParameters extends ArtistParameters {
    private final String url;

    public String getUrl() {
        return url;
    }

    public ArtistUrlParameters(MessageReceivedEvent e, String artist, User user, String url) {
        super(e, artist, user);
        this.url = url;

    }
}
