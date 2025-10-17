package me.matsubara.roulette.listener;

import me.matsubara.roulette.RoulettePlugin;
import me.matsubara.roulette.game.Game;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

public final class PlayerQuit implements Listener {

    private final RoulettePlugin plugin;

    public PlayerQuit(RoulettePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Remove player from input.
        plugin.getInputManager().remove(player);

        // Remove player from game when leaving.
        Game game = plugin.getGameManager().getGameByPlayer(player);
        if (game != null) game.removeCompletely(player);
    }
}