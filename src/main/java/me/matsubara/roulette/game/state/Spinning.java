package me.matsubara.roulette.game.state;

import com.cryptomorin.xseries.XSound;
import com.github.retrooper.packetevents.protocol.entity.pose.EntityPose;
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot;
import lombok.Setter;
import me.matsubara.roulette.RoulettePlugin;
import me.matsubara.roulette.event.LastRouletteSpinEvent;
import me.matsubara.roulette.file.Config;
import me.matsubara.roulette.file.Messages;
import me.matsubara.roulette.game.Game;
import me.matsubara.roulette.game.GameState;
import me.matsubara.roulette.game.data.Bet;
import me.matsubara.roulette.game.data.Slot;
import me.matsubara.roulette.hologram.Hologram;
import me.matsubara.roulette.model.stand.PacketStand;
import me.matsubara.roulette.model.stand.data.ItemSlot;
import me.matsubara.roulette.npc.NPC;
import me.matsubara.roulette.npc.modifier.MetadataModifier;
import me.matsubara.roulette.util.PluginUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.function.UnaryOperator;

public final class Spinning extends BukkitRunnable {

    private final RoulettePlugin plugin;
    private final Game game;
    private final PacketStand ball;
    private final boolean isEuropean;
    private final Slot[] slots;
    private final int totalTime;

    private int time;
    private boolean shouldStart;
    private @Setter Slot force;

    private final String hologramSpinning = Config.SPINNING.asStringTranslated();
    private final String hologramWinningNumber = Config.WINNING_NUMBER.asStringTranslated();
    private final XSound.Record spinningSound = XSound.parse(Config.SOUND_SPINNING.asString());

    public Spinning(@NotNull RoulettePlugin plugin, @NotNull Game game) {
        this.plugin = plugin;
        this.game = game;
        this.ball = game.getBall();
        this.isEuropean = game.getType().isEuropean();
        this.slots = new Slot[isEuropean ? 37 : 38];
        this.totalTime = Config.COUNTDOWN_SORTING.asInt() * 20;
        this.time = totalTime;
        this.shouldStart = true;

        System.arraycopy(Slot.values(game), 0, slots, 0, isEuropean ? 37 : 38);

        game.setState(GameState.SPINNING);
        game.removeSleepingPlayers();

        for (Player player : game.getPlayers()) {
            // Remove glow, hide hologram and close custom menus.
            game.getBets(player).forEach(Bet::hide);
            game.sendBets(
                    player,
                    Messages.Message.YOUR_BETS,
                    Messages.Message.BET_HOVER,
                    UnaryOperator.identity(),
                    false);
            game.closeOpenMenu(player);
        }

        if (game.isEmpty()) {
            game.restart();
            shouldStart = false;
            return;
        }

        NPC npc = game.getNpc();

        game.npcBroadcast(Messages.Message.NO_BETS);

        // Play NPC spin animation.
        game.lookAtFace(game.getDefaultNPCFace());
        npc.metadata().queue(MetadataModifier.EntityMetadata.POSE, EntityPose.CROUCHING).send();
        npc.equipment().queue(EquipmentSlot.MAIN_HAND, RoulettePlugin.EMPTY_ITEM).send();

        // Show ball, shouldn't be null.
        if (ball == null) return;

        ball.getSettings().getEquipment().put(ItemSlot.HEAD, new ItemStack(Material.END_ROD));
        ball.sendEquipment(game.getSeeingPlayers());
    }

    @Override
    public void run() {
        if (!shouldStart) {
            cancel();
            return;
        }

        Hologram spinHologram = game.getSpinHologram();

        if (time == 0) {
            spinHologram.setLine(0, hologramWinningNumber);

            // Stop NPC animation, check if there are winners and stop.
            game.getNpc().metadata().queue(MetadataModifier.EntityMetadata.POSE, EntityPose.STANDING).send();
            game.setState(GameState.ENDING);
            game.checkWinner();

            cancel();
            return;
        }

        // Spin ball.
        Location location = ball.getLocation();
        location.setYaw(location.getYaw() + (time >= totalTime / 3 ? 30.0f : (30.0f * time / totalTime)));
        ball.teleport(game.getSeeingPlayers(), location);

        // Select a random number.
        int which = PluginUtils.RANDOM.nextInt(0, isEuropean ? 37 : 38);
        game.setWinner(slots[which]);

        if (time == 1) {
            if (force != null) {
                game.setWinner(force);
            } else {
                LastRouletteSpinEvent lastSpinEvent = new LastRouletteSpinEvent(game, game.getWinner());
                plugin.getServer().getPluginManager().callEvent(lastSpinEvent);

                game.setWinner(lastSpinEvent.getWinnerSlot());
            }
        }

        String slotName = PluginUtils.getSlotName(game.getWinner());

        // If the spin hologram is empty, create the lines, else update them.
        if (spinHologram.size() == 0) {

            // Teleport spin hologram to its proper location.
            spinHologram.teleport(ball
                    .getLocation()
                    .clone()
                    .add(0.0d, 2.5d, 0.0d));

            spinHologram.addLines(hologramSpinning);
            spinHologram.addLines(slotName);
        } else {
            spinHologram.setLine(1, slotName);
            // Play spinning sound at spin hologram location, this sound can be heard by every player (even those outside the game).
            game.playSound(spinHologram.getLocation(), spinningSound);
        }

        time--;
    }
}