package hearthstoneparser.arenadrafts;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerotoheroes.hsgameparser.db.CardsList;

import hearthstoneparser.arenadrafts.domain.Run;

public class ArenaRunExtractor {

	public static final String CARD_URL = "(?:.*)\\/\\/(.*).png";

	private static CardsList cardsList;

	public List<Run> extractRuns() throws Exception {
		if (cardsList == null) {
			cardsList = CardsList.create();
		}

		List<String> userIds = extractUserIds();
		System.out.println("Extracted " + userIds.size() + " users");
		List<String> runIds = extractRunIds(userIds);
		System.out.println("Extracted " + runIds.size() + " run IDs");
		List<Run> allRuns = buildRuns(runIds);
		System.out.println("Build " + allRuns.size() + " runs");

		return allRuns;
	}

	@SuppressWarnings("resource")
	private static List<Run> buildRuns(List<String> runIds)
			throws JsonParseException, JsonMappingException, IOException {
		JSONParser parser = new JSONParser();
		List<Run> allRuns = new ArrayList<>();

		boolean invalidRun = false;

		for (String runId : runIds) {

			File file = new File("drafts/runs/" + runId + ".json");
			JSONObject runData = null;
			if (file.exists()) {
				// System.out.println("\tAlready parsed run " + runId);
				try {
					runData = new JSONObject(((org.json.simple.JSONObject) parser
							.parse(new FileReader("drafts/runs/" + runId + ".json"))).toJSONString());
				}
				catch (Exception e) {
					// System.err.println("Invalid run file " + runId);
					invalidRun = true;

				}
			}
			else {
				System.out.println("Still have a new run: " + runId);
				file.createNewFile();
			}

			if (runData == null && !invalidRun) {
				try {
					System.out.println("\tParsing run " + runId);
					FileWriter runWriter = null;

					runWriter = new FileWriter("drafts/runs/" + runId + ".json", false);
					URL runUrl = new URL("http://arenadrafts.com/api/pick/stats/" + runId);
					Scanner runScanner = new Scanner(runUrl.openStream(), "UTF-8");

					StringBuilder runResult = new StringBuilder();
					while (runScanner.hasNext()) {
						runResult.append(runScanner.nextLine());
					}
					runData = new JSONObject(runResult.toString());
					runWriter.write(runResult.toString());
					runWriter.close();
				}
				catch (Exception e) {
					// System.err.println("Could not parse run " + runId);
				}
			}

			// We can take the decklist here, as all choices are always the
			// same rarity
			if (runData != null) {
				Run run = new ObjectMapper().readValue(runData.toString(), Run.class);
				if (run.getChoices().size() != 30) {
					continue;
				}
				run.setId(runId);
				run.populateData(cardsList);
				allRuns.add(run);
			}
		}

		return allRuns;
	}

	@SuppressWarnings("resource")
	private static List<String> extractRunIds(List<String> userIds)
			throws IOException, ParseException, FileNotFoundException {
		JSONParser parser = new JSONParser();
		List<String> runIds = new ArrayList<>();

		FileWriter userWriter = null;

		for (String userId : userIds) {

			File file = new File("drafts/users/" + userId + ".json");
			JSONArray userJson = null;
			if (file.exists()) {
				// System.out.println("\tAlready parsed " + userId);
				try {
					userJson = new JSONArray(((org.json.simple.JSONArray) parser
							.parse(new FileReader("drafts/users/" + userId + ".json"))).toJSONString());
				}
				catch (Exception e) {
					// System.err.println("Invalid user file " + userId);
				}
			}
			else {
				try {
					file.createNewFile();
				}
				catch (Exception e) {
					// System.err.println("Can't create file with name " +
					// file.getName());
					continue;
				}
			}

			if (userJson == null) {
				try {
					// System.out.println("\tParsing " + userId);

					userWriter = new FileWriter("drafts/users/" + userId + ".json", false);
					URL userUrl = new URL("http://arenadrafts.com/api/stats/getarenas/" + userId);
					Scanner userScanner = new Scanner(userUrl.openStream(), "UTF-8");

					StringBuilder userResult = new StringBuilder();
					while (userScanner.hasNext()) {
						userResult.append(userScanner.nextLine());
					}
					userJson = new JSONArray(userResult.toString());
					userWriter.write(userResult.toString());
					userWriter.close();
				}
				catch (Exception e) {
					// System.err.println("Could not parse draft infos for " +
					// userId);
				}
			}

			// Now get each run
			try {
				if (userJson != null) {
					for (Object runObj : userJson) {
						JSONObject run = (JSONObject) runObj;
						String runId = run.getString("Id");
						runIds.add(runId);
					}
				}
			}
			catch (Exception e) {
				// System.err.println("Could not parse draft infos for " +
				// userId);
			}
		}
		// System.out.println("Run IDS: " + runIds);
		return runIds;
	}

	@SuppressWarnings("resource")
	private static List<String> extractUserIds()
			throws IOException, ParseException, FileNotFoundException, MalformedURLException {
		JSONParser parser = new JSONParser();
		JSONObject allUsers = null;

		File usersFile = new File("drafts/allUsers.json");

		if (usersFile.exists()) {
			// System.out.println("\tAlready created initial users file");
			allUsers = new JSONObject(
					((org.json.simple.JSONObject) parser.parse(new FileReader("drafts/allUsers.json"))).toJSONString());
		}
		if (allUsers == null) {
			// System.out.println("Reparsing users");
			usersFile.createNewFile();

			URL url = new URL("http://arenadrafts.com/api/players/search/value?search=");
			Scanner s = new Scanner(url.openStream(), "UTF-8");

			// We get all the users
			StringBuilder newResult = new StringBuilder();
			while (s.hasNext()) {
				newResult.append(s.nextLine());
			}
			allUsers = new JSONObject(newResult.toString());
			FileWriter usersWriter = new FileWriter("drafts/allUsers.json", false);
			usersWriter.write(allUsers.toString());
			usersWriter.close();
		}

		// Now for each user
		System.out.println("Extracting IDs");
		List<String> userIds = new ArrayList<>();
		for (Object userObj : allUsers.getJSONArray("Data")) {
			JSONObject user = (JSONObject) userObj;
			String username = user.getString("Username").replaceAll(" ", "%20");
			userIds.add(username);
		}
		// System.out.println("User IDs: " + userIds);
		return userIds;
	}

	public int numberOfClassCards() {
		return (int) cardsList.getCards().stream().filter(c -> !c.getPlayerClass().equalsIgnoreCase("neutral")).count();
	}

	public int numberOfNeutralCards() {
		return (int) cardsList.getCards().stream().filter(c -> c.getPlayerClass().equalsIgnoreCase("neutral")).count();
	}
}
