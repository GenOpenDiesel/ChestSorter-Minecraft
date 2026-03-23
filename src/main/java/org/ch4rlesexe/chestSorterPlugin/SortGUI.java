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

    // Layout: 45-slot chest (5 rows)
    // Row 0: filler
    // Row 1: [G] [label] [G] [Brak] [Shift+R] [Shift+L] [G] [G] [G]  <- Skrzynki
    // Row 2: filler
    // Row 3: [G] [label] [G] [Brak] [Shift+R] [Shift+L] [G] [G] [G]  <- EQ
    // Row 4: filler

    // Chest sorting options - row 1
    private static final int CHEST_LABEL_SLOT = 10;
    private static final int[] CHEST_OPTION_SLOTS = {12, 13, 14};

    // EQ sorting options - row 3
    private static final int EQ_LABEL_SLOT = 28;
    private static final int[] EQ_OPTION_SLOTS = {30, 31, 32};

    private static final ClickType[] OPTION_TYPES = {null, ClickType.SHIFT_RIGHT, ClickType.SHIFT_LEFT};
    private static final String[] OPTION_NAMES = {"Brak", "Shift+Prawy", "Shift+Lewy"};

    public SortGUI(ChestSorterPlugin plugin) {
        this.plugin = plugin;
    }

    public void openGUI(Player player) {
        String title = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("messages.gui-title", "&8Sortowanie"));

        GUIHolder holder = new GUIHolder();
        Inventory inv = Bukkit.createInventory(holder, 45, title);
        holder.inventory = inv;

        ChestSorterPlugin.PlayerSortData data = plugin.getOrCreatePlayerData(player.getUniqueId());

        // Fill all slots with gray glass
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 45; i++) {
            inv.setItem(i, filler);
        }

        // Chest sorting label
        inv.setItem(CHEST_LABEL_SLOT, createItem(Material.CHEST, "&6Sortowanie Skrzyn"));

        // Chest sorting option buttons
        for (int i = 0; i < CHEST_OPTION_SLOTS.length; i++) {
            boolean selected = isChestOptionSelected(data, i);
            inv.setItem(CHEST_OPTION_SLOTS[i], createOptionItem(i, selected));
        }

        // EQ sorting label
        inv.setItem(EQ_LABEL_SLOT, createItem(Material.IRON_CHESTPLATE, "&6Sortowanie EQ"));

        // EQ sorting option buttons
        for (int i = 0; i < EQ_OPTION_SLOTS.length; i++) {
            boolean selected = isEqOptionSelected(data, i);
            inv.setItem(EQ_OPTION_SLOTS[i], createOptionItem(i, selected));
        }

        player.openInventory(inv);
    }

    private boolean isChestOptionSelected(ChestSorterPlugin.PlayerSortData data, int index) {
        if (OPTION_TYPES[index] == null) {
            return !data.chestEnabled;
        }
        return data.chestEnabled && data.chestClickType == OPTION_TYPES[index];
    }

    private boolean isEqOptionSelected(ChestSorterPlugin.PlayerSortData data, int index) {
        if (OPTION_TYPES[index] == null) {
            return !data.eqEnabled;
        }
        return data.eqEnabled && data.eqClickType == OPTION_TYPES[index];
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof GUIHolder)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 45) return;

        ChestSorterPlugin.PlayerSortData data = plugin.getOrCreatePlayerData(player.getUniqueId());
        Inventory inv = event.getInventory();

        // Chest sorting option selection
        for (int i = 0; i < CHEST_OPTION_SLOTS.length; i++) {
            if (slot == CHEST_OPTION_SLOTS[i]) {
                if (isChestOptionSelected(data, i)) return;

                if (OPTION_TYPES[i] == null) {
                    data.chestEnabled = false;
                } else {
                    data.chestEnabled = true;
                    data.chestClickType = OPTION_TYPES[i];
                }

                for (int j = 0; j < CHEST_OPTION_SLOTS.length; j++) {
                    inv.setItem(CHEST_OPTION_SLOTS[j], createOptionItem(j, isChestOptionSelected(data, j)));
                }

                plugin.savePlayerDataAsync();
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                return;
            }
        }

        // EQ sorting option selection
        for (int i = 0; i < EQ_OPTION_SLOTS.length; i++) {
            if (slot == EQ_OPTION_SLOTS[i]) {
                if (isEqOptionSelected(data, i)) return;

                if (OPTION_TYPES[i] == null) {
                    data.eqEnabled = false;
                } else {
                    data.eqEnabled = true;
                    data.eqClickType = OPTION_TYPES[i];
                }

                for (int j = 0; j < EQ_OPTION_SLOTS.length; j++) {
                    inv.setItem(EQ_OPTION_SLOTS[j], createOptionItem(j, isEqOptionSelected(data, j)));
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
