package me.matsubara.roulette.game.state;


import com.cryptomorin.xseries.XSound;
import com.google.common.base.Predicates;
import me.matsubara.roulette.RoulettePlugin;
import me.matsubara.roulette.event.RouletteStartEvent;
import me.matsubara.roulette.file.Config;
import me.matsubara.roulette.file.Messages;
import me.matsubara.roulette.game.Game;
import me.matsubara.roulette.game.GameState;
import me.matsubara.roulette.game.data.Bet;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

public final class Starting extends BukkitRunnable {

    private final RoulettePlugin plugin;
    private final Game game;
    private final XSound.Record countdownSound = XSound.parse(Config.SOUND_COUNTDOWN.asString());

    private int seconds;

    public Starting(RoulettePlugin plugin, @NotNull Game game) {
        this.plugin = plugin;
        this.game = game;

        this.seconds = game.getStartTime();
        game.setState(GameState.STARTING);
    }

    @Override
    public void run() {
        if (seconds == 0) {
            Selecting selecting = new Selecting(plugin, game);

            if (game.getAllBets().stream().anyMatch(Predicates.not(Bet::isEnPrison))) {
                // If there's at least 1 bet that is NOT in prison, then we want to start the selecting task.
                game.setSelecting(selecting);
                selecting.runTaskTimer(plugin, 1L, 1L);

                RouletteStartEvent startEvent = new RouletteStartEvent(game);
                plugin.getServer().getPluginManager().callEvent(startEvent);
            } else {
                // All bets in this game are in prison, go straight to spinning.
                selecting.startSpinningTask();
            }

            cancel();
            return;
        }

        if (seconds % 5 == 0 || seconds <= 3) {
            // Play countdown sound.
            game.playSound(countdownSound);

            // Send countdown.
            game.broadcast(Messages.Message.STARTING, line -> line.replace("%seconds%", String.valueOf(seconds)));
        }

        seconds--;
    }
}