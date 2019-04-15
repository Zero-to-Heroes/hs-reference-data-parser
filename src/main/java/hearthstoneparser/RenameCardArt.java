package hearthstoneparser;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;

public class RenameCardArt {

    public static void main(String[] args) throws Exception {
        JSONParser parser = new JSONParser();
        BufferedReader referenceIn = new BufferedReader(new InputStreamReader(
                GenerateCardData.class.getResourceAsStream("cards.json"),
                "UTF-8"));
        JSONArray referenceCards = new JSONArray(((org.json.simple.JSONArray) parser.parse(referenceIn)).toJSONString());

        for (Object cardObject : referenceCards) {
            JSONObject card = (JSONObject) cardObject;
            if (!card.has("dbfId")) {
                System.err.println("Cannot parse card without dbfId: " + card.toString());
                continue;
            }
            File existingImage = new File("G:\\Source\\github\\hearthstone-card-images\\rel\\" + card.getInt("dbfId") + ".png");
            if (!existingImage.exists()) {
                continue;
            }
            File destination = new File("images/copies/en/" + card.getString("id") + ".png");
            if (destination.exists()) {
                continue;
            }
            Files.copy(existingImage.toPath(), destination.toPath());
            System.out.println("Created " + destination.toPath());
        }
    }
}
