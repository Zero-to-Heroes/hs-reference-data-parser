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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class GenerateCardData {

	private static final boolean FETCH_IMAGES = false;
	private static final String PYTHON_UNITYPACK_AUDIO_OUT_DIRE = "G:\\Source\\hearthsim\\python-unitypack\\out\\audio2";
	private static final Map<String, String> SET_CODES = buildSetCodes();
	// The exported info from hearthstonejson isn"t good
	private static final List<String> CARD_IDS_TO_FIX = Lists.newArrayList(
//			"DAL_357t", // Spirit of Lucentbark
//			"DALA_BOSS_07p", // Take Flight!
//			"DALA_BOSS_07p2", // Flying!
//			"DALA_BOSS_45p", // Ray of Suffering
//			"DALA_BOSS_45px", // Ray of Suffering
//			"DALA_BOSS_69p", // Dragonwrath
//			"DALA_BOSS_69px", // Dragonwrath
//			"FB_LK005", // Remorseless Winter
//			"GILA_601", // Cannon
//			"ICCA08_030p", // Remorseless Winter
//			"DAL_007", // Rafaam"s Scheme
//			"DAL_008", // Dr Boom"s Scheme
//			"DAL_009", // Hagatha"s Scheme
//			"DAL_010", // Togwaggle"s Scheme
//			"DAL_011", // Lazul"s Scheme
//			"ULDA_204", // Reno's Magical Torch
//			"ULDA_207", // The Gatling Wand
//			"ULDA_302" // Academic Research
	);

	private static Map<String,String> buildSetCodes() {
		Map<String, String> result = new HashMap<>();
		result.put("1129", "Troll");
		return result;
	}

	public static void main(String[] args) throws Exception {
		JSONParser parser = new JSONParser();
		BufferedReader referenceIn = new BufferedReader(new InputStreamReader(
				GenerateCardData.class.getResourceAsStream("cards.json"),
				"UTF-8"));
		JSONArray referenceCards = new JSONArray(((org.json.simple.JSONArray) parser.parse(referenceIn)).toJSONString());

		BufferedReader soundEffectsIn = new BufferedReader(new InputStreamReader(
				GenerateCardData.class.getResourceAsStream("sound_effects.json"),
				"UTF-8"));
		JSONObject soundEffects = new JSONObject(((org.json.simple.JSONObject) parser.parse(soundEffectsIn)).toJSONString());

		// Build the list of all audio files
		List<String> audioClips = Arrays.stream(new File(PYTHON_UNITYPACK_AUDIO_OUT_DIRE).listFiles())
                .filter(File::isFile)
                .map(File::getName)
                .collect(Collectors.toList());
        System.out.println("Total audio clips: " + audioClips.size());

		Set<String> existingImages = Arrays.stream(new File("images").listFiles())
				.map(GenerateCardData::flattenDirectoryStructure).flatMap(List::stream)
				.map(GenerateCardData::flattenDirectoryStructure).flatMap(List::stream)
				.map(GenerateCardData::flattenDirectoryStructure).flatMap(List::stream)
				.map(File::getName)
				.collect(Collectors.toSet());

		Set<String> existingImages512 = Arrays.stream(new File("images512").listFiles())
				.map(GenerateCardData::flattenDirectoryStructure).flatMap(List::stream)
				.map(GenerateCardData::flattenDirectoryStructure).flatMap(List::stream)
				.map(GenerateCardData::flattenDirectoryStructure).flatMap(List::stream)
				.map(File::getName)
				.collect(Collectors.toSet());
//        Set<String> existingImages = new HashSet<>();

		System.out.println("init done " + referenceCards.length() + ", " + soundEffects.length() + ", " + audioClips.size());
		JSONObject card = null;
		try {
			for (Object cardObject : referenceCards) {
				card = (JSONObject) cardObject;
				String id = card.getString("id");
				if (!card.has("name")) {
					continue;
				}
				JSONObject nameLoc = card.getJSONObject("name");
				card.remove("name");
				card.put("name", nameLoc.getString("enUS"));

				if (soundEffects.has(id)) {
					card.put("audio", soundEffects.getJSONObject(id));
					for (String key : soundEffects.getJSONObject(id).keySet()) {
						for (int i = 0; i < soundEffects.getJSONObject(id).getJSONArray(key).length(); i++) {
							String audioFilename = (String) soundEffects.getJSONObject(id).getJSONArray(key).get(i);
							if (!audioClips.contains(audioFilename)) {
								System.err.println("Audio file not present: " + audioFilename + " for " + id);
							}
						}
					}
				}

				card.remove("targetingArrowText");

				JSONObject originalCollectionText = (JSONObject) card.remove("collectionText");
				if (originalCollectionText != null && originalCollectionText.has("enUS")) {
					card.put("collectionText", sanitize(originalCollectionText.getString("enUS")));
				}

				JSONObject originalText = (JSONObject) card.remove("text");
				if (originalText != null && originalText.has("enUS")) {
					card.put("text", fixText(card, originalText.getString("enUS")));
				}

				if (card.has("flavor")) {
					card.put("flavor", sanitize(card.getJSONObject("flavor").getString("enUS")));
				}

				if (card.has("race")) {
					String newRace = card.getString("race").equals("MECHANICAL") ? "MECH" : card.getString("race");
					card.put("race", newRace);
				}

				if (card.has("cardClass")) {
					card.put("playerClass", WordUtils.capitalizeFully(card.getString("cardClass")));
				}
				if (card.has("playerClass")) {
					card.put("playerClass", WordUtils.capitalizeFully(card.getString("playerClass")));
				}
				if (card.has("rarity")) {
					card.put("rarity", WordUtils.capitalizeFully(card.getString("rarity")));
				}
				if (card.has("set")) {
					String set = String.valueOf(card.get("set"));
					if (SET_CODES.containsKey(set)) {
						set = SET_CODES.get(set);
					}
					card.put("set", WordUtils.capitalizeFully(set));
				}
				if (card.has("type")) {
					card.put("type", WordUtils.capitalizeFully(card.getString("type")));
				}

				// Now handle images
				if (!FETCH_IMAGES) {
					continue;
				}
//				if (!id.startsWith("DAL")) {
//					continue;
//				}

				String imageName = id + ".png";
				if (existingImages.contains(imageName) && existingImages512.contains(imageName)) {
					System.out.println("File exists: " + imageName);
					card.put("cardImage", imageName);
					continue;
				}
				System.out.println("File doesn't exist, moving on: " + imageName);
				try {
					// Download the card
					if (!existingImages.contains(imageName)) {
						InputStream in = getInputStream(card);
						Files.copy(in, Paths.get("images/" + imageName));
						long imageSize = Files.size(Paths.get("images/" + imageName));
						if (imageSize > 0) {
							// Update the card
							card.put("cardImage", imageName);
							in.close();
							System.out.println("Downloaded card for " + id);
						}
						else {
							Paths.get("images/" + imageName).toFile().delete();
							System.out.println("Empty image: " + imageName);
						}
					}
				} catch (FileAlreadyExistsException e) {
					card.put("cardImage", imageName);
				} catch (Exception e) {
					System.err.println("Could not find image! " + e.getMessage());
				}
				try {
					if (!existingImages512.contains(imageName)) {
						InputStream in = getInputStream512(card);
						Files.copy(in, Paths.get("images512/" + imageName));
						long imageSize = Files.size(Paths.get("images512/" + imageName));
						if (imageSize > 0) {
							// Update the card
							card.put("cardImage", imageName);
							in.close();
							System.out.println("Downloaded 512 card for " + id);
						}
						else {
							Paths.get("images512/" + imageName).toFile().delete();
							System.out.println("Empty 512 image: " + imageName);
						}
					}
				} catch (FileAlreadyExistsException e) {
					card.put("cardImage", imageName);
				} catch (Exception e) {
					System.err.println("Could not find 512 image! " + e.getMessage());
				}
				System.out.println(referenceCards);
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Issue processing card " + card);
		}
		System.out.println("finished processing cards");
		System.out.println(referenceCards);
	}

	private static List<File> flattenDirectoryStructure(File file) {
		return file.isDirectory() ? Arrays.asList(file.listFiles()) : Lists.newArrayList(file);
	}

	private static InputStream getInputStream(JSONObject card) throws Exception {
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
		URL url = new URL("https://art.hearthstonejson.com/v1/render/latest/enUS/256x/" + card.getString("id") + ".png");
		return url.openStream();
	}

	private static InputStream getInputStream512(JSONObject card) throws Exception {
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
		URL url = new URL("https://art.hearthstonejson.com/v1/render/latest/enUS/512x/" + card.getString("id") + ".png");
		return url.openStream();
	}

	private static String fixText(JSONObject card, String text) {
		String newText = sanitize(text);
		if (!CARD_IDS_TO_FIX.contains(card.getString("id"))) {
			return newText;
		}
		return newText + " @" +  sanitize(card.getString("collectionText"));
	}

	private static String sanitize(String text) {
		return text.replaceAll("\u00A0", " ");
	}
}
