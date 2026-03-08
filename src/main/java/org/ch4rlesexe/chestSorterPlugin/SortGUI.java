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
    // Row 1: [G] [G] [G] [Brak] [Shift+R] [Shift+L] [G] [G] [G]
    // Row 2: filler
    private static final int[] OPTION_SLOTS = {12, 13, 14};
    private static final ClickType[] OPTION_TYPES = {null, ClickType.SHIFT_RIGHT, ClickType.SHIFT_LEFT};
    private static final String[] OPTION_NAMES = {"Brak", "Shift+Prawy", "Shift+Lewy"};

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

        // Option buttons
        for (int i = 0; i < OPTION_SLOTS.length; i++) {
            boolean selected = isOptionSelected(data, i);
            inv.setItem(OPTION_SLOTS[i], createOptionItem(i, selected));
        }

        player.openInventory(inv);
    }

    private boolean isOptionSelected(ChestSorterPlugin.PlayerSortData data, int index) {
        if (OPTION_TYPES[index] == null) {
            // "Brak" is selected when sorting is disabled
            return !data.enabled;
        }
        return data.enabled && data.clickType == OPTION_TYPES[index];
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

        // Option selection
        for (int i = 0; i < OPTION_SLOTS.length; i++) {
            if (slot == OPTION_SLOTS[i]) {
                if (isOptionSelected(data, i)) return; // Already selected

                if (OPTION_TYPES[i] == null) {
                    // "Brak" -> disable sorting
                    data.enabled = false;
                } else {
                    // Method selected -> enable with that method
                    data.enabled = true;
                    data.clickType = OPTION_TYPES[i];
                }

                // Update all option items visually
                for (int j = 0; j < OPTION_SLOTS.length; j++) {
                    inv.setItem(OPTION_SLOTS[j], createOptionItem(j, isOptionSelected(data, j)));
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

    private ItemStack createOptionItem(int index, boolean selected) {
        boolean isBrak = OPTION_TYPES[index] == null;
        Material mat;
        if (selected) {
            mat = isBrak ? Material.RED_STAINED_GLASS_PANE : Material.LIME_STAINED_GLASS_PANE;
        } else {
            mat = Material.WHITE_STAINED_GLASS_PANE;
        }

        String color = selected ? (isBrak ? "&c" : "&a") : "&7";
        ItemStack item = createItem(mat, color + OPTION_NAMES[index]);
        ItemMeta meta = item.getItemMeta();

        if (selected) {
            meta.addEnchant(Enchantment.DURABILITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            if (isBrak) {
                meta.setLore(Collections.singletonList(
                        ChatColor.translateAlternateColorCodes('&', "&c► Wylaczone")));
            } else {
                meta.setLore(Collections.singletonList(
                        ChatColor.translateAlternateColorCodes('&', "&a► Wybrane")));
            }
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
