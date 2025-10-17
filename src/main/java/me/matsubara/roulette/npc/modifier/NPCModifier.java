package me.matsubara.roulette.npc.modifier;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import lombok.Getter;
import me.matsubara.roulette.npc.NPC;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class NPCModifier {

    protected final NPC npc;
    protected final @Getter List<LazyPacket> packetContainers = new CopyOnWriteArrayList<>();

    public NPCModifier(NPC npc) {
        this.npc = npc;
    }

    protected void queuePacket(LazyPacket packet) {
        packetContainers.add(packet);
    }

    protected void queueInstantly(@NotNull LazyPacket packet) {
        PacketWrapper<? extends PacketWrapper<?>> container = packet.provide(npc, null);
        packetContainers.add((npc, player) -> container);
    }

    public void send() {
        send(npc.getSeeingPlayers());
    }

    public void send(@NotNull Iterable<? extends Player> players) {
        players.forEach(player -> {
            for (LazyPacket packet : packetContainers) {
                Object channel = PacketEvents.getAPI().getPlayerManager().getChannel(player);
                if (channel != null) {
                    PacketEvents.getAPI().getProtocolManager().sendPacket(channel, packet.provide(npc, player));
                }
            }
        });
        packetContainers.clear();
    }

    public void send(Player... players) {
        send(Arrays.asList(players));
    }

    public interface LazyPacket {

        PacketWrapper<? extends PacketWrapper<?>> provide(NPC npc, Player player);
    }
}