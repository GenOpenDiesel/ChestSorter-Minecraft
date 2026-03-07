package org.ch4rlesexe.chestSorterPlugin;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MapMeta;

import java.util.*;

public class SortListener implements Listener {

    private final ChestSorterPlugin plugin;

    public SortListener(ChestSorterPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // Permission check
        if (!player.hasPermission("chestsort.sort")) return;

        // Player settings check
        ChestSorterPlugin.PlayerSortData data = plugin.getPlayerData(player.getUniqueId());
        if (data == null || !data.enabled) return;

        // Click type check (player's chosen method)
        if (event.getClick() != data.clickType) return;

        // Inventory checks
        Inventory topInv = event.getView().getTopInventory();

        // Skip our settings GUI
        if (topInv.getHolder() instanceof SortGUI.GUIHolder) return;

        // Inventory type check (server config)
        if (!plugin.isValidInventoryType(topInv.getType())) return;

        // Must be clicking in the top inventory
        if (event.getRawSlot() >= topInv.getSize()) return;

        event.setCancelled(true);

        // Snapshot items for async processing
        int size = topInv.getSize();
        ItemStack[] snapshot = new ItemStack[size];
        for (int i = 0; i < size; i++) {
            ItemStack item = topInv.getItem(i);
            snapshot[i] = (item != null && item.getType() != Material.AIR) ? item.clone() : null;
        }

        InventoryType invType = topInv.getType();

        // Async sort: compute off main thread, apply on main thread
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            ItemStack[] sorted = computeSortedInventory(snapshot, size);

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!plugin.isEnabled()) return;
                if (!player.isOnline()) return;
                if (player.getOpenInventory().getTopInventory() != topInv) return;

                for (int i = 0; i < size; i++) {
                    topInv.setItem(i, sorted[i]);
                }

                String containerName = ChestSorterPlugin.toDisplayName(invType);
                player.sendMessage(plugin.formatMessage("messages.sorted", containerName));
            });
        });
    }

    private ItemStack[] computeSortedInventory(ItemStack[] snapshot, int size) {
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack item : snapshot) {
            if (item != null) {
                items.add(item);
            }
        }

        // Separate stackable and non-stackable
        Map<String, Integer> countMap = new HashMap<>();
        Map<String, ItemStack> protoMap = new HashMap<>();
        List<ItemStack> nonStackable = new ArrayList<>();

        for (ItemStack it : items) {
            if (it.getMaxStackSize() > 1) {
                String key = getItemKey(it);
                countMap.merge(key, it.getAmount(), Integer::sum);
                protoMap.computeIfAbsent(key, k -> {
                    ItemStack proto = it.clone();
                    proto.setAmount(1);
                    return proto;
                });
            } else {
                nonStackable.add(it);
            }
        }

        // Combine stacks respecting max stack size
        List<ItemStack> combined = new ArrayList<>();
        for (Map.Entry<String, Integer> e : countMap.entrySet()) {
            ItemStack proto = protoMap.get(e.getKey());
            int total = e.getValue();
            int max = proto.getMaxStackSize();
            while (total > 0) {
                int take = Math.min(total, max);
                total -= take;
                ItemStack batch = proto.clone();
                batch.setAmount(take);
                combined.add(batch);
            }
        }
        combined.addAll(nonStackable);

        // Sort alphabetically
        combined.sort(Comparator.comparing(this::getItemSortName, String.CASE_INSENSITIVE_ORDER));

        // Build result array
        ItemStack[] result = new ItemStack[size];
        for (int i = 0; i < combined.size() && i < size; i++) {
            result[i] = combined.get(i);
        }
        return result;
    }

    private String getItemKey(ItemStack item) {
        StringBuilder key = new StringBuilder(item.getType().toString());

        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();

            if (meta.hasDisplayName()) {
                key.append("|name=").append(meta.getDisplayName());
            }

            key.append("|meta=").append(meta.serialize().toString());

            try {
                if (meta instanceof CompassMeta cm && cm.hasLodestone()) {
                    Location loc = cm.getLodestone();
                    if (loc != null && loc.getWorld() != null) {
                        key.append("|lodestone=")
                                .append(loc.getWorld().getName())
                                .append("@")
                                .append(loc.getBlockX()).append(",")
                                .append(loc.getBlockY()).append(",")
                                .append(loc.getBlockZ());
                    }
                }
            } catch (Exception ignored) {
            }

            if (meta instanceof MapMeta mm) {
                key.append("|mapId=").append(mm.getMapId());
            }
        }

        return key.toString();
    }

    private String getItemSortName(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            return meta.getDisplayName();
        }
        return item.getType().toString();
    }
}
