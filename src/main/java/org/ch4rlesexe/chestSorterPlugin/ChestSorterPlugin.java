// src/main/java/org/ch4rlesexe/chestSorterPlugin/ChestSorterPlugin.java
package org.ch4rlesexe.chestSorterPlugin;

import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class ChestSorterPlugin extends JavaPlugin {

    private final FileConfiguration config = getConfig();

    private static final List<ClickType> validClickTypes = new ArrayList<>();

    private static final List<InventoryType> validInventoryTypes = new ArrayList<>();

    @Override
    public void onEnable() {
        loadConfig();
        getServer().getPluginManager().registerEvents(new ChestSortListener(), this);
        getLogger().info("ChestSorter enabled!");

    }

    private void loadConfig() {
        config.addDefault("enableClickTypeShiftRight", true);
        config.addDefault("enableInventoryTypeChest", true);
        config.addDefault("enableInventoryTypeEnderChest", true);
        config.addDefault("enableInventoryTypeBarrel", true);
        config.addDefault("enableInventoryTypeShulker", true);
        config.addDefault("enableInventoryTypePlayer", false);
        config.options().copyDefaults(true);
        saveConfig();
        this.saveDefaultConfig();

        if (config.getBoolean("enableClickTypeShiftRight")) {
            validClickTypes.add(ClickType.SHIFT_RIGHT);
        }
        if (config.getBoolean("enableInventoryTypeChest")) {
            validInventoryTypes.add(InventoryType.CHEST);
        }
        if (config.getBoolean("enableInventoryTypeEnderChest")) {
            validInventoryTypes.add(InventoryType.ENDER_CHEST);
        }
        if (config.getBoolean("enableInventoryTypeBarrel")) {
            validInventoryTypes.add(InventoryType.BARREL);
        }
        if (config.getBoolean("enableInventoryTypeShulker")) {
            validInventoryTypes.add(InventoryType.SHULKER_BOX);
        }
        if (config.getBoolean("enableInventoryTypePlayer")) {
            validInventoryTypes.add(InventoryType.PLAYER);
        }
    }

    static class ChestSortListener implements Listener {

        @EventHandler
        public void onInventoryClick(InventoryClickEvent event) {
            // ONLY trigger on configured click types
            if(validClickTypes.stream().noneMatch(t -> t == event.getClick())) return;

            if (!(event.getWhoClicked() instanceof Player)) return;
            Player player = (Player) event.getWhoClicked();

            Inventory topInv = event.getView().getTopInventory();
            if (event.getRawSlot() >= topInv.getSize()) return;
            // ONLY trigger on configured inventory types
            if(validInventoryTypes.stream().noneMatch(t -> t == topInv.getType())) return;

            event.setCancelled(true);
            sortInventory(topInv);
            player.sendMessage("§aChest sorted!");
        }

        private void sortInventory(Inventory inv) {
            List<ItemStack> items = new ArrayList<>();
            for (int i = 0; i < inv.getSize(); i++) {
                ItemStack it = inv.getItem(i);
                if (it != null && it.getType() != Material.AIR) {
                    items.add(it);
                    inv.setItem(i, null);
                }
            }

            Map<String, Integer> countMap = new HashMap<>();
            Map<String, ItemStack> protoMap = new HashMap<>();
            List<ItemStack> nonStackable = new ArrayList<>();

            for (ItemStack it : items) {
                if (it.getMaxStackSize() > 1) {
                    String key = getItemKey(it);
                    countMap.put(key, countMap.getOrDefault(key, 0) + it.getAmount());
                    protoMap.computeIfAbsent(key, k -> {
                        ItemStack proto = it.clone();
                        proto.setAmount(1);
                        return proto;
                    });
                } else {
                    nonStackable.add(it);
                }
            }

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

            // alphabetical sorting
            combined.sort(Comparator.comparing(this::getItemSortName, String.CASE_INSENSITIVE_ORDER));

            int idx = 0;
            for (ItemStack it : combined) {
                if (idx >= inv.getSize()) break;
                inv.setItem(idx++, it);
            }
        }

        private String getItemKey(ItemStack item) {
            StringBuilder key = new StringBuilder(item.getType().toString());

            if (item.hasItemMeta()) {
                ItemMeta meta = item.getItemMeta();

                //  custom display name
                if (meta.hasDisplayName()) {
                    key.append("|name=").append(meta.getDisplayName());
                }

                // full meta (enchantments, lore, potion data, book data, etc.) serialized
                key.append("|meta=").append(meta.serialize().toString());

                // loadstone compass
                if (meta instanceof CompassMeta cm && cm.hasLodestone()) {
                    Location loc = cm.getLodestone();
                    key.append("|lodestone=")
                            .append(loc.getWorld().getName())
                            .append("@")
                            .append(loc.getBlockX()).append(",")
                            .append(loc.getBlockY()).append(",")
                            .append(loc.getBlockZ());
                }

                // map id
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
}
