package org.ch4rlesexe.chestSorterPlugin;

import org.bukkit.ChatColor;
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

import java.io.File;
import java.util.*;

public class ChestSorterPlugin extends JavaPlugin {

    private FileConfiguration config;

    private static final List<ClickType> validClickTypes = new ArrayList<>();
    private static final List<InventoryType> validInventoryTypes = new ArrayList<>();

    @Override
    public void onEnable() {
        if (!new File(getDataFolder(), "config.yml").exists()) {
            saveDefaultConfig();
        }

        reloadConfig();
        this.config = getConfig();

        loadTogglesFromConfig();

        getServer().getPluginManager().registerEvents(new ChestSortListener(), this);
        getLogger().info("ChestSorter enabled!");
    }

    private void loadTogglesFromConfig() {
        validClickTypes.clear();
        validInventoryTypes.clear();

        // Click toggles
        if (config.getBoolean("enableClickTypeShiftRight", true)) {
            validClickTypes.add(ClickType.SHIFT_RIGHT);
        }

        // Container toggles
        if (config.getBoolean("enableInventoryTypeChest", true)) {
            validInventoryTypes.add(InventoryType.CHEST);
        }
        if (config.getBoolean("enableInventoryTypeEnderChest", true)) {
            validInventoryTypes.add(InventoryType.ENDER_CHEST);
        }
        if (config.getBoolean("enableInventoryTypeBarrel", true)) {
            validInventoryTypes.add(InventoryType.BARREL);
        }
        if (config.getBoolean("enableInventoryTypeShulker", true)) {
            validInventoryTypes.add(InventoryType.SHULKER_BOX);
        }
        if (config.getBoolean("enableInventoryTypeDropper", true)) {
            validInventoryTypes.add(InventoryType.DROPPER);
        }
        if (config.getBoolean("enableInventoryTypeDispenser", true)) {
            validInventoryTypes.add(InventoryType.DISPENSER);
        }
        if (config.getBoolean("enableInventoryTypePlayer", false)) {
            validInventoryTypes.add(InventoryType.PLAYER);
        }
    }

    private String formatMessage(String path, String containerName) {
        String raw = config.getString(path, "&a%container% sorted!");
        raw = raw.replace("%container%", containerName);
        return ChatColor.translateAlternateColorCodes('&', raw);
    }

    private static String toDisplayName(InventoryType type) {
        switch (type) {
            case CHEST: return "Chest";
            case ENDER_CHEST: return "Ender Chest";
            case BARREL: return "Barrel";
            case SHULKER_BOX: return "Shulker Box";
            case DROPPER: return "Dropper";
            case DISPENSER: return "Dispenser";
            case PLAYER: return "Player Inventory";
            default:
                String name = type.name().toLowerCase(Locale.ROOT).replace('_', ' ');
                String[] parts = name.split(" ");
                StringBuilder sb = new StringBuilder();
                for (String p : parts) {
                    if (p.isEmpty()) continue;
                    sb.append(Character.toUpperCase(p.charAt(0)))
                            .append(p.length() > 1 ? p.substring(1) : "")
                            .append(' ');
                }
                return sb.toString().trim();
        }
    }

    static class ChestSortListener implements Listener {

        @EventHandler
        public void onInventoryClick(InventoryClickEvent event) {
            // ONLY trigger on configured click types
            if (validClickTypes.stream().noneMatch(t -> t == event.getClick())) return;

            if (!(event.getWhoClicked() instanceof Player)) return;
            Player player = (Player) event.getWhoClicked();

            Inventory topInv = event.getView().getTopInventory();
            if (event.getRawSlot() >= topInv.getSize()) return;

            // ONLY trigger on configured inventory types
            if (validInventoryTypes.stream().noneMatch(t -> t == topInv.getType())) return;

            event.setCancelled(true);
            sortInventory(topInv);

            ChestSorterPlugin plugin = JavaPlugin.getPlugin(ChestSorterPlugin.class);
            String containerName = ChestSorterPlugin.toDisplayName(topInv.getType());
            player.sendMessage(plugin.formatMessage("messages.sorted", containerName));
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

                // custom display name
                if (meta.hasDisplayName()) {
                    key.append("|name=").append(meta.getDisplayName());
                }

                // meta (enchants, lore, potion, book, etc.)
                key.append("|meta=").append(meta.serialize().toString());

                // lodestone compass
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
