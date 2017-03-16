package hearthstoneparser;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.commons.lang.WordUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.simple.parser.JSONParser;

public class GenerateCardData {

	public static void main(String[] args) throws Exception {
		JSONParser parser = new JSONParser();

		JSONArray referenceCards = new JSONArray(
				((org.json.simple.JSONArray) parser.parse(new FileReader("ref_allCards.json"))).toJSONString());
		JSONObject imageMap = new JSONObject(
				((org.json.simple.JSONObject) parser.parse(new FileReader("ref_imageMap.json"))).toJSONString());
		JSONObject goldImageMap = new JSONObject(
				((org.json.simple.JSONObject) parser.parse(new FileReader("ref_goldImageMap.json"))).toJSONString());
		JSONObject cardsArtMap = new JSONObject(
				((org.json.simple.JSONObject) parser.parse(new FileReader("ref_cardsArtMap.json"))).toJSONString());

		FileWriter cardsWriter = new FileWriter("out_cardsWithImages.json", false);
		System.out.println("init done " + referenceCards.length());
		JSONObject card = null;
		try {

			for (Object cardObject : referenceCards) {
				card = (JSONObject) cardObject;
				if (!card.has("name")) {
					continue;
				}
				// System.out.println("do the card " +
				// card.getJSONObject("name").getString("enUS") + " " +
				// cardObject);
				JSONObject nameLoc = card.getJSONObject("name");
				card.remove("name");
				String id = card.getString("id");

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
				card.remove("race");
				card.remove("mechanics");
				card.remove("targetingArrowText");
				card.remove("flavor");
				card.remove("textInPlay");
				card.remove("entourage");

				// And capitalize the data that is now in full uppercase
				if (card.has("playerClass")) {
					card.put("playerClass", WordUtils.capitalizeFully(card.getString("playerClass")));
				}
				if (card.has("rarity")) {
					card.put("rarity", WordUtils.capitalizeFully(card.getString("rarity")));
				}
				if (card.has("set")) {
					card.put("set", WordUtils.capitalizeFully(card.getString("set")));
				}
				if (card.has("type")) {
					card.put("type", WordUtils.capitalizeFully(card.getString("type")));
				}

				// Now handle images
				String imageName = id + ".png";

				if (!imageMap.has(id)) {
					String spec = "http://media.services.zam.com/v1/media/byName/hs/cards/enus/" + imageName + "?12576";
					try {
						// Download the card
						URL url = new URL(spec);
						InputStream in = url.openStream();
						Files.copy(in, Paths.get("images/en/" + imageName));
						// Update the card
						card.put("cardImage", imageName);
						imageMap.put(id, imageName);
						in.close();
						System.out.println("Downloaded card for " + id);
					}
					catch (FileAlreadyExistsException e) {
						// Update the card
						card.put("cardImage", imageName);
						imageMap.put(id, imageName);
					}
					catch (Exception e) {
						System.out.println("Image does not exist " + imageName + " at path " + spec);
						// e.printStackTrace();
					}
				}
				else {
					// This means we have already downloaded the image
					card.put("cardImage", imageName);
				}

				imageName = id + ".gif";
				if (!goldImageMap.has(id)) {
					// Get the golden image
					System.out.println("Trying to get golden image for " + id);
					String spec = "http://media.services.zam.com/v1/media/byName/hs/cards/enus/animated/" + id
							+ "_premium.gif" + "?12576";

					try {
						// Download the card
						URL url = new URL(spec);
						InputStream in = url.openStream();
						Files.copy(in, Paths.get("images/en/golden/" + id + ".gif"));
						long imageSize = Files.size(Paths.get("images/en/golden/" + id + ".gif"));
						if (imageSize > 0) {
							// Update the card
							card.put("goldenImage", id + ".gif");
							goldImageMap.put(id, id + ".gif");
						}
						else {
							System.out.println("\tEmpty image, skipping");
						}
						in.close();
					}
					catch (FileAlreadyExistsException e) {
						// Update the card
						// System.out.println("\tGolden image already exists
						// for: " + id);
						card.put("goldenImage", id + ".gif");
						goldImageMap.put(id, id + ".gif");
					}
					catch (Exception e) {
						System.out.println("Image does not exist " + imageName + " at path " + spec);
						// e.printStackTrace();
					}
				}
				else {
					card.put("goldenImage", imageName);
				}

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
			System.out.println(imageMap);
			System.out.println(goldImageMap);
			System.out.println(cardsArtMap);

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
