package core.commands.stats;

import core.commands.abstracts.ConcurrentCommand;
import core.commands.utils.CommandCategory;
import core.commands.utils.CommandUtil;
import core.exceptions.LastFmException;
import core.parsers.NoOpParser;
import core.parsers.OptionalEntity;
import core.parsers.Parser;
import core.parsers.params.CommandParameters;
import dao.ChuuService;
import dao.entities.DiscordUserDisplay;
import dao.entities.ScoredAlbumRatings;
import dao.exceptions.InstanceNotFoundException;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import javax.validation.constraints.NotNull;
import java.util.List;

public class TopRatedRandomUrls extends ConcurrentCommand<CommandParameters> {
    public TopRatedRandomUrls(ChuuService dao) {
        super(dao);
    }

    @Override
    protected CommandCategory initCategory() {
        return CommandCategory.BOT_STATS;
    }

    @Override
    public Parser<CommandParameters> initParser() {
        NoOpParser noOpParser = new NoOpParser();
        noOpParser.addOptional(new OptionalEntity("myself", " show your top rated urls "));
        noOpParser.addOptional(new OptionalEntity("server", " show ratings from users only in this server"));
        return noOpParser;
    }

    @Override
    public String getDescription() {
        return "The top rated random urls by yourself, this server or the bot";
    }

    @Override
    public List<String> getAliases() {
        return List.of("toprandoms", "topr", "urls");
    }

    @Override
    public String getName() {
        return "Top random urls";
    }

    @Override
    protected void onCommand(MessageReceivedEvent e, @NotNull CommandParameters params) throws LastFmException, InstanceNotFoundException {


        boolean server = params.hasOptional("server");
        boolean myself = params.hasOptional("myself");
        List<ScoredAlbumRatings> ratings;

        String title;
        String url;

        if (server && params.getE().isFromGuild()) {
            title = "users in this server";
            url = params.getE().getGuild().getIconUrl();
            long idLong = params.getE().getGuild().getIdLong();
            ratings = getService().getServerTopUrl(idLong);
        } else {
            if (!myself || (server && !params.getE().isFromGuild())) {
                ratings = getService().getGlobalTopUrl();

                title = "users in the bot";
                url = params.getE().getJDA().getSelfUser().getAvatarUrl();
            } else {
                long idLong = e.getAuthor().getIdLong();
                ratings = getService().getByUserTopRatedUrls(idLong);
                DiscordUserDisplay userInfoConsideringGuildOrNot = CommandUtil.getUserInfoConsideringGuildOrNot(params.getE(), idLong);
                title = userInfoConsideringGuildOrNot.getUsername();
                url = userInfoConsideringGuildOrNot.getUrlImage();

            }
        }
        MyTopRatedRandomUrls.RandomUrlDisplay(e, ratings, "Top random urls rated by " + title, url);
    }
}
