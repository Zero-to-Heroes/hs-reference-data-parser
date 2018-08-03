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
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class GenerateCardData {

	public static void main(String[] args) throws Exception {
		JSONParser parser = new JSONParser();

		URL referenceUrl = new URL("https://s3-us-west-2.amazonaws.com/static.zerotoheroes.com/hearthstone/jsoncards/25770/all/cards.json");
		BufferedReader referenceIn = new BufferedReader(new InputStreamReader(referenceUrl.openStream(), "UTF-8"));
		JSONArray referenceCards = new JSONArray(((org.json.simple.JSONArray) parser.parse(referenceIn)).toJSONString());

		// Build the list of all audio files
        List<String> audioClips = Arrays.stream(new File("D:\\Dev\\Projects\\HearthSim\\python-unitypack\\out\\audio").listFiles())
                .filter(File::isFile)
                .map(File::getName)
                .filter(clip -> clip.startsWith("VO_"))
                .collect(Collectors.toList());
//        System.out.println(audioClips);
		Pattern audioFilePattern = Pattern.compile("VO_(?:.*)_(.*)_(.*)\\.ogg", Pattern.CASE_INSENSITIVE);

		List<String> possiblePaths = Lists.newArrayList(
				"images/en/old/",
				"images/en/new/",
				"images/en/new_LOOT/",
				"images/en/new_KFT/",
				"images/en/new_UNG/",
				"images/en/BOT/"
		);

		FileWriter cardsWriter = new FileWriter("out_cardsWithImages.json", false);
		System.out.println("init done " + referenceCards.length());
		JSONObject card = null;
		try {

			for (Object cardObject : referenceCards) {
				card = (JSONObject) cardObject;
				String id = card.getString("id");
				if (!card.has("name")) {
					continue;
				}
//				System.out.println("Considering card: " + card.getString("id") + " - " + card.getJSONObject("name").getString("enUS") + " " + cardObject);
				JSONObject nameLoc = card.getJSONObject("name");
				card.remove("name");
				card.put("name", nameLoc.getString("enUS"));

				JSONObject audio = new JSONObject();
                for (Iterator<String> iter = audioClips.iterator(); iter.hasNext(); ) {
                    String fileName = iter.next();
                    if (fileName.contains(id)) {
//						System.out.println("Matching " + fileName);
						Matcher matcher = audioFilePattern.matcher(fileName);
						matcher.find();
						audio.put(
								StringUtils.capitalize(matcher.group(1).toLowerCase())
										+ "_"
										+ StringUtils.capitalize(matcher.group(2).toLowerCase()),
								fileName);
                        iter.remove();
                    }
                }
                if (audio.length() > 0) {
					card.put("audio", audio);
				}

				JSONObject originalText = (JSONObject) card.remove("text");
				if (originalText != null && originalText.has("enUS")) {
					// frLocalization.put("text",
					// originalText.getString("frFR"));
					card.put("text", originalText.getString("enUS"));
				}

				if (card.has("flavor")) {
					card.put("flavor", card.getJSONObject("flavor").getString("enUS"));
				}
				// Remove all the big chunks of useless / localization data
				card.remove("howToEarn");
				card.remove("howToEarnGolden");
				card.remove("playRequirements");
				card.remove("texture");
				card.remove("collectionText");
				card.remove("targetingArrowText");
				card.remove("textInPlay");

				// And capitalize the data that is now in full uppercase
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
					card.put("set", WordUtils.capitalizeFully(String.valueOf(card.get("set"))));
				}
				if (card.has("type")) {
					card.put("type", WordUtils.capitalizeFully(card.getString("type")));
				}

				// Now handle images
				String imageName = id + ".png";
//				String spec = "http://media.services.zam.com/v1/media/byName/hs/cards/enus/" + imageName + "?12576";

				try {
					for (String possiblePath : possiblePaths) {
						if (Paths.get(possiblePath + id + ".png").toFile().exists()) {
							throw new FileAlreadyExistsException(Paths.get(possiblePath + id + ".png").toString());
						}
					}
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
					if (imageName.startsWith("BOT")) {
						System.err.println("Could not find Boomsday image! " + e.getMessage());
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
				cardsWriter.write(cardObject.toString() + ",");
			}
//			System.out.println(imageMap);
//			System.out.println(goldImageMap);
//			System.out.println(cardsArtMap);

			System.out.println(audioClips);
			System.out.println(referenceCards);

			cardsWriter.close();

		}
		catch (Exception e) {
			e.printStackTrace();
			System.err.println("Issue processing card " + card);
		}
		System.out.println("finished processing cards");
	}

	/** Try various combinations to download the image */
	private static InputStream getInputStream(JSONObject card) throws Exception {
		List<String> possibleSpecs = Lists.newArrayList(
				"2018/08/",
				"2018/07/"
		);
		String baseCardName = new Slugify().slugify(card.getString("name")
				.replaceAll("'", "")
				.replaceAll("\\.", ""));
		List<String> possibleCardNames = Lists.newArrayList(
				baseCardName,
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
