package core.commands.music;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import core.Chuu;
import core.commands.abstracts.MusicCommand;
import core.commands.utils.CommandUtil;
import core.exceptions.LastFmException;
import core.music.MusicManager;
import core.music.utils.TrackContext;
import core.parsers.NoOpParser;
import core.parsers.Parser;
import core.parsers.params.CommandParameters;
import dao.ChuuService;
import dao.exceptions.InstanceNotFoundException;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import javax.validation.constraints.NotNull;
import java.util.List;

public class NpVoiceCommand extends MusicCommand<CommandParameters> {
    public NpVoiceCommand(ChuuService dao) {
        super(dao);
        requirePlayingTrack = true;
        requirePlayer = true;
    }

    @Override
    public Parser<CommandParameters> initParser() {
        return new NoOpParser();
    }

    @Override
    public String getDescription() {
        return "Your now playing song on voice";
    }

    @Override
    public List<String> getAliases() {
        return List.of("vnp", "current", "current", "voice", "song");
    }

    @Override
    public String getName() {
        return "Voice now playing";
    }

    @Override
    protected void onCommand(MessageReceivedEvent e, @NotNull CommandParameters params) throws LastFmException, InstanceNotFoundException {
        MusicManager manager = Chuu.playerRegistry.get(e.getGuild());
        AudioTrack track = manager.getPlayer().getPlayingTrack();

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Now Playing")
                .setDescription(String.format("[%s](%s)", track.getInfo().title, track.getInfo().uri));
        if (manager.getRadio() != null) {
            String b = "Currently streaming music from radio station " + manager.getRadio().getSource().getName() +
                    ", requested by " + manager.getRadio().requester() +
                    ". When the queue is empty, random tracks from the station will be added.";
            embedBuilder.addField("Radio", b, false);
        }
        embedBuilder.addField("Requester", track.getUserData(TrackContext.class).getRequesterStr(), true)
                .addField("Request Channel", track.getUserData(TrackContext.class).getChannelRequesterStr(), true)
                .addBlankField(true)
                .addField("Repeating", manager.getRepeatOption().name().toLowerCase(), true)
                .addField("Volume", manager.getPlayer().getVolume() + "%", true)
                .addField("Bass Boost", "false", true);
        String timeString;
        if (track.getDuration() == Long.MAX_VALUE) {
            timeString = "Streaming";
        } else {
            var position = CommandUtil.getTimestamp(track.getPosition());
            var duration = CommandUtil.getTimestamp(track.getDuration());
            timeString = "`[" + position + " / " + duration + "]`";
        }
        embedBuilder.addField("Time", timeString, true);
        double percent = track.getPosition() / (double) track.getDuration();
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < 20; i++) {
            if ((int) (percent * (20 - 1)) == i) {
                str.append("__**▬**__");
            } else {
                str.append("―");
            }
        }
        str.append(String.format(" **%.1f**%%", percent * 100.0));
        embedBuilder.addField("Progress", str.toString(), false);

        if (manager.getLoops() > 5) {
            embedBuilder.setFooter("bröther may i have some lööps | You've looped ${manager.loops} times");
        } else {
            embedBuilder.setFooter("Use " + CommandUtil.getMessagePrefix(e) + "lyrics to see the lyrics of the song!");
        }
        e.getChannel().sendMessage(embedBuilder.build()).queue();
    }

}