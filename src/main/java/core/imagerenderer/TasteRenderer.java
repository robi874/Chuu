package core.imagerenderer;

import dao.entities.ResultWrapper;
import dao.entities.Results;
import dao.entities.UserInfo;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TasteRenderer {
	private static final int PROFILE_IMAGE_SIZE = 100;
	private static final int x_MAX = 600;
	private static final int y_MAX = 500;


	public static BufferedImage generateTasteImage(ResultWrapper resultWrapper, List<UserInfo> userInfoLiust) {

		BufferedImage canvas = new BufferedImage(x_MAX, y_MAX, BufferedImage.TYPE_INT_RGB);

		List<BufferedImage> imageList = new ArrayList<>();

		Graphics2D g = canvas.createGraphics();
		GraphicUtils.setQuality(g);

		GraphicUtils.initRandomImageBlurredBackground(g, x_MAX, y_MAX);

		//Gets Profile Images
		for (UserInfo userInfo : userInfoLiust) {
			try {
				java.net.URL url = new java.net.URL(userInfo.getImage());
				imageList.add(ImageIO.read(url));
			} catch (IndexOutOfBoundsException | IOException exception) {
				//JDK error when reading a gif as png | other errors
				imageList.add(GraphicUtils.noArtistImage);
			}

		}

		//Init Of Variables
		Font artistFont = new Font("Roboto", Font.PLAIN, 21);
		Font numberFont = new Font("Heebo-Light", Font.PLAIN, 21);
		Font titleFont = new Font("Heebo-Light", Font.PLAIN, 23);
		Font scrobbleFont = new Font("Heebo-Light", Font.BOLD, 17);
		int startFont = 26;
		Font usernameFont = (new Font("Roboto Medium", Font.PLAIN, startFont));
		Font subtitle = new Font("Roboto Condensed Bold Italic", Font.ITALIC, 12);

		int x = 0;
		int y = 20;
		int image1StartPosition, image2StartPosition;
		image1StartPosition = 20;
		image2StartPosition = canvas.getWidth() - PROFILE_IMAGE_SIZE - 20;
		g.setColor(Color.WHITE);
		//int rectangle_start_x = image1StartPosition + PROFILE_IMAGE_SIZE + 4;
		int rectangle_start_y = y + PROFILE_IMAGE_SIZE - 20;
		int rectangle_height = g.getFontMetrics().getHeight();
		int rectangle_width = image2StartPosition - image1StartPosition - PROFILE_IMAGE_SIZE - 8;

		float[] rgb1 = new float[3];
		Color.ORANGE.getRGBColorComponents(rgb1);
		Color colorA = new Color(rgb1[0], rgb1[1], rgb1[2], 0.5f);
		Color colorA1 = new Color(rgb1[0], rgb1[1], rgb1[2], 0.8f);

		float[] rgb2 = new float[3];
		Color.CYAN.getRGBColorComponents(rgb2);
		Color colorB = new Color(rgb2[0], rgb2[1], rgb2[2], 0.5f);
		Color colorB1 = new Color(rgb2[0], rgb2[1], rgb2[2], 0.8f);

		g.setFont(usernameFont);
		int widht1 = g.getFontMetrics().stringWidth(userInfoLiust.get(0).getUsername());
		int width2 = g.getFontMetrics().stringWidth(userInfoLiust.get(1).getUsername());
		int totalwidth = widht1 + width2 + 4;
		int disponibleSize = rectangle_width + 8;

		while (totalwidth >= disponibleSize) {
			startFont -= 2;
			usernameFont = new Font("Roboto Medium", Font.PLAIN, startFont);
			g.setFont(usernameFont);
			widht1 = g.getFontMetrics().stringWidth(userInfoLiust.get(0).getUsername());
			width2 = g.getFontMetrics().stringWidth(userInfoLiust.get(1).getUsername());
			totalwidth = widht1 + width2 + 4;
		}
		int totalCount = userInfoLiust.stream().mapToInt(UserInfo::getPlayCount).sum();

		//Draws Profile Images
		for (BufferedImage image : imageList) {
			int drawx;
			int nameStringPosition;
			Color color;
			int countStringPosition;
			int rectanglePosition;
			UserInfo userInfo = userInfoLiust.get(x);
			float percentage = (float) userInfo.getPlayCount() / totalCount;

			if (x == 0) {
				drawx = image1StartPosition;
				nameStringPosition = image1StartPosition + PROFILE_IMAGE_SIZE + 4;
				color = colorA.brighter();
				countStringPosition = image1StartPosition + PROFILE_IMAGE_SIZE + 5;
				rectanglePosition = countStringPosition - 1;
			} else {
				drawx = image2StartPosition;
				nameStringPosition = image2StartPosition - width2 - 4;
				color = colorB.brighter();
				countStringPosition = image2StartPosition - g.getFontMetrics()
						.stringWidth(String.valueOf(userInfo.getPlayCount())) - 5;
				rectanglePosition = (int) (image2StartPosition - percentage * rectangle_width) - 4;
			}
			g.setColor(color);
			g.drawImage(image, drawx, y, 100, 100, null);
			g.fillRect(rectanglePosition, rectangle_start_y, (int) (rectangle_width * percentage), rectangle_height);
			g.setColor(Color.WHITE);
			g.setFont(usernameFont);
			GraphicUtils.drawStringNicely(g, userInfo
					.getUsername(), nameStringPosition, 20 + PROFILE_IMAGE_SIZE / 2, canvas);
			g.setFont(scrobbleFont);
			GraphicUtils.drawStringNicely(g, "" + userInfo
					.getPlayCount(), countStringPosition, rectangle_start_y + rectangle_height - 1, canvas);
			x++;

		}

		//Draws Common Artists
		y = rectangle_start_y + 64 + 20;
		String a = String.valueOf(resultWrapper.getRows());

		g.setFont(titleFont);
		int length = g.getFontMetrics().stringWidth(a);
		GraphicUtils.drawStringNicely(g, "" + resultWrapper.getRows(), x_MAX / 2 - length / 2, y - 30, canvas);

		g.setFont(subtitle);

		GraphicUtils.drawStringNicely(g, "common artists", x_MAX / 2 + length / 2 + 4, y - 30, canvas);

		//Draws Top 10

		for (Results item : resultWrapper.getResultList()) {

			String artistID = item.getArtistID();
			int countA = item.getCountA();
			int countB = item.getCountB();

			int halfDistance = x_MAX - 200;
			int ac = Math.round((float) countA / (float) (countA + countB) * halfDistance);
			int bc = Math.round((float) countB / (float) (countA + countB) * halfDistance);
			g.setColor(colorA1);
			g.fillRect(x_MAX / 2 - ac / 2, y + 3, ac / 2, 5);
			g.setColor(colorB1);
			g.fillRect(x_MAX / 2, y + 3, bc / 2, 5);

			g.setColor(Color.WHITE);
			g.setFont(numberFont);
			String strCountBString = String.valueOf(item.getCountB());

			int widthB = g.getFontMetrics().stringWidth(strCountBString);

			int countBStart = x_MAX - 100 - widthB;
			GraphicUtils.drawStringNicely(g, "" + countA, 100, y, canvas);
			GraphicUtils.drawStringNicely(g, "" + countB, countBStart, y, canvas);
			g.setFont(artistFont);

			Font fontToUse;
			if (g.getFont().canDisplayUpTo(artistID) != -1) {
				fontToUse = new Font("Noto Serif CJK JP", Font.PLAIN, 21);
				g.setFont(fontToUse);
			}
			int widthStr = g.getFontMetrics().stringWidth(item.getArtistID());
			GraphicUtils.drawStringNicely(g, artistID, x_MAX / 2 - (widthStr / 2), y, canvas);
			y += g.getFontMetrics().getHeight() + 5;
		}
		g.dispose();
		return canvas;
	}


}
