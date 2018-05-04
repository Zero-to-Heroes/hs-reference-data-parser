package hearthstoneparser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.simple.parser.JSONParser;

public class GenerateCardData {

	public static void main(String[] args) throws Exception {
		JSONParser parser = new JSONParser();

		URL referenceUrl = new URL("https://s3.amazonaws.com/com.zerotoheroes/plugins/hearthstone/cardsjson/23966/all/cards.json");
		BufferedReader referenceIn = new BufferedReader(new InputStreamReader(referenceUrl.openStream(), "UTF-8"));
		JSONArray referenceCards = new JSONArray(((org.json.simple.JSONArray) parser.parse(referenceIn)).toJSONString());

		JSONObject imageMap = new JSONObject(
				((org.json.simple.JSONObject) parser.parse(new FileReader("ref_imageMap.json"))).toJSONString());
		JSONObject goldImageMap = new JSONObject(
				((org.json.simple.JSONObject) parser.parse(new FileReader("ref_goldImageMap.json"))).toJSONString());
		JSONObject cardsArtMap = new JSONObject(
				((org.json.simple.JSONObject) parser.parse(new FileReader("ref_cardsArtMap.json"))).toJSONString());

		// Build the list of all audio files
        List<String> audioClips = Arrays.stream(new File("D:\\Dev\\Projects\\HearthSim\\python-unitypack\\out\\audio").listFiles())
                .filter(File::isFile)
                .map(File::getName)
                .filter(clip -> clip.startsWith("VO_"))
                .collect(Collectors.toList());
        System.out.println(audioClips);
		Pattern audioFilePattern = Pattern.compile("VO_(?:.*)_(.*)_(.*)\\.ogg", Pattern.CASE_INSENSITIVE);

		FileWriter cardsWriter = new FileWriter("out_cardsWithImages.json", false);
		System.out.println("init done " + referenceCards.length());
		JSONObject card = null;
		try {

			for (Object cardObject : referenceCards) {
				card = (JSONObject) cardObject;
				if (!card.has("name")) {
					continue;
				}
				System.out.println("Considering card: " + card.getString("id") + " - " + card.getJSONObject("name").getString("enUS") + " " + cardObject);
				JSONObject nameLoc = card.getJSONObject("name");
				card.remove("name");
				String id = card.getString("id");

				JSONObject audio = new JSONObject();
                for (Iterator<String> iter = audioClips.iterator(); iter.hasNext(); ) {
                    String fileName = iter.next();
                    if (fileName.contains(id)) {
						System.out.println("Matching " + fileName);
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
//                System.out.println("Added audio: " + audio);

				// Put the localization info
				// JSONObject frLocalization = new JSONObject();
				// frLocalization.put("name", nameLoc.getString("frFR"));
				// card.put("fr", frLocalization);
				card.put("name", nameLoc.getString("enUS"));

				JSONObject originalText = (JSONObject) card.remove("text");
				if (originalText != null && originalText.has("enUS")) {
					// frLocalization.put("text",
					// originalText.getString("frFR"));
					card.put("text", originalText.getString("enUS"));
				}

				// Remove all the big chunks of useless / localization data
				card.remove("howToEarn");
				card.remove("howToEarnGolden");
				card.remove("playRequirements");
				card.remove("texture");
				card.remove("dust");
//				card.remove("race");
//				card.remove("mechanics");
				card.remove("collectionText");
				card.remove("targetingArrowText");
				card.remove("flavor");
				card.remove("textInPlay");
//				card.remove("entourage");

				// And capitalize the data that is now in full uppercase
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
				String spec = "http://media.services.zam.com/v1/media/byName/hs/cards/enus/" + imageName + "?12576";

//				if (!imageMap.has(id)) {
				try {
					if (Paths.get("images/en/old/" + id + ".png").toFile().exists()) {
						throw new FileAlreadyExistsException(Paths.get("images/old/" + id + ".png").toString());
					}
					if (Paths.get("images/en/new/" + id + ".png").toFile().exists()) {
						throw new FileAlreadyExistsException(Paths.get("images/new/" + id + ".png").toString());
					}
					if (Paths.get("images/en/new_LOOT/" + id + ".png").toFile().exists()) {
						throw new FileAlreadyExistsException(Paths.get("images/new_LOOT/" + id + ".png").toString());
					}
					if (Paths.get("images/en/new_KFT/" + id + ".png").toFile().exists()) {
						throw new FileAlreadyExistsException(Paths.get("images/new_KFT/" + id + ".png").toString());
					}
					if (Paths.get("images/en/new_UNG/" + id + ".png").toFile().exists()) {
						throw new FileAlreadyExistsException(Paths.get("images/new_UNG/" + id + ".png").toString());
					}
					// Download the card
					URL url = new URL(spec);
					InputStream in = url.openStream();
					Files.copy(in, Paths.get("images/en/" + imageName));
					long imageSize = Files.size(Paths.get("images/en/" + imageName));
					if (imageSize > 0) {
						// Update the card
						card.put("cardImage", imageName);
						imageMap.put(id, imageName);
						in.close();
						System.out.println("Downloaded card for " + id);
					}
					else {
						Paths.get("images/en/" + imageName).toFile().delete();
						System.out.println("\tEmpty image, skipping");
					}
				}
				catch (FileAlreadyExistsException e) {
					// Update the card
					 System.out.println("\tImage already exists for: " + id);
					 card.put("cardImage", imageName);
					imageMap.put(id, imageName);
				}
				catch (Exception e) {
					System.out.println("Image does not exist " + imageName + " at path " + spec);
					// e.printStackTrace();
				}
//				}
//				else {
//					// This means we have already downloaded the image
//					card.put("cardImage", imageName);
//				}

				imageName = id + ".gif";
				spec = "http://media.services.zam.com/v1/media/byName/hs/cards/enus/animated/" + id
							+ "_premium.gif" + "?12576";

//				if (!goldImageMap.has(id)) {
				try {
					if (Paths.get("images/golden/" + id + ".gif").toFile().exists()) {
						throw new FileAlreadyExistsException(Paths.get("images/golden/" + id + ".gif").toString());
					}
					if (Paths.get("images/golden/old/" + id + ".gif").toFile().exists()) {
						throw new FileAlreadyExistsException(Paths.get("images/golden/old/" + id + ".gif").toString());
					}
					if (Paths.get("images/golden/new/" + id + ".gif").toFile().exists()) {
						throw new FileAlreadyExistsException(Paths.get("images/golden/new" + id + ".gif").toString());
					}
					if (Paths.get("images/golden/new_UNG/" + id + ".gif").toFile().exists()) {
						throw new FileAlreadyExistsException(Paths.get("images/golden/new_UNG" + id + ".gif").toString());
					}
					// Get the golden image
					// Download the card
					URL url = new URL(spec);
					InputStream in = url.openStream();
					Files.copy(in, Paths.get("images/golden/" + id + ".gif"));
					long imageSize = Files.size(Paths.get("images/golden/" + id + ".gif"));
					if (imageSize > 0) {
						// Update the card
						card.put("goldenImage", id + ".gif");
						goldImageMap.put(id, id + ".gif");
						System.out.println("Golden image downloaded for " + id);
					}
					else {
						Paths.get("images/golden/" + id + ".gif").toFile().delete();
						System.out.println("\tEmpty image, skipping");
					}
					in.close();
				}
				catch (FileAlreadyExistsException e) {
					// Update the card
					 System.out.println("\tGolden image already exists for: " + id);
					card.put("goldenImage", id + ".gif");
					goldImageMap.put(id, id + ".gif");
				}
				catch (Exception e) {
					System.out.println("Golden mage does not exist " + imageName + " at path " + spec);
					// e.printStackTrace();
				}
//				}
//				else {
//					// This means we have already downloaded the image
//					card.put("goldenImage", id + ".gif");
//				}

				// imageName = id + ".jpg";
				// if (!cardsArtMap.has(id)) {
				// String spec = "http://art.hearthstonejson.com/v1/256x/" +
				// imageName;
				// String specBig = "http://art.hearthstonejson.com/v1/512x/" +
				// imageName;
				// try {
				// // Download the card
				// URL url = new URL(spec);
				// InputStream in = url.openStream();
				// Files.copy(in, Paths.get("images/cardsart/256x/" +
				// imageName));
				// // Update the card
				// card.put("cardArt", imageName);
				// cardsArtMap.put(id, imageName);
				// in.close();
				// System.out.println("Downloaded small card for " + id);
				//
				// URL urlBig = new URL(specBig);
				// InputStream inBig = urlBig.openStream();
				// Files.copy(inBig, Paths.get("images/cardsart/512x/" +
				// imageName));
				// // Update the card
				// inBig.close();
				// System.out.println("Downloaded big card for " + id);
				// }
				// catch (FileAlreadyExistsException e) {
				// // Update the card
				// card.put("cardArt", imageName);
				// cardsArtMap.put(id, imageName);
				// }
				// catch (Exception e) {
				// System.out.println("Image does not exist " + imageName + " at
				// path " + spec);
				// e.printStackTrace();
				// }
				// }
				// else {
				// // This means we have already downloaded the image
				// card.put("cardArt", imageName);
				// }

				// System.out.println(card);
				cardsWriter.write(cardObject.toString() + ",");
			}
//			System.out.println(imageMap);
//			System.out.println(goldImageMap);
//			System.out.println(cardsArtMap);

			System.out.println(audioClips);
			System.out.println(referenceCards);

			cardsWriter.close();
			FileWriter imagesWriter = new FileWriter("ref_imageMap.json", false);
			imagesWriter.write(imageMap.toString());
			imagesWriter.close();

			FileWriter goldImagesWriter = new FileWriter("ref_goldImageMap.json", false);
			goldImagesWriter.write(goldImageMap.toString());
			goldImagesWriter.close();

			FileWriter cardsArtWriter = new FileWriter("ref_cardsArtMap.json", false);
			cardsArtWriter.write(cardsArtMap.toString());
			cardsArtWriter.close();

		}
		catch (Exception e) {
			e.printStackTrace();
			System.err.println("Issue processing card " + card);
			if (imageMap != null) {
				FileWriter imagesWriter = new FileWriter("ref_imageMap.json", false);
				imagesWriter.write(imageMap.toString());
				imagesWriter.close();
			}
			if (goldImageMap != null) {
				FileWriter imagesWriter = new FileWriter("ref_goldImageMap.json", false);
				imagesWriter.write(goldImageMap.toString());
				imagesWriter.close();
			}
			if (cardsArtMap != null) {
				FileWriter imagesWriter = new FileWriter("ref_cardsArtMap.json", false);
				imagesWriter.write(cardsArtMap.toString());
				imagesWriter.close();
			}
		}
		System.out.println("finished processing cards");
	}
}
