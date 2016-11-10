package hearthstoneparser.arenadrafts.domain;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.zerotoheroes.hsgameparser.db.Card;
import com.zerotoheroes.hsgameparser.db.CardsList;

import hearthstoneparser.arenadrafts.ArenaRunExtractor;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CardPick {

	private Card card;

	@JsonProperty(value = "ImgUrl")
	private String url;

	@JsonProperty(value = "Selected")
	private boolean selected;

	public void populateData(CardsList cardsList) {
		// Build the real ID
		Pattern pattern = Pattern.compile(ArenaRunExtractor.CARD_URL);
		Matcher matcher = pattern.matcher(url);
		while (matcher.find()) {
			String id = matcher.group(1);

			// Find the card's rarity
			card = cardsList.find(id);
		}
	}
}