package main.commands;

import dao.DaoImplementation;
import dao.entities.NowPlayingArtist;
import main.apis.youtube.Search;
import main.apis.youtube.SearchSingleton;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.Arrays;
import java.util.List;

public class NPYoutubeCommand extends NpCommand {
	private final Search search;

	public NPYoutubeCommand(DaoImplementation dao) {
		super(dao);
		this.search = SearchSingleton.getInstanceUsingDoubleLocking();

	}

	@Override
	public void doSomethingWithArtist(NowPlayingArtist nowPlayingArtist, MessageReceivedEvent e) {
		MessageBuilder messageBuilder = new MessageBuilder();

		String uri = search.doSearch(nowPlayingArtist.getArtistName() + " " + nowPlayingArtist.getSongName());

		if (uri.equals("")) {
			System.out.println("Doing a second attempt");
			uri = search.doSearch(nowPlayingArtist.getSongName() + " " + nowPlayingArtist.getArtistName());
			if (uri.equals("")) {
				sendMessageQueue(e, "Was not able to find artist " + nowPlayingArtist
						.getArtistName() + " - " + nowPlayingArtist.getSongName() + " on YT");
				return;
			}
		}
		messageBuilder.setContent(uri).sendTo(e.getChannel()).queue();
	}

	@Override
	public String getDescription() {
		return "Returns a link to your current song via Youtube";
	}

	@Override
	public List<String> getAliases() {
		return Arrays.asList("npyt", "npyoutube");
	}

	@Override
	public String getName() {
		return "Now Playing Youtube";
	}

}
