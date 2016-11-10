package hearthstoneparser.arenadrafts.domain;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.zerotoheroes.hsgameparser.db.Card;
import com.zerotoheroes.hsgameparser.db.CardsList;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Run {

	private String id;

	@JsonProperty(value = "Choices")
	private List<Choice> choices = new ArrayList<>();

	public List<Card> getPickedCards() {
		List<Card> picked = new ArrayList<>();

		for (Choice choice : choices) {
			if (choice.getCard1() != null && choice.getCard1().isSelected()) {
				picked.add(choice.getCard1().getCard());
			}
			else if (choice.getCard2() != null && choice.getCard2().isSelected()) {
				picked.add(choice.getCard2().getCard());
			}
			else if (choice.getCard3() != null && choice.getCard3().isSelected()) {
				picked.add(choice.getCard3().getCard());
			}
		}

		return picked;
	}

	public void populateData(CardsList cardsList) {
		for (Choice choice : choices) {
			choice.populateData(cardsList);
		}
	}
}