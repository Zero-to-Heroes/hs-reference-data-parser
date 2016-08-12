package hearthstoneparser;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;

import org.json.JSONObject;

public class GenerateReferenceStrings {

	public static void main(String[] args) throws Exception {
		JSONObject referenceStrings = new JSONObject();

		// Read the english version of the file
		populateReferenceStrings(referenceStrings, "en", "enUS");
		populateReferenceStrings(referenceStrings, "fr", "frFR");

		System.out.println(referenceStrings);
	}

	private static void populateReferenceStrings(JSONObject referenceStrings, String languageShort, String language)
			throws MalformedURLException, IOException {
		URL url = new URL(
				"https://raw.githubusercontent.com/HearthSim/hsdata/master/Strings/" + language + "/GLOBAL.txt");
		Scanner s = new Scanner(url.openStream(), "UTF-8");

		while (s.hasNext()) {
			String line = s.nextLine();
			// Only keep the keywords
			if (!line.startsWith("GLOBAL_KEYWORD")) {
				continue;
			}

			String[] elements = line.split("\t");
			JSONObject stringObj = null;
			if (referenceStrings.has(elements[0])) {
				stringObj = referenceStrings.getJSONObject(elements[0]);
			}
			else {
				stringObj = new JSONObject();
				referenceStrings.put(elements[0], stringObj);
			}
			stringObj.put(languageShort, elements[1]);
		}
	}
}
