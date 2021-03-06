package core.commands.crowns;

import core.commands.abstracts.ConcurrentCommand;
import core.commands.utils.CommandCategory;
import core.commands.utils.CommandUtil;
import core.exceptions.LastFmException;
import core.otherlisteners.Reactionary;
import core.parsers.NumberParser;
import core.parsers.OnlyUsernameParser;
import core.parsers.Parser;
import core.parsers.params.ChuuDataParams;
import core.parsers.params.NumberParameters;
import dao.ChuuService;
import dao.entities.ArtistPlays;
import dao.entities.DiscordUserDisplay;
import dao.entities.UniqueWrapper;
import dao.exceptions.InstanceNotFoundException;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static core.parsers.ExtraParser.LIMIT_ERROR;

public class CrownsCommand extends ConcurrentCommand<NumberParameters<ChuuDataParams>> {
    public CrownsCommand(ChuuService dao) {
        super(dao);
        this.respondInPrivate = false;
    }


    @Override
    protected CommandCategory initCategory() {
        return CommandCategory.CROWNS;
    }

    @Override
    public Parser<NumberParameters<ChuuDataParams>> initParser() {
        Map<Integer, String> map = new HashMap<>(2);
        map.put(LIMIT_ERROR, "The number introduced must be positive and not very big");
        String s = "You can also introduce a number to vary the number of plays to award a crown, " +
                "defaults to whatever the guild has configured (0 if not configured)";
        return new NumberParser<>(new OnlyUsernameParser(getService()),
                null,
                Integer.MAX_VALUE,
                map, s, false, true, true);
    }

    public String getTitle() {
        return "";
    }


    public UniqueWrapper<ArtistPlays> getList(NumberParameters<ChuuDataParams> params) {
        Long threshold = params.getExtraParam();
        long idLong = params.getE().getGuild().getIdLong();

        if (threshold == null) {
            threshold = (long) getService().getGuildCrownThreshold(idLong);
        }
        return getService().getCrowns(params.getInnerParams().getLastFMData().getName(), idLong, Math.toIntExact(threshold));
    }

    @Override
    public String getDescription() {
        return ("List of artist you are the top listener within a server");
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("crowns");
    }

    @Override
    protected void onCommand(MessageReceivedEvent e, @NotNull NumberParameters<ChuuDataParams> params) throws LastFmException, InstanceNotFoundException {


        ChuuDataParams innerParams = params.getInnerParams();
        UniqueWrapper<ArtistPlays> uniqueDataUniqueWrapper = getList(params);
        DiscordUserDisplay userInformation = CommandUtil.getUserInfoConsideringGuildOrNot(e, innerParams.getLastFMData().getDiscordId());
        String userName = userInformation.getUsername();
        String userUrl = userInformation.getUrlImage();
        List<ArtistPlays> resultWrapper = uniqueDataUniqueWrapper.getUniqueData();

        int rows = resultWrapper.size();
        if (rows == 0) {
            sendMessageQueue(e, userName + " doesn't have any " + getTitle() + "crown :'(");
            return;
        }

        StringBuilder a = new StringBuilder();
        for (int i = 0; i < 10 && i < rows; i++) {
            ArtistPlays g = resultWrapper.get(i);
            a.append(i + 1).append(g.toString());
        }


        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setDescription(a)
                .setColor(CommandUtil.randomColor())
                .setTitle(String.format("%s's %scrowns", userName, getTitle()), CommandUtil.getLastFmUser(uniqueDataUniqueWrapper.getLastFmId()))
                .setFooter(String.format("%s has %d %scrowns!!%n", CommandUtil.markdownLessUserString(userName, uniqueDataUniqueWrapper.getDiscordId(), e), resultWrapper.size(), getTitle()), null)
                .setThumbnail(userUrl);
        e.getChannel().sendMessage(new MessageBuilder()
                .setEmbed(embedBuilder.build()).build()).queue(message1 ->

                new Reactionary<>(resultWrapper, message1, embedBuilder));
    }

    @Override
    public String getName() {
        return "Crowns";
    }


}



