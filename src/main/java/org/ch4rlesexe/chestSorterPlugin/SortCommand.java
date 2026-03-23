package org.ch4rlesexe.chestSorterPlugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SortCommand implements CommandExecutor, TabCompleter {

    private final ChestSorterPlugin plugin;

    public SortCommand(ChestSorterPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Ta komenda jest tylko dla graczy!");
            return true;
        }

        if (!player.hasPermission("chestsort.sort")) {
            player.sendMessage(plugin.formatMessage("messages.no-permission", ""));
            return true;
        }

        // No arguments -> open GUI
        if (args.length == 0) {
            plugin.getSortGUI().openGUI(player);
            return true;
        }

        String arg = args[0].toLowerCase();

        // Reload (admin only)
        if (arg.equals("reload")) {
            if (!player.hasPermission("chestsort.admin")) {
                player.sendMessage(plugin.formatMessage("messages.no-permission", ""));
                return true;
            }
            plugin.reloadPlugin();
            player.sendMessage(plugin.formatMessage("messages.reloaded", ""));
            return true;
        }

        // Help
        if (arg.equals("help") || arg.equals("pomoc")) {
            showHelp(player);
            return true;
        }

        // Enable all
        if (arg.equals("on")) {
            ChestSorterPlugin.PlayerSortData data = plugin.getOrCreatePlayerData(player.getUniqueId());
            data.chestEnabled = true;
            if (!ChestSorterPlugin.VALID_CLICK_TYPES.contains(data.chestClickType)) {
                data.chestClickType = plugin.getDefaultClickType();
            }
            data.eqEnabled = true;
            if (!ChestSorterPlugin.VALID_CLICK_TYPES.contains(data.eqClickType)) {
                data.eqClickType = plugin.getDefaultClickType();
            }
            plugin.savePlayerDataAsync();
            String method = ChestSorterPlugin.clickTypeDisplayName(data.chestClickType);
            player.sendMessage(plugin.formatMessage("messages.enabled", "").replace("%method%", method));
            return true;
        }

        // Disable all
        if (arg.equals("off") || arg.equals("brak")) {
            ChestSorterPlugin.PlayerSortData data = plugin.getOrCreatePlayerData(player.getUniqueId());
            data.chestEnabled = false;
            data.eqEnabled = false;
            plugin.savePlayerDataAsync();
            player.sendMessage(plugin.formatMessage("messages.disabled", ""));
            return true;
        }

        // Try to parse as click type method (sets for both)
        ClickType clickType = ChestSorterPlugin.CLICK_TYPE_ALIASES.get(arg);
        if (clickType != null) {
            ChestSorterPlugin.PlayerSortData data = plugin.getOrCreatePlayerData(player.getUniqueId());
            data.chestClickType = clickType;
            data.chestEnabled = true;
            data.eqClickType = clickType;
            data.eqEnabled = true;
            plugin.savePlayerDataAsync();
            String method = ChestSorterPlugin.clickTypeDisplayName(clickType);
            player.sendMessage(plugin.formatMessage("messages.method-set", "").replace("%method%", method));
            return true;
        }

        // Unknown argument -> show help
        showHelp(player);
        return true;
    }

    private void showHelp(Player player) {
        ChestSorterPlugin.PlayerSortData data = plugin.getPlayerData(player.getUniqueId());
        if (data != null && (data.chestEnabled || data.eqEnabled)) {
            String chestStatus = data.chestEnabled
                    ? "&awl. &7(" + ChestSorterPlugin.clickTypeDisplayName(data.chestClickType) + ")"
                    : "&cwyl.";
            String eqStatus = data.eqEnabled
                    ? "&awl. &7(" + ChestSorterPlugin.clickTypeDisplayName(data.eqClickType) + ")"
                    : "&cwyl.";
            player.sendMessage(plugin.formatMessage("messages.status-detailed", "")
                    .replace("%chest%", org.bukkit.ChatColor.translateAlternateColorCodes('&', chestStatus))
                    .replace("%eq%", org.bukkit.ChatColor.translateAlternateColorCodes('&', eqStatus)));
        } else {
            player.sendMessage(plugin.formatMessage("messages.status-off", ""));
        }
        for (String line : plugin.getHelpMessages()) {
            player.sendMessage(line);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) return Collections.emptyList();
        if (!sender.hasPermission("chestsort.sort")) return Collections.emptyList();

        String prefix = args[0].toLowerCase();
        List<String> completions = new ArrayList<>();

        completions.add("on");
        completions.add("off");
        completions.add("help");
        completions.addAll(ChestSorterPlugin.TAB_METHODS);

        if (sender.hasPermission("chestsort.admin")) {
            completions.add("reload");
        }

        List<String> filtered = new ArrayList<>();
        for (String c : completions) {
            if (c.startsWith(prefix)) {
                filtered.add(c);
            }
        }
        return filtered;
    }
}
