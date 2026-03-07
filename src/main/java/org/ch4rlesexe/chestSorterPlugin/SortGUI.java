package org.ch4rlesexe.chestSorterPlugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;

public class SortGUI implements Listener {

    private final ChestSorterPlugin plugin;

    // Layout: 27-slot chest
    // Row 0: filler
    // Row 1: [G] [TOGGLE] [G] [SR] [SL] [R] [L] [M] [D]
    // Row 2: filler
    private static final int TOGGLE_SLOT = 10;
    private static final int[] METHOD_SLOTS = {12, 13, 14, 15, 16, 17};
    private static final ClickType[] METHODS = {
            ClickType.SHIFT_RIGHT, ClickType.SHIFT_LEFT, ClickType.RIGHT,
            ClickType.LEFT, ClickType.MIDDLE, ClickType.DOUBLE_CLICK
    };

    public SortGUI(ChestSorterPlugin plugin) {
        this.plugin = plugin;
    }

    public void openGUI(Player player) {
        String title = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("messages.gui-title", "&8Sortowanie Skrzyn"));

        GUIHolder holder = new GUIHolder();
        Inventory inv = Bukkit.createInventory(holder, 27, title);
        holder.inventory = inv;

        ChestSorterPlugin.PlayerSortData data = plugin.getOrCreatePlayerData(player.getUniqueId());

        // Fill all slots with gray glass
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, filler);
        }

        // Toggle button
        inv.setItem(TOGGLE_SLOT, createToggleItem(data.enabled));

        // Method buttons
        for (int i = 0; i < METHODS.length; i++) {
            boolean selected = data.clickType == METHODS[i];
            inv.setItem(METHOD_SLOTS[i], createMethodItem(METHODS[i], selected));
        }

        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof GUIHolder)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 27) return;

        ChestSorterPlugin.PlayerSortData data = plugin.getOrCreatePlayerData(player.getUniqueId());
        Inventory inv = event.getInventory();

        // Toggle ON/OFF
        if (slot == TOGGLE_SLOT) {
            data.enabled = !data.enabled;
            inv.setItem(TOGGLE_SLOT, createToggleItem(data.enabled));
            plugin.savePlayerDataAsync();
            player.playSound(player.getLocation(),
                    data.enabled ? Sound.BLOCK_NOTE_BLOCK_PLING : Sound.UI_BUTTON_CLICK,
                    1.0f, data.enabled ? 2.0f : 0.5f);
            return;
        }

        // Method selection
        for (int i = 0; i < METHOD_SLOTS.length; i++) {
            if (slot == METHOD_SLOTS[i]) {
                if (data.clickType == METHODS[i]) return; // Already selected

                data.clickType = METHODS[i];

                // Update all method items visually
                for (int j = 0; j < METHODS.length; j++) {
                    inv.setItem(METHOD_SLOTS[j], createMethodItem(METHODS[j], METHODS[j] == data.clickType));
                }

                plugin.savePlayerDataAsync();
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                return;
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof GUIHolder) {
            event.setCancelled(true);
        }
    }

    // --- Item builders ---

    private ItemStack createToggleItem(boolean enabled) {
        Material mat = enabled ? Material.LIME_DYE : Material.GRAY_DYE;
        String name = enabled ? "&a&lSortowanie: WLACZONE" : "&c&lSortowanie: WYLACZONE";
        String lore = enabled ? "&7Kliknij aby wylaczyc" : "&7Kliknij aby wlaczyc";

        ItemStack item = createItem(mat, name);
        ItemMeta meta = item.getItemMeta();
        meta.setLore(Collections.singletonList(ChatColor.translateAlternateColorCodes('&', lore)));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createMethodItem(ClickType method, boolean selected) {
        Material mat = selected ? Material.LIME_STAINED_GLASS_PANE : Material.WHITE_STAINED_GLASS_PANE;
        String color = selected ? "&a" : "&7";
        String displayName = ChestSorterPlugin.clickTypeDisplayName(method);

        ItemStack item = createItem(mat, color + displayName);
        ItemMeta meta = item.getItemMeta();

        if (selected) {
            meta.addEnchant(Enchantment.DURABILITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.setLore(Collections.singletonList(
                    ChatColor.translateAlternateColorCodes('&', "&a► Wybrane")));
        } else {
            meta.setLore(Collections.singletonList(
                    ChatColor.translateAlternateColorCodes('&', "&7Kliknij aby wybrac")));
        }

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        item.setItemMeta(meta);
        return item;
    }

    // --- GUI Holder (used to identify our GUI inventories) ---

    static class GUIHolder implements InventoryHolder {
        Inventory inventory;

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
