package main.Commands;

import main.Parsers.Parser;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public abstract class MyCommand extends ListenerAdapter {
	final String PREFIX = "!";
	Parser parser;

	abstract void onCommand(MessageReceivedEvent e, String[] args);

	abstract List<String> getAliases();

	abstract String getDescription();

	abstract String getName();

	abstract List<String> getUsageInstructions();


	@Override
	public void onMessageReceived(MessageReceivedEvent e) {
		if (!e.getMessage().getContentRaw().startsWith(PREFIX) || (e.getAuthor().isBot() && !respondToBots()))
			return;

		if (containsCommand(e.getMessage())) {
			e.getChannel().sendTyping().queue();
			System.out.println("We received a message from " +
					e.getAuthor().getName() + "; " + e.getMessage().getContentDisplay());

			measureTime(e);


		}
	}

	private void measureTime(MessageReceivedEvent e) {
		long startTime = System.currentTimeMillis();
		onCommand(e, commandArgs(e.getMessage()));
		long endTime = System.currentTimeMillis();
		long timeElapsed = endTime - startTime;
		System.out.println("Execution time in milliseconds " + getName() + " : " + timeElapsed);
		System.out.println();
	}

	boolean containsCommand(Message message) {
		return getAliases().contains(commandArgs(message)[0]);
	}

	String[] commandArgs(Message message) {
		return commandArgs(message.getContentDisplay());
	}

	String[] commandArgs(String string) {
		return string.split(" ");
	}

	Message sendMessage(MessageReceivedEvent e, Message message) {
		if (e.isFromType(ChannelType.PRIVATE))
			return e.getPrivateChannel().sendMessage(message).complete();
		else
			return e.getTextChannel().sendMessage(message).complete();
	}

	Message sendMessage(MessageReceivedEvent e, String message) {
		return sendMessage(e, new MessageBuilder().append(message).build());
	}

	boolean respondToBots() {
		return false;
	}


	public String[] getSubMessage(Message message) {
		String[] parts = message.getContentRaw().substring(1).split("\\s+");

		return Arrays.copyOfRange(parts, 1, parts.length);

	}

	void sendImage(BufferedImage image, MessageReceivedEvent e) {
		MessageBuilder messageBuilder = new MessageBuilder();
		if (image == null) {
			sendMessage(e, "Ish Pc Bad");
			return;
		}
		ByteArrayOutputStream b = new ByteArrayOutputStream();

		try {
			ImageIO.write(image, "png", b);
			byte[] img = b.toByteArray();
			if (img.length < 8388608)
				messageBuilder.sendTo(e.getChannel()).addFile(img, "cat.png").queue();
			else
				messageBuilder.setContent("Boot to big").sendTo(e.getChannel()).queue();

		} catch (IOException ex) {
			sendMessage(e, "Ish Pc Bad");
			ex.printStackTrace();
			return;
		}


	}

	String getUserString(Long discordid, MessageReceivedEvent e, String replacement) {

		Member member = e.getGuild().getMemberById(discordid);
		return member == null ? replacement : member.getEffectiveName();
	}


}
