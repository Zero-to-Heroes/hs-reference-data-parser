package hearthstoneparser.arenadrafts.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.zerotoheroes.hsgameparser.db.Card;
import com.zerotoheroes.hsgameparser.db.CardsList;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Choice {

	@JsonProperty(value = "Card1")
	private CardPick card1;

	@JsonProperty(value = "Card2")
	private CardPick card2;

	@JsonProperty(value = "Card3")
	private CardPick card3;

	public void populateData(CardsList cardsList) {
		if (card1 != null) {
			card1.populateData(cardsList);
		}
		if (card2 != null) {
			card2.populateData(cardsList);
		}
		if (card3 != null) {
			card3.populateData(cardsList);
		}
	}

	public Card getSelected() {
		if (card1 != null && card1.isSelected()) { return card1.getCard(); }
		if (card2 != null && card2.isSelected()) { return card2.getCard(); }
		if (card3 != null && card3.isSelected()) { return card3.getCard(); }
		return null;
	}
}