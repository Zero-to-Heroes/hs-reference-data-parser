package hearthstoneparser;

import com.google.common.collect.Lists;
import org.apache.commons.lang.WordUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.simple.parser.JSONParser;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class GenerateCardBackImages {
	public static void main(String[] args) throws Exception {
		JSONParser parser = new JSONParser();
		BufferedReader referenceIn = new BufferedReader(new InputStreamReader(
				GenerateCardBackImages.class.getResourceAsStream("card-backs.json"),
				"UTF-8"));
		JSONArray referenceCards = new JSONArray(((org.json.simple.JSONArray) parser.parse(referenceIn)).toJSONString());

		Set<String> existingImages = Arrays.stream(new File("E:\\hearthstone_images_card_backs").listFiles())
				.map(GenerateCardBackImages::flattenDirectoryStructure).flatMap(List::stream)
				.map(GenerateCardBackImages::flattenDirectoryStructure).flatMap(List::stream)
				.map(GenerateCardBackImages::flattenDirectoryStructure).flatMap(List::stream)
				.map(File::getName)
				.collect(Collectors.toSet());

		System.out.println("init done " + referenceCards.length());
		JSONObject card = null;
		try {
			for (Object cardObject : referenceCards) {
				card = (JSONObject) cardObject;
				int id = card.getInt("id");
				String image = card.getString("image");
				System.out.println("Processing " + id);

				String imageName = id + ".png";
				if (existingImages.contains(imageName)) {
					System.out.println("File exists: " + imageName);
					continue;
				}

				try {
					// Download the card
					InputStream in = getInputStream(image);
					Files.copy(in, Paths.get("E:\\hearthstone_images_card_backs/" + imageName));
					long imageSize = Files.size(Paths.get("E:\\hearthstone_images_card_backs/" + imageName));
					if (imageSize > 0) {
						in.close();
						System.out.println("Downloaded card for " + id);
					}
					else {
						Paths.get("E:\\hearthstone_images_card_backs/" + imageName).toFile().delete();
						System.out.println("Empty image: " + imageName);
					}
				} catch (FileAlreadyExistsException e) {
				} catch (Exception e) {
					System.err.println("Could not find image! " + e.getMessage());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Issue processing card " + card.getString("id"));
		}
		System.out.println("finished processing cards");
	}

	private static List<File> flattenDirectoryStructure(File file) {
		return file.isDirectory() ? Arrays.asList(file.listFiles()) : Lists.newArrayList(file);
	}

	private static InputStream getInputStream(String path) throws Exception {
		// Create a new trust manager that trust all certificates
		TrustManager[] trustAllCerts = new TrustManager[]{
				new X509TrustManager() {
					public java.security.cert.X509Certificate[] getAcceptedIssuers() {
						return null;
					}
					public void checkClientTrusted(
							java.security.cert.X509Certificate[] certs, String authType) {
					}
					public void checkServerTrusted(
							java.security.cert.X509Certificate[] certs, String authType) {
					}
				}
		};

		// Activate the new trust manager
		try {
			SSLContext sc = SSLContext.getInstance("SSL");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		} catch (Exception e) {
			System.err.println("Caught exception " + e.getMessage());
		}

		// And as before now you can use URL and URLConnection
		System.setProperty("http.agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:66.0) Gecko/20100101 Firefox/66.0");
		URL url = new URL(path);
		return url.openStream();
	}
}
