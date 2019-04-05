package main.last;

import DAO.Entities.ArtistData;
import DAO.Entities.NowPlayingArtist;
import DAO.Entities.UrlCapsule;
import DAO.Entities.UserInfo;
import main.ImageRenderer.CollageMaker;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


public class ConcurrentLastFM {//implements LastFMService {
	private static final String API_KEY = "&api_key=a5e08a41d7b5a3c71c45708190b792f4";
	private static final String BASE = "http://ws.audioscrobbler.com/2.0/";
	private static final String GET_ALBUMS = "?method=user.gettopalbums&user=";
	private static final String GET_LIBRARY = "?method=library.getartists&user=";
	private static final String GET_USER = "?method=user.getinfo&user=";
	private static final String ending = "&format=json";
	private static final String GET_NOW_PLAYINH = "?method=user.getrecenttracks&limit=1&user=";
	private static final BlockingQueue<UrlCapsule> queue = new LinkedBlockingQueue<>();

	//@Override
	public static NowPlayingArtist getNowPlayingInfo(String user) throws LastFMServiceException {
		HttpClient client = new HttpClient();
		String url = BASE + GET_NOW_PLAYINH + user + API_KEY + ending;
		GetMethod method = new GetMethod(url);
		try {
			int statusCode = client.executeMethod(method);
			if (statusCode != HttpStatus.SC_OK) {
				System.err.println("Method failed: " + method.getStatusLine());
				throw new LastFMServiceException("Error in the service: " + method.getStatusLine());
			}
			byte[] responseBody = method.getResponseBody();
			JSONObject obj = new JSONObject(new String(responseBody));
			obj = obj.getJSONObject("recenttracks");
			JSONObject tracltObj = obj.getJSONArray("track").getJSONObject(0);

			JSONObject artistObj = tracltObj.getJSONObject("artist");
			String artistname = artistObj.getString("#text");
			String mbid = artistObj.getString("mbid");
			boolean nowPlayin;
			try {
				nowPlayin = tracltObj.getJSONObject("@attr").getBoolean("nowplaying");
			} catch (JSONException e) {
				nowPlayin = false;
			}
			String albumName = tracltObj.getJSONObject("album").getString("#text");
			String songName = tracltObj.getString("name");
			String image_url = tracltObj.getJSONArray("image").getJSONObject(2).getString("#text");

			return new NowPlayingArtist(artistname, mbid, nowPlayin, albumName, songName, image_url);
		} catch (IOException e) {
			throw new LastFMServiceException("Error in the service: " + method.getStatusLine());
		}

	}

	//@Override
	public static List<UserInfo> getUserInfo(List<String> lastFmNames) throws LastFMServiceException {
		HttpClient client = new HttpClient();
		List<UserInfo> returnList = new ArrayList<>();

		try {

			for (String lastFmName : lastFmNames) {
				String url = BASE + GET_USER + lastFmName + API_KEY + ending;
				GetMethod method = new GetMethod(url);
				int statusCode = client.executeMethod(method);
				if (statusCode != HttpStatus.SC_OK) {
					System.err.println("Method failed: " + method.getStatusLine());
					throw new LastFMServiceException("Error in the service: " + method.getStatusLine());
				}
				byte[] responseBody = method.getResponseBody();
				JSONObject obj = new JSONObject(new String(responseBody));
				obj = obj.getJSONObject("user");
				JSONArray image = obj.getJSONArray("image");
				JSONObject bigImage = image.getJSONObject(2);
				String image2 = bigImage.getString("#text");
				int playcount = obj.getInt("playcount");
				returnList.add(new UserInfo(playcount, image2, lastFmName));

			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Execute the method.


		// Read the response body.


		return returnList;

	}

	//@Override
	public static LinkedList<ArtistData> getLibrary(String User) throws LastFMServiceException {
		String url = BASE + GET_LIBRARY + User + API_KEY + ending;
		int page = 1;
		int pages = 1;
		HttpClient client = new HttpClient();
		url += "&limit=500";

		LinkedList<ArtistData> linkedlist = new LinkedList<>();
		Map<String, Integer> map = new HashMap<>();
		while (page <= pages) {

			String urlPage = url + "&page=" + page;
			GetMethod method = new GetMethod(urlPage);

			try {

				// Execute the method.
				int statusCode = client.executeMethod(method);

				if (statusCode != HttpStatus.SC_OK) {
					System.err.println("Method failed: " + method.getStatusLine());
					throw new LastFMServiceException("Error in the service: " + method.getStatusLine());
				}

				// Read the response body.
				byte[] responseBody = method.getResponseBody();
				JSONObject obj = new JSONObject(new String(responseBody));
				obj = obj.getJSONObject("artists");
				if (page++ == 1) {
					pages = obj.getJSONObject("@attr").getInt("totalPages");
				}

				JSONArray arr = obj.getJSONArray("artist");
				for (int i = 0; i < arr.length(); i++) {
					JSONObject artistObj = arr.getJSONObject(i);
					String mbid = artistObj.getString("name");

					int count = artistObj.getInt("playcount");
					JSONArray image = artistObj.getJSONArray("image");

					JSONObject bigImage = image.getJSONObject(2);
					linkedlist.add(new ArtistData(mbid, count, bigImage.getString("#text")));
				}


			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return linkedlist;
	}


	public static byte[] getUserList(String User, String weekly, int x, int y) throws LastFMServiceException {

		String url = BASE + GET_ALBUMS + User + API_KEY + ending + "&period=" + weekly;


		HttpClient client = new HttpClient();

		int requestedSize = x * y;
		int size = 0;
		int page = 1;
		while (size < requestedSize) {

			String urlPage = url + "&page=" + page;
			GetMethod method = new GetMethod(urlPage);
			++page;
			System.out.println(page + " :page             size: " + size);
			try {

				// Execute the method.
				int statusCode = client.executeMethod(method);

				if (statusCode != HttpStatus.SC_OK) {
					System.err.println("Method failed: " + method.getStatusLine());
					throw new LastFMServiceException("Error in the service: " + method.getStatusLine());
				}

				// Read the response body.
				byte[] responseBody = method.getResponseBody();
				JSONObject obj = new JSONObject(new String(responseBody));
				obj = obj.getJSONObject("topalbums");
				int limit = obj.getJSONObject("@attr").getInt("total");
				if (limit == size)
					break;
				JSONArray arr = obj.getJSONArray("album");
				for (int i = 0; i < arr.length() && size < requestedSize; i++) {
					JSONObject albumObj = arr.getJSONObject(i);
					JSONObject artistObj = albumObj.getJSONObject("artist");
					String albumName = albumObj.getString("name");
					String artistName = artistObj.getString("name");
					JSONArray image = albumObj.getJSONArray("image");
					JSONObject bigImage = image.getJSONObject(3);
					queue.add(new UrlCapsule(bigImage.getString("#text"), size, albumName, artistName));
					++size;
				}


			} catch (HttpException e) {
				System.err.println("Fatal protocol violation: " + e.getMessage());
				e.printStackTrace();
			} catch (JSONException | IOException e) {
				e.printStackTrace();
			} finally {
				// Release the connection.
				method.releaseConnection();
			}
		}
		byte[] img;
		//
		BufferedImage image = CollageMaker.generateCollageThreaded(Integer.max(1, size / x), Integer.max(1, (size / y)), queue);
		ByteArrayOutputStream b = new ByteArrayOutputStream();

		try {
			ImageIO.write(image, "png", b);
		} catch (IOException e) {
			e.printStackTrace();
		}

		img = b.toByteArray();
		// Deal with the response.
		// Use caution: ensure correct character encoding and is not binary data

		return img;
	}


}


