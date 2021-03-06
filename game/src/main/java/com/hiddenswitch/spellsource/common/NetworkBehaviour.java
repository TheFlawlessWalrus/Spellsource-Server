package com.hiddenswitch.spellsource.common;

import co.paralleluniverse.fibers.Suspendable;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.ext.sync.Sync;
import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.actions.GameAction;
import net.demilich.metastone.game.behaviour.UtilityBehaviour;
import net.demilich.metastone.game.cards.Card;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.List;

/**
 * Represents a behaviour that delegates its requests to a networking interface provided by a {@link GameContext}.
 */
public class NetworkBehaviour extends UtilityBehaviour implements Serializable {
	private static final long serialVersionUID = 1L;

	private Logger logger = LoggerFactory.getLogger(NetworkBehaviour.class);
	private boolean isHuman = true;

	public NetworkBehaviour() {
	}

	@Override
	public String getName() {
		return "Network behaviour";
	}

	@Override
	@Suspendable
	public List<Card> mulligan(GameContext context, Player player, List<Card> cards) {
		return Sync.awaitFiber(done -> context.networkRequestMulligan(player, cards, result -> done.handle(Future.succeededFuture(result))));
	}

	@Override
	@Suspendable
	public void mulliganAsync(GameContext context, Player player, List<Card> cards, Handler<List<Card>> handler) {
		context.networkRequestMulligan(player, cards, handler);
	}

	@Override
	@Suspendable
	public GameAction requestAction(GameContext context, Player player, List<GameAction> validActions) {
		return Sync.awaitFiber(done -> requestActionAsync(context, player, validActions, result -> done.handle(Future.succeededFuture(result))));
	}

	@Suspendable
	@Override
	public void requestActionAsync(GameContext context, Player player, List<GameAction> validActions, Handler<GameAction> handler) {
		context.networkRequestAction(context.getGameStateCopy(), player.getId(), validActions, handler);
	}

	@Override
	@Suspendable
	public void onGameOver(GameContext context, int playerId, int winningPlayerId) {
	}

	@Override
	@Suspendable
	public void onGameOverAuthoritative(GameContext context, int playerId, int winningPlayerId) {
		if (winningPlayerId != -1) {
			context.sendGameOver(context.getPlayer(playerId), context.getPlayer(winningPlayerId));
		} else {
			context.sendGameOver(context.getPlayer(playerId), null);
		}
	}

	@Override
	public boolean isHuman() {
		return isHuman;
	}

	public void setHuman(boolean human) {
		isHuman = human;
	}
}
