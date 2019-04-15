package hearthstoneparser;

import com.github.slugify.Slugify;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.simple.parser.JSONParser;

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
	private static final String PYTHON_UNITYPACK_AUDIO_OUT_DIRE = "G:\\Source\\hearthsim\\python-unitypack\\out\\audio";
	private static final Map<String, String> SET_CODES = buildSetCodes();

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

				JSONObject originalText = (JSONObject) card.remove("text");
				if (originalText != null && originalText.has("enUS")) {
					card.put("text", originalText.getString("enUS"));
				}

				if (card.has("flavor")) {
					card.put("flavor", card.getJSONObject("flavor").getString("enUS"));
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
				if (!id.startsWith("DAL")) {
					continue;
				}

				String imageName = id + ".png";
				try {
					if (existingImages.contains(imageName)) {
						System.out.println("File exists: " + imageName);
						card.put("cardImage", imageName);
						continue;
					}
					System.out.println("File doesn't exist, moving on: " + imageName);
					// Download the card
					InputStream in = getInputStream(card);
					Files.copy(in, Paths.get("images/en/" + imageName));
					long imageSize = Files.size(Paths.get("images/en/" + imageName));
					if (imageSize > 0) {
						// Update the card
						card.put("cardImage", imageName);
						in.close();
						System.out.println("Downloaded card for " + id);
					}
					else {
						Paths.get("images/en/" + imageName).toFile().delete();
						System.out.println("Empty image: " +imageName);
					}
				}
				catch (FileAlreadyExistsException e) {
//					 System.out.println("\tImage already exists for: " + id);
					 card.put("cardImage", imageName);
				}
				catch (Exception e) {
//					System.out.println("Image does not exist " + imageName + " at path " + e.getMessage());
					if (imageName.startsWith("DAL")) {
						System.err.println("Could not find Dalaran image! " + e.getMessage());
					}
					// e.printStackTrace();
				}

//				imageName = id + ".gif";
//				spec = "http://media.services.zam.com/v1/media/byName/hs/cards/enus/animated/" + id
//							+ "_premium.gif" + "?12576";
//				try {
//					if (Paths.get("images/golden/" + id + ".gif").toFile().exists()) {
//						throw new FileAlreadyExistsException(Paths.get("images/golden/" + id + ".gif").toString());
//					}
//					if (Paths.get("images/golden/old/" + id + ".gif").toFile().exists()) {
//						throw new FileAlreadyExistsException(Paths.get("images/golden/old/" + id + ".gif").toString());
//					}
//					if (Paths.get("images/golden/new/" + id + ".gif").toFile().exists()) {
//						throw new FileAlreadyExistsException(Paths.get("images/golden/new" + id + ".gif").toString());
//					}
//					if (Paths.get("images/golden/new_UNG/" + id + ".gif").toFile().exists()) {
//						throw new FileAlreadyExistsException(Paths.get("images/golden/new_UNG" + id + ".gif").toString());
//					}
//					// Get the golden image
//					// Download the card
//					URL url = new URL(spec);
//					InputStream in = url.openStream();
//					Files.copy(in, Paths.get("images/golden/" + id + ".gif"));
//					long imageSize = Files.size(Paths.get("images/golden/" + id + ".gif"));
//					if (imageSize > 0) {
//						// Update the card
//						card.put("goldenImage", id + ".gif");
//						goldImageMap.put(id, id + ".gif");
//						System.out.println("Golden image downloaded for " + id);
//					}
//					else {
//						Paths.get("images/golden/" + id + ".gif").toFile().delete();
//						System.out.println("\tEmpty image, skipping");
//					}
//					in.close();
//				}
//				catch (FileAlreadyExistsException e) {
//					// Update the card
//					 System.out.println("\tGolden image already exists for: " + id);
//					card.put("goldenImage", id + ".gif");
//					goldImageMap.put(id, id + ".gif");
//				}
//				catch (Exception e) {
//					System.out.println("Golden mage does not exist " + imageName + " at path " + spec);
//					// e.printStackTrace();
//				}
			}
//			System.out.println(imageMap);
//			System.out.println(goldImageMap);
//			System.out.println(cardsArtMap);

//			System.out.println(audioClips);
			System.out.println(referenceCards);
		}
		catch (Exception e) {
			e.printStackTrace();
			System.err.println("Issue processing card " + card);
		}
		System.out.println("finished processing cards");
	}

	private static List<File> flattenDirectoryStructure(File file) {
		return file.isDirectory() ? Arrays.asList(file.listFiles()) : Lists.newArrayList(file);
	}

	/** Try various combinations to download the image */
	private static InputStream getInputStream(JSONObject card) throws Exception {
		List<String> possibleSpecs = Lists.newArrayList(
				"2018/11/",
				"2018/10/",
				"2018/08/",
				"2019/04/",
				"2019/03/"
				);
		String baseCardName = new Slugify().slugify(card.getString("name")
				.replaceAll("'", "")
				.replaceAll("\\.", ""));
		List<String> possibleCardNames = Lists.newArrayList(
				baseCardName,
				baseCardName + "-300x414",
				baseCardName + "-card-art-300x429",
				baseCardName + "-temp-card-art-300x429",
				StringUtils.capitalize(baseCardName),
				WordUtils.capitalize(baseCardName, new char[] {'-'}));
		for (String cardName : possibleCardNames) {
			for (String possibleSpec : possibleSpecs) {
				String spec = "https://www.hearthstonetopdecks.com/wp-content/uploads/"
						+ possibleSpec
						+ cardName
						+ ".png";
				try {
					return getInputStream(spec);
				}
				catch (Exception e) {
					// Image is not at correct location, trying something else
				}
			}
		}
		throw new Exception("Could not find image for card " + card.getString("id")
				+ " with slugified name: " + possibleCardNames);
	}

	private static InputStream getInputStream(String spec) throws Exception {
		URL url = new URL(spec);
		return url.openStream();
	}
}
