package org.ch4rlesexe.untitled1;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class ChestSorterPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new ChestSortListener(), this);
        getLogger().info("ChestSorter enabled!");
    }
}

class ChestSortListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getClick() != ClickType.SHIFT_RIGHT) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        Inventory topInv = event.getView().getTopInventory();
        if (event.getRawSlot() >= topInv.getSize()) {
            return;
        }
        if (topInv.getType() != InventoryType.CHEST) {
            return;
        }
        event.setCancelled(true);
        sortInventory(topInv);
        player.sendMessage("§aChest sorted!");
    }

    /**
     * Sorts the given inventory by stacking similar items and then ordering them alphabetically.
     */
    private void sortInventory(Inventory inv) {
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                items.add(item);
                inv.setItem(i, null); // Clear the slot.
            }
        }

        Map<String, Integer> stackableMap = new HashMap<>();
        Map<String, ItemStack> representativeMap = new HashMap<>();
        List<ItemStack> nonStackableItems = new ArrayList<>();

        for (ItemStack item : items) {
            if (item.getMaxStackSize() > 1) {
                String key = getItemKey(item);
                int count = stackableMap.getOrDefault(key, 0);
                stackableMap.put(key, count + item.getAmount());
                if (!representativeMap.containsKey(key)) {
                    ItemStack rep = item.clone();
                    rep.setAmount(1);
                    representativeMap.put(key, rep);
                }
            } else {
                nonStackableItems.add(item);
            }
        }

        List<ItemStack> combinedItems = new ArrayList<>();
        for (String key : stackableMap.keySet()) {
            int total = stackableMap.get(key);
            ItemStack rep = representativeMap.get(key);
            int maxStack = rep.getMaxStackSize();
            while (total > 0) {
                int stackSize = Math.min(total, maxStack);
                total -= stackSize;
                ItemStack newStack = rep.clone();
                newStack.setAmount(stackSize);
                combinedItems.add(newStack);
            }
        }

        combinedItems.addAll(nonStackableItems);

        combinedItems.sort((a, b) -> {
            String nameA = getItemSortName(a);
            String nameB = getItemSortName(b);
            return nameA.compareToIgnoreCase(nameB);
        });

        int index = 0;
        for (ItemStack item : combinedItems) {
            if (index < inv.getSize()) {
                inv.setItem(index, item);
                index++;
            }
        }
    }

    /**
     * Generates a key for an ItemStack based on its type, durability, and display name.
     */
    private String getItemKey(ItemStack item) {
        StringBuilder key = new StringBuilder();
        key.append(item.getType().toString());
        if (item.getDurability() != 0) {
            key.append(":").append(item.getDurability());
        }
        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta.hasDisplayName()) {
                key.append(":").append(meta.getDisplayName());
            }
        }
        return key.toString();
    }

    /**
     * Returns the name used for sorting an item. If it has a custom display name, that is used;
     * otherwise, the material’s name is used.
     */
    private String getItemSortName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        return item.getType().toString();
    }
}
