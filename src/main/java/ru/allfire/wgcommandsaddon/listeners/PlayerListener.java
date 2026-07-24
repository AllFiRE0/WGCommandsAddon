package ru.allfire.wgcommandsaddon.listeners;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import ru.allfire.wgcommandsaddon.WGCommandsAddon;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class PlayerListener implements Listener {

    private final WGCommandsAddon plugin;
    private final Map<Player, String> playerRegionStates = new HashMap<>();
    private final Map<Player, Long> lastCheckTime = new HashMap<>();
    private boolean placeholderAPIEnabled = false;

    public PlayerListener(WGCommandsAddon plugin) {
        this.plugin = plugin;
        // Проверяем наличие PlaceholderAPI
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            placeholderAPIEnabled = true;
            plugin.getLogger().info("PlaceholderAPI найден! Внешние заполнители поддерживаются.");
        } else {
            plugin.getLogger().warning("PlaceholderAPI НЕ найден! Внешние заполнители не будут работать.");
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockY() == event.getTo().getBlockY() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        if (!event.getFrom().getWorld().equals(event.getTo().getWorld())) {
            playerRegionStates.remove(player);
            lastCheckTime.remove(player);
            return;
        }

        long currentTime = System.currentTimeMillis();
        Long lastTime = lastCheckTime.get(player);
        if (lastTime != null && (currentTime - lastTime) < 100) {
            return;
        }
        lastCheckTime.put(player, currentTime);

        try {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionManager regionManager = container.get(BukkitAdapter.adapt(event.getTo().getWorld()));
            
            if (regionManager == null) return;

            BlockVector3 toVector = BlockVector3.at(
                event.getTo().getX(),
                event.getTo().getY(),
                event.getTo().getZ()
            );
            
            ApplicableRegionSet applicableRegions = regionManager.getApplicableRegions(toVector);
            
            String currentRegion = null;
            for (ProtectedRegion region : applicableRegions) {
                if (region.getFlag(WGCommandsAddon.ONE_COMMAND_ASCONSOLE_FLAG) != null ||
                    region.getFlag(WGCommandsAddon.ONE_COMMAND_ASPLAYER_FLAG) != null ||
                    region.getFlag(WGCommandsAddon.ONE_PERM_COMMAND_ASCONSOLE_FLAG) != null ||
                    region.getFlag(WGCommandsAddon.ONE_PERM_COMMAND_ASPLAYER_FLAG) != null) {
                    currentRegion = region.getId();
                    break;
                }
            }

            String previousRegion = playerRegionStates.get(player);
            if (!Objects.equals(currentRegion, previousRegion)) {
                playerRegionStates.put(player, currentRegion);
                
                if (currentRegion != null) {
                    handleRegionEntry(player, currentRegion);
                } else if (previousRegion != null) {
                    handleRegionExit(player, previousRegion);
                }
            }

        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка при проверке региона: " + e.getMessage());
        }
    }

    private void handleRegionEntry(Player player, String regionName) {
        try {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionManager regionManager = container.get(BukkitAdapter.adapt(player.getWorld()));
            if (regionManager == null) return;
            
            ProtectedRegion region = regionManager.getRegion(regionName);
            if (region == null) return;

            // 1. one-command-asconsole (консоль, без прав)
            String consoleCommand = region.getFlag(WGCommandsAddon.ONE_COMMAND_ASCONSOLE_FLAG);
            if (consoleCommand != null && !consoleCommand.isEmpty()) {
                String parsed = replacePlaceholders(player, consoleCommand, regionName);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
                plugin.getLogger().info("[one-command-asconsole] " + player.getName() + " вошел в " + regionName);
            }

            // 2. one-command-asplayer (игрок, без прав)
            String playerCommand = region.getFlag(WGCommandsAddon.ONE_COMMAND_ASPLAYER_FLAG);
            if (playerCommand != null && !playerCommand.isEmpty()) {
                String parsed = replacePlaceholders(player, playerCommand, regionName);
                player.performCommand(parsed);
                plugin.getLogger().info("[one-command-asplayer] " + player.getName() + " вошел в " + regionName);
            }

            // 3. one-perm-command-asconsole (консоль, с правом)
            String permConsoleCommand = region.getFlag(WGCommandsAddon.ONE_PERM_COMMAND_ASCONSOLE_FLAG);
            if (permConsoleCommand != null && !permConsoleCommand.isEmpty()) {
                if (player.hasPermission("wgca.onecommand")) {
                    String parsed = replacePlaceholders(player, permConsoleCommand, regionName);
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
                    plugin.getLogger().info("[one-perm-command-asconsole] " + player.getName() + " вошел в " + regionName);
                }
            }

            // 4. one-perm-command-asplayer (игрок, с правом)
            String permPlayerCommand = region.getFlag(WGCommandsAddon.ONE_PERM_COMMAND_ASPLAYER_FLAG);
            if (permPlayerCommand != null && !permPlayerCommand.isEmpty()) {
                if (player.hasPermission("wgca.onecommand")) {
                    String parsed = replacePlaceholders(player, permPlayerCommand, regionName);
                    player.performCommand(parsed);
                    plugin.getLogger().info("[one-perm-command-asplayer] " + player.getName() + " вошел в " + regionName);
                }
            }

        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка при выполнении команд входа: " + e.getMessage());
        }
    }

    private void handleRegionExit(Player player, String regionName) {
        try {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionManager regionManager = container.get(BukkitAdapter.adapt(player.getWorld()));
            if (regionManager == null) return;
            
            ProtectedRegion region = regionManager.getRegion(regionName);
            if (region == null) return;

            // 1. one-command-asconsole (консоль, без прав)
            String consoleCommand = region.getFlag(WGCommandsAddon.ONE_COMMAND_ASCONSOLE_FLAG);
            if (consoleCommand != null && !consoleCommand.isEmpty()) {
                String parsed = replacePlaceholders(player, consoleCommand, regionName);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
                plugin.getLogger().info("[one-command-asconsole] " + player.getName() + " вышел из " + regionName);
            }

            // 2. one-command-asplayer (игрок, без прав)
            String playerCommand = region.getFlag(WGCommandsAddon.ONE_COMMAND_ASPLAYER_FLAG);
            if (playerCommand != null && !playerCommand.isEmpty()) {
                String parsed = replacePlaceholders(player, playerCommand, regionName);
                player.performCommand(parsed);
                plugin.getLogger().info("[one-command-asplayer] " + player.getName() + " вышел из " + regionName);
            }

            // 3. one-perm-command-asconsole (консоль, с правом)
            String permConsoleCommand = region.getFlag(WGCommandsAddon.ONE_PERM_COMMAND_ASCONSOLE_FLAG);
            if (permConsoleCommand != null && !permConsoleCommand.isEmpty()) {
                if (player.hasPermission("wgca.onecommand")) {
                    String parsed = replacePlaceholders(player, permConsoleCommand, regionName);
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
                    plugin.getLogger().info("[one-perm-command-asconsole] " + player.getName() + " вышел из " + regionName);
                }
            }

            // 4. one-perm-command-asplayer (игрок, с правом)
            String permPlayerCommand = region.getFlag(WGCommandsAddon.ONE_PERM_COMMAND_ASPLAYER_FLAG);
            if (permPlayerCommand != null && !permPlayerCommand.isEmpty()) {
                if (player.hasPermission("wgca.onecommand")) {
                    String parsed = replacePlaceholders(player, permPlayerCommand, regionName);
                    player.performCommand(parsed);
                    plugin.getLogger().info("[one-perm-command-asplayer] " + player.getName() + " вышел из " + regionName);
                }
            }

        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка при выполнении команд выхода: " + e.getMessage());
        }
    }

    /**
     * Заменяет плейсхолдеры в команде
     * Поддерживает:
     * - {player} - имя игрока
     * - {region} - название региона
     * - %placeholder% - любые PlaceholderAPI заполнители
     */
    private String replacePlaceholders(Player player, String command, String regionName) {
        String result = command;
        
        // Встроенные заполнители
        result = result.replace("{player}", player.getName());
        result = result.replace("{region}", regionName);
        
        // PlaceholderAPI заполнители (%player_name%, %worldguard_region_name%, и т.д.)
        if (placeholderAPIEnabled) {
            try {
                result = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, result);
            } catch (Exception e) {
                plugin.getLogger().warning("Ошибка при обработке PlaceholderAPI: " + e.getMessage());
            }
        }
        
        return result;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        playerRegionStates.remove(player);
        lastCheckTime.remove(player);
    }
}
