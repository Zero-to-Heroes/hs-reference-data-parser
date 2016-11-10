package hearthstoneparser.arenadrafts;

import java.util.ArrayList;
import java.util.List;

import com.zerotoheroes.hsgameparser.db.Card;

import hearthstoneparser.arenadrafts.domain.Choice;
import hearthstoneparser.arenadrafts.domain.Run;

/**
 * Get all the data from ArenaDrafts and save it locally for future processing
 *
 * TODO: need to separate from picks 1, 10, 20 and 30 (that are special picks)
 * and the "standard" picks
 */
public class GenerateDraftStats {

	public static void main(String[] args) throws Exception {
		List<Run> allRuns = new ArenaRunExtractor().extractRuns();

		buildGlobalRarityStats(allRuns);
		buildPickRarityStats(allRuns);
	}

	private static void buildPickRarityStats(List<Run> allRuns) {
		ByPickStats fullStats = new ByPickStats();

		for (Run run : allRuns) {

			if (run.getChoices().size() != 30) {
				System.err.println("Suspicious run, not 30 picks " + run);
				continue;
			}

			Stats tempSpecial = new Stats();
			Stats tempNormal = new Stats();

			try {
				for (int i = 0; i < run.getChoices().size(); i++) {

					if (i == 0 || i == 9 || i == 19 || i == 29) {
						addStats(run.getChoices().get(i), tempSpecial, i);
					}
					else {
						addStats(run.getChoices().get(i), tempNormal, i);
					}
				}
			}
			catch (Exception e) {
				System.err.println("Invalid draft? Not considering it " + run);
				continue;
			}

			fullStats.specialPicks.normalIds.addAll(tempSpecial.normalIds);
			fullStats.specialPicks.rareIds.addAll(tempSpecial.rareIds);
			fullStats.specialPicks.epicIds.addAll(tempSpecial.epicIds);
			fullStats.specialPicks.legendaryIds.addAll(tempSpecial.legendaryIds);

			fullStats.normalPicks.normalIds.addAll(tempNormal.normalIds);
			fullStats.normalPicks.rareIds.addAll(tempNormal.rareIds);
			fullStats.normalPicks.epicIds.addAll(tempNormal.epicIds);
			fullStats.normalPicks.legendaryIds.addAll(tempNormal.legendaryIds);
		}

		System.out.println();
		System.out.println("Special picks: ");
		displayStats(fullStats.specialPicks);

		System.out.println("Normal picks: ");
		displayStats(fullStats.normalPicks);
	}

	private static void displayStats(Stats stats) {
		int totalCards = stats.normalIds.size() + stats.rareIds.size() + stats.epicIds.size()
				+ stats.legendaryIds.size();
		System.out.println("\tSample size: " + totalCards);
		System.out.println(
				"\t\tNormal: " + stats.normalIds.size() + "\t" + 1.0 * stats.normalIds.size() / totalCards + "%");
		System.out.println("\t\tRare: " + stats.rareIds.size() + "\t" + 1.0 * stats.rareIds.size() / totalCards + "%");
		System.out.println("\t\tEpic: " + stats.epicIds.size() + "\t" + 1.0 * stats.epicIds.size() / totalCards + "%");
		System.out.println("\t\tLegendary: " + stats.legendaryIds.size() + "\t"
				+ 1.0 * stats.legendaryIds.size() / totalCards + "%");
	}

	private static void addStats(Choice choice, Stats stats, int pickIndex) throws Exception {
		Card card = choice.getSelected();
		if (card == null) { return; }

		if ("legendary".equalsIgnoreCase(card.getRarity())) {
			stats.legendaryIds.add(card.getId());
		}
		else if ("epic".equalsIgnoreCase(card.getRarity())) {
			stats.epicIds.add(card.getId());
		}
		else if ("rare".equalsIgnoreCase(card.getRarity())) {
			stats.rareIds.add(card.getId());
		}
		else {
			if (pickIndex == 0 || pickIndex == 9 || pickIndex == 19 || pickIndex == 29) {
				System.err.println("Should not have normal cards on special picks!!! " + choice);
				throw new Exception("normal pick while expecting a rare pick");
			}
			stats.normalIds.add(card.getId());
		}
	}

	private static void buildGlobalRarityStats(List<Run> allRuns) {
		List<Card> cards = extractCards(allRuns);

		// And now create rarity
		Stats stats = new Stats();
		for (Card card : cards) {
			if ("legendary".equalsIgnoreCase(card.getRarity())) {
				stats.legendaryIds.add(card.getId());
			}
			else if ("epic".equalsIgnoreCase(card.getRarity())) {
				stats.epicIds.add(card.getId());
			}
			else if ("rare".equalsIgnoreCase(card.getRarity())) {
				stats.rareIds.add(card.getId());
			}
			else {
				stats.normalIds.add(card.getId());
			}
		}

		System.out.println();
		System.out.println("Global stats: ");

		int totalCards = stats.normalIds.size() + stats.rareIds.size() + stats.epicIds.size()
				+ stats.legendaryIds.size();

		System.out.println("\tSample size: " + totalCards);
		System.out.println(
				"\t\tNormal: " + stats.normalIds.size() + "\t" + 1.0 * stats.normalIds.size() / totalCards + "%");
		System.out.println("\t\tRare: " + stats.rareIds.size() + "\t" + 1.0 * stats.rareIds.size() / totalCards + "%");
		System.out.println("\t\tEpic: " + stats.epicIds.size() + "\t" + 1.0 * stats.epicIds.size() / totalCards + "%");
		System.out.println("\t\tLegendary: " + stats.legendaryIds.size() + "\t"
				+ 1.0 * stats.legendaryIds.size() / totalCards + "%");
	}

	private static List<Card> extractCards(List<Run> allRuns) {
		List<Card> allCards = new ArrayList<>();

		for (Run run : allRuns) {
			List<Card> picked = run.getPickedCards();
			allCards.addAll(picked);
		}

		return allCards;
	}

	private static class Stats {
		List<String> normalIds = new ArrayList<>();
		List<String> rareIds = new ArrayList<>();
		List<String> epicIds = new ArrayList<>();
		List<String> legendaryIds = new ArrayList<>();
	}

	private static class ByPickStats {

		Stats specialPicks = new Stats();
		Stats normalPicks = new Stats();
	}
}
