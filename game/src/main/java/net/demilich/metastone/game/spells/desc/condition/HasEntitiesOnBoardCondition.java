package net.demilich.metastone.game.spells.desc.condition;

import java.util.ArrayList;
import java.util.List;

import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.entities.Actor;
import net.demilich.metastone.game.entities.Entity;
import net.demilich.metastone.game.targeting.EntityReference;

public class HasEntitiesOnBoardCondition extends Condition {

	public HasEntitiesOnBoardCondition(ConditionDesc desc) {
		super(desc);
	}

	@Override
	protected boolean isFulfilled(GameContext context, Player player, ConditionDesc desc, Entity source, Entity target) {
		EntityReference entityReference = (EntityReference) desc.get(ConditionArg.TARGET);
		Entity entity = null;
		if (entityReference == null) {
			entity = target;
		} else {
			List<Entity> entities = context.resolveTarget(player, entity, entityReference);
			if (entities == null || entities.isEmpty()) {
				return false;
			}
			entity = entities.get(0);
		}
		String[] cardNames = (String[]) desc.get(ConditionArg.CARDS);
		
		List<Actor> checkedActors = new ArrayList<Actor>(player.getMinions());
		if (player.getHero().getWeapon() != null) {
			checkedActors.add(player.getHero().getWeapon());
		}
		checkedActors.add(player.getHero());
		
		for (String cardName : cardNames) {
			boolean check = false;
			for (Actor actor : checkedActors) {
				if (actor.getSourceCard().getCardId().equalsIgnoreCase(cardName)) {
					check = true;
					checkedActors.remove(actor);
					break;
				}
			}
			if (!check) {
				return false;
			}
		}
		return true;
	}

}
