package core.commands;

import core.exceptions.InstanceNotFoundException;
import core.exceptions.LastFmException;
import core.otherlisteners.Reactionary;
import core.parsers.OptionableParser;
import core.parsers.OptionalEntity;
import core.parsers.Parser;
import core.parsers.params.CommandParameters;
import dao.ChuuService;
import dao.entities.NowPlayingArtist;
import dao.entities.UsersWrapper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class AllPlayingCommand extends ConcurrentCommand<CommandParameters> {
    public AllPlayingCommand(ChuuService dao) {
        super(dao);

        this.respondInPrivate = false;

    }

    @Override
    public Parser<CommandParameters> getParser() {
        return new OptionableParser(new OptionalEntity("--recent", "show last song from ALL users"));
    }

    @Override
    public String getDescription() {
        return ("Returns lists of all people that are playing music right now");
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("playing");
    }

    @Override
    public void onCommand(MessageReceivedEvent e) throws LastFmException, InstanceNotFoundException {

        CommandParameters parameters = parser.parse(e);
        boolean showFresh = !parameters.hasOptional("--recent");

        List<UsersWrapper> list = getService().getAll(e.getGuild().getIdLong());

        EmbedBuilder embedBuilder = new EmbedBuilder().setColor(CommandUtil.randomColor())
                .setThumbnail(e.getGuild().getIconUrl())
                .setTitle(
                        (showFresh ? "What is being played now in " : "What was being played in ")
                        + CommandUtil.cleanMarkdownCharacter(e.getGuild().getName()));

        List<String> result = list.parallelStream().map(u ->
        {
            Optional<NowPlayingArtist> opt;
            try {
                opt = Optional.of(lastFM.getNowPlayingInfo(u.getLastFMName()));
            } catch (Exception ex) {
                opt = Optional.empty();
            }
            return Map.entry(u, opt);
        }).filter(x -> {
            Optional<NowPlayingArtist> value = x.getValue();
            return value.isPresent() && !(showFresh && !value.get().isNowPlaying());
        }).map(x -> {
                    UsersWrapper usersWrapper = x.getKey();
                    NowPlayingArtist value = x.getValue().get(); //Checked previous filter
                    String username = getUserString(e, usersWrapper.getDiscordID(), usersWrapper.getLastFMName());
                    String started = !showFresh && value.isNowPlaying() ? "#" : "+";
                    return started + " [" +
                           username + "](" +
                           CommandUtil.getLastFmUser(usersWrapper.getLastFMName()) +
                           "): " +
                           CommandUtil.cleanMarkdownCharacter(value.getSongName() +
                                                              " - " + value.getArtistName() +
                                                              " | " + value.getAlbumName() + "\n");
                }
        ).collect(Collectors.toList());
        if (result.isEmpty()) {
            sendMessageQueue(e, "None is listening to music at the moment UwU");
            return;
        }
        StringBuilder a = new StringBuilder();
        int count = 0;
        for (String string : result) {
            count++;
            if ((a.length() > 1500) || (count == 30)) {
                break;
            }
            a.append(string);
        }
        int pageSize = count;
        MessageBuilder mes = new MessageBuilder();
        embedBuilder.setDescription(a);
        e.getChannel().sendMessage(mes.setEmbed(embedBuilder.build()).build()).queue(message1 ->
                new Reactionary<>(result, message1, pageSize, embedBuilder, false));

    }

    @Override
    public String getName() {
        return "Playing";
    }


}
