package me.matsubara.roulette.gui.data;

import com.google.common.base.Predicates;
import lombok.Getter;
import me.matsubara.roulette.RoulettePlugin;
import me.matsubara.roulette.file.Config;
import me.matsubara.roulette.game.Game;
import me.matsubara.roulette.gui.RouletteGUI;
import me.matsubara.roulette.manager.data.PlayerResult;
import me.matsubara.roulette.manager.data.RouletteSession;
import me.matsubara.roulette.util.InventoryUpdate;
import me.matsubara.roulette.util.ItemBuilder;
import me.matsubara.roulette.util.PluginUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public final class SessionsGUI extends RouletteGUI {

    // The instance of the plugin.
    private final RoulettePlugin plugin;

    // The player viewing this inventory.
    private final Player player;

    // The inventory being used.
    private final Inventory inventory;

    // Format to use in dates.
    private final SimpleDateFormat format;

    // The current page.
    private int currentPage;

    // The max number of pages.
    private int pages;

    // The slots to show the content.
    private static final int[] SLOTS = {10, 11, 12, 13, 14, 15, 16};

    // The slot to put page navigator items and other stuff.
    private static final int[] HOTBAR = {19, 20, 21, 22, 23, 24, 25};

    public SessionsGUI(RoulettePlugin plugin, Player player) {
        this(plugin, player, 0);
    }

    public SessionsGUI(@NotNull RoulettePlugin plugin, @NotNull Player player, int currentPage) {
        super("sessions-menu");
        this.plugin = plugin;
        this.player = player;
        this.inventory = plugin.getServer().createInventory(this, 36);
        this.format = new SimpleDateFormat(Config.DATE_FORMAT.asString());
        this.currentPage = currentPage;

        player.openInventory(inventory);
        updateInventory();
    }

    public void updateInventory() {
        inventory.clear();

        // Get the list of sessions.
        List<RouletteSession> sessions = plugin.getDataManager().getSessions();

        // Page formula.
        pages = (int) (Math.ceil((double) sessions.size() / SLOTS.length));

        ItemStack background = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                .setDisplayName("&7")
                .build();

        // Set background items.
        for (int i = 0; i < 36; i++) {
            if (ArrayUtils.contains(SLOTS, i) || ArrayUtils.contains(HOTBAR, i)) continue;
            // Set background item in the current slot from the loop.
            inventory.setItem(i, background);
        }


        // If the current page isn't 0 (first page), show the previous page item.
        if (currentPage > 0) inventory.setItem(19, getItem("previous").build());

        // If the current page isn't the last one, show the next page item.
        if (currentPage < pages - 1) inventory.setItem(25, getItem("next").build());

        // Assigning slots.
        Map<Integer, Integer> slotIndex = new HashMap<>();
        for (int i : SLOTS) {
            slotIndex.put(ArrayUtils.indexOf(SLOTS, i), i);
        }

        // Where to start.
        int startFrom = currentPage * SLOTS.length;

        boolean isLastPage = currentPage == pages - 1;

        for (int index = 0, aux = startFrom; isLastPage ? (index < sessions.size() - startFrom) : (index < SLOTS.length); index++, aux++) {
            RouletteSession session = sessions.get(aux);

            ItemBuilder builder = getItem("session")
                    .replace("%name%", session.name())
                    .replace("%date%", format.format(new Date(session.timestamp())))
                    .replace("%slot%", PluginUtils.getSlotName(session.slot()))
                    .setData(plugin.getSessionKey(), PluginUtils.UUID_TYPE, session.sessionUUID());


            if (session.results().stream()
                    .noneMatch(Predicates.not(PlayerResult::won))) {
                builder.addLore("", Config.SESSIONS_ONLY_VICTORIES_TEXT.asStringTranslated());
            }

            inventory.setItem(slotIndex.get(index), builder
                    .build());
        }

        // Update inventory title to show the current page.
        InventoryUpdate.updateInventory(player, Config.SESSIONS_MENU_TITLE.asStringTranslated()
                .replace("%page%", String.valueOf(currentPage + 1))
                .replace("%max%", String.valueOf(pages)));
    }

    public void previousPage(boolean isShiftClick) {
        currentPage = isShiftClick ? 0 : currentPage - 1;
        updateInventory();
    }

    public void nextPage(boolean isShiftClick) {
        currentPage = isShiftClick ? pages - 1 : currentPage + 1;
        updateInventory();
    }

    @Contract(pure = true)
    @Override
    public @Nullable Game getGame() {
        return null;
    }
}