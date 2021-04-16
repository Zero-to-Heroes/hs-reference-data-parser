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
import java.io.*;
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
	private static final boolean FETCH_IMAGES = true;
	private static final boolean FORCE_REFETCH_IMAGES = true;
	private static final boolean ONLY_BGS = true;

	private static final String PYTHON_UNITYPACK_AUDIO_OUT_DIRE =
			"E:\\Source\\hearthsim\\python-unitypack\\out\\audio2";

	private static final Map<String, String> SET_CODES = buildSetCodes();
	// The exported info from hearthstonejson isn"t good
	private static final List<String> CARDS_TO_DOWNLOAD = Lists.newArrayList(
//		// 20.0
//		"YOP_015", "NEW1_012", "SCH_243",
//			"DAL_177", "DRG_322", "EX1_544", "CS2_084",
//			"CS2_237", "ULD_156", "CS2_103", "ULD_720",
//			"DRG_019", "KAR_063", "EX1_565", "DRG_248",
//			"DRG_217", "DRG_218", "EX1_166", "NEW1_008",
//			"CS2_004", "UNG_067", "EX1_613", "CS2_233",
//			"DRG_031", "DRG_600", "DRG_600t2", "DRG_620t3",
//			"LOOT_093", "DRG_250", "FP1_028", "EX1_029", "DRG_255t2",
//			"NEW1_019", "YOD_032", "DRG_071", "ICC_466",
//			"EX1_089", "ICC_705", "DAL_736", "DRG_089",
//		// 20.0.2
//			"DMF_108", "BAR_875", "SCH_351", "SCH_248",
//			"BAR_074", "BAR_076",
//			"TB_BaconShop_HP_037a", "TB_BaconShop_HP_075",
//			"BGS_037", "TB_BaconUps_107",
//			"BGS_017", "TB_BaconUps_086",
//			"BGS_078", "TB_BaconUps_135",
//			"BGS_032", "TB_BaconUps_103",
//			"BGS_126", "TB_BaconUps_166",
//			"BGS_124", "TB_BaconUps_163",
//			"BGS_100", "TB_BaconUps_200",
//			"BAR_721t",
//			"PVPDR_SCH_Roguet1", "PVPDR_BAR_Passive01", "PVPDR_DMF_Roguep1"
	);
	private static final List<String> CARD_IDS_TO_FIX = Lists.newArrayList(
	);

	private static Map<String,String> buildSetCodes() {
		Map<String, String> result = new HashMap<>();
		result.put("1129", "Troll");
		result.put("1403", "Yod");
		result.put("year_of_the_dragon", "Yod");
		result.put("1463", "Demon_hunter_initiate");
		result.put("expert1", "legacy");
		return result;
	}

	public static void main(String[] args) throws Exception {
		JSONParser parser = new JSONParser();

		BufferedReader referenceIn = new BufferedReader(new InputStreamReader(
				GenerateCardData.class.getResourceAsStream("cards.json"),
				"UTF-8"));
		JSONArray referenceCards = new JSONArray(((org.json.simple.JSONArray) parser.parse(referenceIn)).toJSONString());

		BufferedReader outOfCardsReferenceIn = new BufferedReader(new InputStreamReader(
				GenerateCardData.class.getResourceAsStream("out.of.cards.json"),
				"UTF-8"));
		JSONArray outOfCardsReferenceCards = new JSONArray(((org.json.simple.JSONArray) parser.parse(outOfCardsReferenceIn)).toJSONString());

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

		Set<String> existingImages = Arrays.stream(new File("E:\\hearthstone_images\\images").listFiles())
				.map(GenerateCardData::flattenDirectoryStructure).flatMap(List::stream)
				.map(GenerateCardData::flattenDirectoryStructure).flatMap(List::stream)
				.map(GenerateCardData::flattenDirectoryStructure).flatMap(List::stream)
				.map(File::getName)
				.collect(Collectors.toSet());

		Set<String> existingImagesGolden = Arrays.stream(new File("E:\\hearthstone_images\\imagesGolden").listFiles())
				.map(GenerateCardData::flattenDirectoryStructure).flatMap(List::stream)
				.map(GenerateCardData::flattenDirectoryStructure).flatMap(List::stream)
				.map(GenerateCardData::flattenDirectoryStructure).flatMap(List::stream)
				.map(File::getName)
				.collect(Collectors.toSet());

//		Set<String> existingImages512 = Arrays.stream(new File("E:\\hearthstone_images\\images512").listFiles())
//				.map(GenerateCardData::flattenDirectoryStructure).flatMap(List::stream)
//				.map(GenerateCardData::flattenDirectoryStructure).flatMap(List::stream)
//				.map(GenerateCardData::flattenDirectoryStructure).flatMap(List::stream)
//				.map(File::getName)
//				.collect(Collectors.toSet());

//		Set<String> existingTextures = Arrays.stream(new File("E:\\hearthstone_images\\textures").listFiles())
////				.map(GenerateCardData::flattenDirectoryStructure).flatMap(List::stream)
////				.map(GenerateCardData::flattenDirectoryStructure).flatMap(List::stream)
////				.map(GenerateCardData::flattenDirectoryStructure).flatMap(List::stream)
////				.map(File::getName)
////				.collect(Collectors.toSet());
////
////		Set<String> existingTiles = Arrays.stream(new File("E:\\hearthstone_images\\tiles").listFiles())
////				.map(GenerateCardData::flattenDirectoryStructure).flatMap(List::stream)
////				.map(GenerateCardData::flattenDirectoryStructure).flatMap(List::stream)
////				.map(GenerateCardData::flattenDirectoryStructure).flatMap(List::stream)
////				.map(File::getName)
////				.collect(Collectors.toSet());
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
//								System.err.println("Audio file not present: " + audioFilename + " for " + id);
							}
						}
					}
				}

				card.remove("targetingArrowText");
				card.remove("howToEarn");
				card.remove("howToEarnGolden");

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
					if (card.get("cardClass").equals(14)) {
						card.put("playerClass", "DEMONHUNTER");
						card.put("cardClass", "DEMONHUNTER");
					} else {
						card.put("playerClass", WordUtils.capitalizeFully(card.getString("cardClass")));
					}
				}
				if (card.has("playerClass")) {
					card.put("playerClass", WordUtils.capitalizeFully(card.getString("playerClass")));
				}
				if (card.has("rarity")) {
					card.put("rarity", WordUtils.capitalizeFully(card.getString("rarity")));
				}
				if (card.has("set")) {
					String set = String.valueOf(card.get("set"));
					// TODO: issue with Year_of_the_dragon (not replaced by Yod in output)
					if (SET_CODES.containsKey(set.toLowerCase())) {
						set = SET_CODES.get(set.toLowerCase());
					}
					if (set.equalsIgnoreCase("darkmoon_faire") && id.startsWith("YOP")) {
						set = "darkmoon_races";
					}
					card.put("set", WordUtils.capitalizeFully(set));
				} else if (card.has("collectible") && card.getBoolean("collectible") && "Free".equals(card.getString("rarity"))) {
					String set = "basic";
					card.put("set", WordUtils.capitalizeFully(set));
				} else if (card.has("collectible") && card.getBoolean("collectible")) {
					String set = "hof";
					card.put("set", WordUtils.capitalizeFully(set));
				}

				if (card.has("type") && card.get("type") instanceof String) {
					card.put("type", WordUtils.capitalizeFully(card.getString("type")));
				}

				// Now handle images
				if (!FETCH_IMAGES) {
					continue;
				}
				if (CARDS_TO_DOWNLOAD.size() > 0 && !CARDS_TO_DOWNLOAD.contains(id)) {
					continue;
				}
				System.out.println("Processing " + id);

				String imageName = id + ".png";
				String animatedImageName = id + ".webm";
				String texture = id + ".jpg";
				JSONObject outOfCardsRefCard = getOutOfCardReference(outOfCardsReferenceCards, id);
				if (!ONLY_BGS && !FORCE_REFETCH_IMAGES && existingImages.contains(imageName)
						&& existingImagesGolden.contains(animatedImageName)
//						&& existingImages512.contains(imageName)
//						&& existingTextures.contains(texture)
//						&& existingTiles.contains(texture)
						) {
					System.out.println("File exists: " + imageName);
					continue;
				}

				try {
					// Download the card
					if (!ONLY_BGS && (FORCE_REFETCH_IMAGES || !existingImages.contains(imageName))) {
						System.out.println("Downloading card " + imageName);
						InputStream in = getInputStream(card);
						Files.copy(in, Paths.get("E:\\hearthstone_images\\images/" + imageName));
						long imageSize = Files.size(Paths.get("E:\\hearthstone_images\\images/" + imageName));
						if (imageSize > 0) {
							in.close();
							System.out.println("Downloaded card for " + id);
						}
						else {
							Paths.get("E:\\hearthstone_images\\images/" + imageName).toFile().delete();
							System.out.println("Empty image: " + imageName);
						}
					}
				} catch (FileAlreadyExistsException e) {
				} catch (Exception e) {
					System.err.println("Could not find image! " + e.getMessage());
				}

				// BG cards
				try {
					// Download the card
					String bgsImageName = card.has("battlegroundsPremiumDbfId") ? id + "_bgs.png" : id + "_bgs_premium.png";
					if ((card.has("battlegroundsPremiumDbfId") || card.has("battlegroundsNormalDbfId"))
							&& (FORCE_REFETCH_IMAGES || !existingImages.contains(bgsImageName))) {
						InputStream in = getInputStreamBgs(card);
						Files.copy(in, Paths.get("E:\\hearthstone_images\\images/bgs/" + bgsImageName));
						long imageSize = Files.size(Paths.get("E:\\hearthstone_images\\images/bgs/" + bgsImageName));
						if (imageSize > 0) {
							in.close();
							System.out.println("Downloaded card for " + id);
						}
						else {
							Paths.get("E:\\hearthstone_images\\images/bgs/" + bgsImageName).toFile().delete();
							System.out.println("Empty image: " + bgsImageName);
						}
					}
				} catch (FileAlreadyExistsException e) {
				} catch (Exception e) {
					System.err.println("Could not find bgs image! " + e.getMessage());
				}

//				try {
//					// Download the texture
//					if (!ONLY_BGS && (FORCE_REFETCH_IMAGES || !existingTextures.contains(texture))) {
//						InputStream in = getInputStreamTexture(card);
//						Files.copy(in, Paths.get("E:\\hearthstone_images\\textures/" + texture));
//						long imageSize = Files.size(Paths.get("E:\\hearthstone_images\\textures/" + texture));
//						if (imageSize > 0) {
//							in.close();
//							System.out.println("Downloaded texture for " + id);
//						}
//						else {
//							Paths.get("E:\\hearthstone_images\\textures/" + texture).toFile().delete();
//							System.out.println("Empty textures: " + texture);
//						}
//					}
//				}catch (FileAlreadyExistsException e) {
//				} catch (Exception e) {
//					System.err.println("Could not find textures! " + e.getMessage());
//				}
//				try {
//					// Download the texture
//					if (!ONLY_BGS && (FORCE_REFETCH_IMAGES || !existingTiles.contains(texture))) {
//						InputStream in = getInputStreamTile(card);
//						Files.copy(in, Paths.get("E:\\hearthstone_images\\tiles/" + texture));
//						long imageSize = Files.size(Paths.get("E:\\hearthstone_images\\tiles/" + texture));
//						if (imageSize > 0) {
//							in.close();
//							System.out.println("Downloaded tile for " + id);
//						}
//						else {
//							Paths.get("E:\\hearthstone_images\\tiles/" + texture).toFile().delete();
//							System.out.println("Empty tile: " + texture);
//						}
//					}
//				}catch (FileAlreadyExistsException e) {
//				} catch (Exception e) {
//					System.err.println("Could not find tile! " + e.getMessage());
//				}

//				try {
//					if (!ONLY_BGS && (FORCE_REFETCH_IMAGES || !existingImages512.contains(imageName))) {
//						InputStream in = getInputStream512(card);
//						Files.copy(in, Paths.get("E:\\hearthstone_images\\images512/" + imageName));
//						long imageSize = Files.size(Paths.get("E:\\hearthstone_images\\images512/" + imageName));
//						if (imageSize > 0) {
//							in.close();
//							System.out.println("Downloaded 512 card for " + id);
//						}
//						else {
//							Paths.get("E:\\hearthstone_images\\images512/" + imageName).toFile().delete();
//							System.out.println("Empty 512 image: " + imageName);
//						}
//					}
//				} catch (FileAlreadyExistsException e) {
//				} catch (Exception e) {
//					System.err.println("Could not find 512 image! " + e.getMessage());
//				}

				try {
					if (!ONLY_BGS && (FORCE_REFETCH_IMAGES || !existingImagesGolden.contains(animatedImageName))) {
						InputStream in = getInputStreamAnimated(outOfCardsRefCard);
						Files.copy(in, Paths.get("E:\\hearthstone_images\\imagesGolden/" + animatedImageName));
						long imageSize = Files.size(Paths.get("E:\\hearthstone_images\\imagesGolden/" + animatedImageName));
						if (imageSize > 0) {
							in.close();
							System.out.println("Downloaded animated card for " + id);
						}
						else {
							Paths.get("E:\\hearthstone_images\\imagesGolden/" + animatedImageName).toFile().delete();
							System.out.println("Empty animated image: " + animatedImageName);
						}
					}
				} catch (FileAlreadyExistsException e) {
				} catch (Exception e) {
					System.err.println("Could not find animated image for  " + id + ": " + e.getMessage());
				}
//				System.out.println(referenceCards);
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Issue processing card " + card.getString("id"));
		}

		File file = new File("./out.json");
		FileWriter writer = new FileWriter(file);
		writer.append(referenceCards.toString());
		writer.close();
		System.out.println("finished processing cards");
//		System.out.println(referenceCards);
	}

	private static JSONObject getOutOfCardReference(JSONArray outOfCardsReferenceCards, String id) {
		for (Object obj : outOfCardsReferenceCards) {
			if (obj instanceof JSONObject && ((JSONObject)obj).getString("id_string").equals(id)) {
				return (JSONObject)obj;
			}
		}
		return null;
	}

	private static List<File> flattenDirectoryStructure(File file) {
		return file.isDirectory() ? Arrays.asList(file.listFiles()) : Lists.newArrayList(file);
	}

	private static InputStream getInputStreamTexture(JSONObject card) throws Exception {
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
		URL url = new URL("https://art.hearthstonejson.com/v1/256x/" + card.getString("id") + ".jpg");
		return url.openStream();
	}

	private static InputStream getInputStreamTile(JSONObject card) throws Exception {
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
		URL url = new URL("https://art.hearthstonejson.com/v1/tiles/" + card.getString("id") + ".jpg");
		return url.openStream();
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
		URL url = new URL("https://cards.hearthpwn.com/enUS/" + card.getString("id") + ".png");
		return url.openStream();
	}


	private static InputStream getInputStreamBgs(JSONObject card) throws Exception {
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
		if (card.has("battlegroundsPremiumDbfId")) {
			URL url = new URL("https://cards.hearthpwn.com/enUS/bgs/" + card.getString("id") + "_bg.png");
			return url.openStream();
		} else {
			URL url = new URL("https://cards.hearthpwn.com/enUS/anims/" + card.getString("id") + "_premium_000.png");
			return url.openStream();
		}
	}



	private static InputStream getInputStreamAnimated(JSONObject outOfCard) throws Exception {
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
		System.out.println("Fetching golden image " + outOfCard.get("image_golden_url"));
		URL url = new URL(outOfCard.getString("image_golden_url"));
		return url.openStream();
	}


//	private static InputStream getInputStream512(JSONObject card) throws Exception {
//		// Create a new trust manager that trust all certificates
//		TrustManager[] trustAllCerts = new TrustManager[]{
//				new X509TrustManager() {
//					public java.security.cert.X509Certificate[] getAcceptedIssuers() {
//						return null;
//					}
//					public void checkClientTrusted(
//							java.security.cert.X509Certificate[] certs, String authType) {
//					}
//					public void checkServerTrusted(
//							java.security.cert.X509Certificate[] certs, String authType) {
//					}
//				}
//		};
//
//		// Activate the new trust manager
//		try {
//			SSLContext sc = SSLContext.getInstance("SSL");
//			sc.init(null, trustAllCerts, new java.security.SecureRandom());
//			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
//		} catch (Exception e) {
//			System.err.println("Caught exception " + e.getMessage());
//		}
//
//		// And as before now you can use URL and URLConnection
//		System.setProperty("http.agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:66.0) Gecko/20100101 Firefox/66.0");
//		URL url = new URL("https://art.hearthstonejson.com/v1/render/latest/enUS/512x/" + card.getString("id") + ".png");
//		return url.openStream();
//	}

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
