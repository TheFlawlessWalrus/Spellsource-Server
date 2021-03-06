package net.demilich.metastone.game.spells;

import co.paralleluniverse.fibers.Suspendable;
import net.demilich.metastone.game.cards.costmodifier.CardCostModifier;
import net.demilich.metastone.game.spells.desc.filter.EntityFilter;
import net.demilich.metastone.game.spells.desc.source.CardSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.cards.Card;
import net.demilich.metastone.game.cards.CardList;
import net.demilich.metastone.game.entities.Entity;
import net.demilich.metastone.game.spells.desc.SpellArg;
import net.demilich.metastone.game.spells.desc.SpellDesc;
import net.demilich.metastone.game.targeting.Zones;

import java.util.List;
import java.util.Map;

/**
 * Copies a {@code target} card. Includes card cost modifiers that are hosted by the card (typically ones that target
 * {@link net.demilich.metastone.game.targeting.EntityReference#SELF}.
 * <p>
 * Casts the {@link SpellArg#SPELL} sub-spell on each newly generated card as the {@link
 * net.demilich.metastone.game.targeting.EntityReference#OUTPUT}. To copy a card in your opponent's hand:
 * <pre>
 *   {
 *     "class": "CopyCardSpell",
 *     "target": "ENEMY_HAND",
 *     "randomTarget": true
 *   }
 * </pre>
 */
public class CopyCardSpell extends Spell {

	private static Logger logger = LoggerFactory.getLogger(CopyCardSpell.class);

	public static SpellDesc create(Card card) {
		Map<SpellArg, Object> args = new SpellDesc(CopyCardSpell.class);
		args.put(SpellArg.TARGET, card.getReference());
		return new SpellDesc(args);
	}

	public static SpellDesc create(Card card, int copies) {
		SpellDesc desc = create(card);
		desc.put(SpellArg.VALUE, copies);
		return desc;
	}

	@Override
	@Suspendable
	protected void onCast(GameContext context, Player player, SpellDesc desc, Entity source, Entity target) {
		checkArguments(logger, context, source, desc, SpellArg.CARD_LOCATION, SpellArg.CARD_FILTER, SpellArg.CARD_SOURCE, SpellArg.SPELL);
		int numberOfCardsToCopy = desc.getValue(SpellArg.VALUE, context, player, target, source, 1);
		if (target != null) {
			Card targetCard = target.getSourceCard();
			for (int i = 0; i < numberOfCardsToCopy; i++) {
				final Card clone = copyAndReceiveCard(context, player, targetCard);
				final SpellDesc subSpell = (SpellDesc) desc.get(SpellArg.SPELL);
				SpellUtils.castChildSpell(context, player, subSpell, source, target, clone);
			}
			return;
		}

		CardList sourceCollection = null;
		Zones cardLocation = (Zones) desc.get(SpellArg.CARD_LOCATION);
		if (cardLocation != null) {
			sourceCollection = getCardsFromLocation(context, player, cardLocation);
		}

		CardSource cardSource = (CardSource) desc.get(SpellArg.CARD_SOURCE);
		if (cardSource != null) {
			sourceCollection = cardSource.getCards(context, source, player);
		}

		if (sourceCollection == null) {
			logger.error("Trying to access a null source collection.");
			return;
		}

		EntityFilter filter = (EntityFilter) desc.get(SpellArg.CARD_FILTER);

		if (filter != null) {
			sourceCollection = sourceCollection.filtered(filter.matcher(context, player, source));
		}

		List<SpellDesc> subSpells = desc.subSpells(0);
		for (int i = 0; i < numberOfCardsToCopy; i++) {
			if (sourceCollection.isEmpty()) {
				return;
			}
			Card random = context.getLogic().getRandom(sourceCollection);
			peek(random, context, player);
			Card output = copyAndReceiveCard(context, player, random);
			for (SpellDesc subSpell : subSpells) {
				SpellUtils.castChildSpell(context, player, subSpell, source, target, output);
			}
		}
	}

	@Suspendable
	private static Card copyAndReceiveCard(GameContext context, Player player, Card inCard) {
		Card clone = inCard.getCopy();
		context.getLogic().receiveCard(player.getId(), clone);
		// Add copies of the card cost modifiers that are associated with this card here
		// TODO: What about Val'anyr buffed cards?
		context.getTriggersAssociatedWith(inCard.getReference())
				.stream()
				.filter(e -> e instanceof CardCostModifier)
				.map(e -> (CardCostModifier) e)
				.filter(CardCostModifier::targetsSelf)
				.map(CardCostModifier::clone)
				.peek(c -> c.setHost(clone))
				.forEach(c -> context.getLogic().addGameEventListener(player, c, clone));

		return clone;
	}

	protected void peek(final Card random, GameContext context, Player player) {
	}

	@Deprecated
	private CardList getCardsFromLocation(GameContext context, Player player, Zones cardLocation) {
		if (cardLocation == null) {
			return null;
		}
		// By default, CopyCardSpell actually uses the OPPONENT'S card locations.
		Player targetPlayer = context.getOpponent(player);
		switch (cardLocation) {
			case DECK:
				return targetPlayer.getDeck();
			case HAND:
				return targetPlayer.getHand();
			default:
				logger.error("Trying to copy cards from invalid cardLocation {}", cardLocation);
				return null;
		}
	}
}
