package org.ch4rlesexe.chestSorterPlugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class ChestSorterPlugin extends JavaPlugin {

    private final EnumSet<InventoryType> validInventoryTypes = EnumSet.noneOf(InventoryType.class);
    private final ConcurrentHashMap<UUID, PlayerSortData> playerData = new ConcurrentHashMap<>();
    private File playerDataFile;
    private ClickType defaultClickType = ClickType.SHIFT_RIGHT;
    private SortGUI sortGUI;

    // User-friendly aliases -> ClickType
    static final LinkedHashMap<String, ClickType> CLICK_TYPE_ALIASES = new LinkedHashMap<>();
    static {
        CLICK_TYPE_ALIASES.put("shift", ClickType.SHIFT_RIGHT);
        CLICK_TYPE_ALIASES.put("shift_right", ClickType.SHIFT_RIGHT);
        CLICK_TYPE_ALIASES.put("shiftrightclick", ClickType.SHIFT_RIGHT);
        CLICK_TYPE_ALIASES.put("shift_left", ClickType.SHIFT_LEFT);
        CLICK_TYPE_ALIASES.put("shiftleftclick", ClickType.SHIFT_LEFT);
        CLICK_TYPE_ALIASES.put("right", ClickType.RIGHT);
        CLICK_TYPE_ALIASES.put("rightclick", ClickType.RIGHT);
        CLICK_TYPE_ALIASES.put("left", ClickType.LEFT);
        CLICK_TYPE_ALIASES.put("leftclick", ClickType.LEFT);
        CLICK_TYPE_ALIASES.put("middle", ClickType.MIDDLE);
        CLICK_TYPE_ALIASES.put("middleclick", ClickType.MIDDLE);
        CLICK_TYPE_ALIASES.put("double", ClickType.DOUBLE_CLICK);
        CLICK_TYPE_ALIASES.put("doubleclick", ClickType.DOUBLE_CLICK);
    }

    // Primary aliases shown in tab-complete
    static final List<String> TAB_METHODS = Arrays.asList(
            "shift", "shift_left", "right", "left", "middle", "double"
    );

    @Override
    public void onEnable() {
        if (!new File(getDataFolder(), "config.yml").exists()) {
            saveDefaultConfig();
        }
        reloadConfig();
        loadTogglesFromConfig();

        playerDataFile = new File(getDataFolder(), "playerdata.yml");
        loadPlayerDataAsync();

        // Register listeners
        getServer().getPluginManager().registerEvents(new SortListener(this), this);
        sortGUI = new SortGUI(this);
        getServer().getPluginManager().registerEvents(sortGUI, this);

        // Register command
        SortCommand cmd = new SortCommand(this);
        if (getCommand("sortowanie") != null) {
            getCommand("sortowanie").setExecutor(cmd);
            getCommand("sortowanie").setTabCompleter(cmd);
        } else {
            getLogger().severe("Could not register /sortowanie command! Check plugin.yml");
        }

        // Auto-save player data every 5 minutes (async)
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, this::savePlayerDataToDisk, 6000L, 6000L);

        getLogger().info("ChestSorter enabled!");
    }

    @Override
    public void onDisable() {
        savePlayerDataToDisk();
        getLogger().info("ChestSorter disabled!");
    }

    void reloadPlugin() {
        reloadConfig();
        loadTogglesFromConfig();
    }

    private void loadTogglesFromConfig() {
        validInventoryTypes.clear();

        if (getConfig().getBoolean("enableInventoryTypeChest", true))
            validInventoryTypes.add(InventoryType.CHEST);
        if (getConfig().getBoolean("enableInventoryTypeEnderChest", true))
            validInventoryTypes.add(InventoryType.ENDER_CHEST);
        if (getConfig().getBoolean("enableInventoryTypeBarrel", true))
            validInventoryTypes.add(InventoryType.BARREL);
        if (getConfig().getBoolean("enableInventoryTypeShulker", true))
            validInventoryTypes.add(InventoryType.SHULKER_BOX);
        if (getConfig().getBoolean("enableInventoryTypeDropper", true))
            validInventoryTypes.add(InventoryType.DROPPER);
        if (getConfig().getBoolean("enableInventoryTypeDispenser", true))
            validInventoryTypes.add(InventoryType.DISPENSER);
        if (getConfig().getBoolean("enableInventoryTypePlayer", false))
            validInventoryTypes.add(InventoryType.PLAYER);

        String clickStr = getConfig().getString("defaultClickType", "shift").toLowerCase();
        ClickType parsed = CLICK_TYPE_ALIASES.get(clickStr);
        defaultClickType = (parsed != null) ? parsed : ClickType.SHIFT_RIGHT;
    }

    // --- GUI access ---

    SortGUI getSortGUI() {
        return sortGUI;
    }

    // --- Player data access ---

    PlayerSortData getPlayerData(UUID uuid) {
        return playerData.get(uuid);
    }

    PlayerSortData getOrCreatePlayerData(UUID uuid) {
        return playerData.computeIfAbsent(uuid, k -> new PlayerSortData(false, defaultClickType));
    }

    boolean isValidInventoryType(InventoryType type) {
        return validInventoryTypes.contains(type);
    }

    ClickType getDefaultClickType() {
        return defaultClickType;
    }

    // --- Messages ---

    String formatMessage(String path, String containerName) {
        String raw = getConfig().getString(path, "");
        raw = raw.replace("%container%", containerName);
        return ChatColor.translateAlternateColorCodes('&', raw);
    }

    List<String> getHelpMessages() {
        List<String> raw = getConfig().getStringList("messages.help");
        List<String> result = new ArrayList<>(raw.size());
        for (String line : raw) {
            result.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        return result;
    }

    static String toDisplayName(InventoryType type) {
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

    static String clickTypeDisplayName(ClickType type) {
        switch (type) {
            case SHIFT_RIGHT: return "Shift+Prawy";
            case SHIFT_LEFT: return "Shift+Lewy";
            case RIGHT: return "Prawy klik";
            case LEFT: return "Lewy klik";
            case MIDDLE: return "Srodkowy klik";
            case DOUBLE_CLICK: return "Podwojny klik";
            default: return type.name();
        }
    }

    // --- Persistence (async) ---

    void savePlayerDataAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(this, this::savePlayerDataToDisk);
    }

    private void savePlayerDataToDisk() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, PlayerSortData> entry : playerData.entrySet()) {
            String path = "players." + entry.getKey().toString();
            yaml.set(path + ".enabled", entry.getValue().enabled);
            yaml.set(path + ".clickType", entry.getValue().clickType.name());
        }
        try {
            yaml.save(playerDataFile);
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Could not save player data!", e);
        }
    }

    private void loadPlayerDataAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            if (!playerDataFile.exists()) return;
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(playerDataFile);
            ConfigurationSection section = yaml.getConfigurationSection("players");
            if (section == null) return;

            for (String uuidStr : section.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    boolean enabled = section.getBoolean(uuidStr + ".enabled", false);
                    String clickStr = section.getString(uuidStr + ".clickType", "SHIFT_RIGHT");
                    ClickType click;
                    try {
                        click = ClickType.valueOf(clickStr);
                    } catch (IllegalArgumentException e) {
                        click = defaultClickType;
                    }
                    playerData.put(uuid, new PlayerSortData(enabled, click));
                } catch (IllegalArgumentException e) {
                    getLogger().warning("Invalid UUID in playerdata: " + uuidStr);
                }
            }
            getLogger().info("Loaded " + playerData.size() + " player settings.");
        });
    }

    // --- Player sort data ---

    static class PlayerSortData {
        volatile boolean enabled;
        volatile ClickType clickType;

        PlayerSortData(boolean enabled, ClickType clickType) {
            this.enabled = enabled;
            this.clickType = clickType;
        }
    }
}
