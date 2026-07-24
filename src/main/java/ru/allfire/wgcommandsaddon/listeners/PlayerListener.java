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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerListener implements Listener {

    private final WGCommandsAddon plugin;
    private final Map<Player, String> playerRegionStates = new HashMap<>();
    private final Map<Player, Long> lastCheckTime = new HashMap<>();
    private final boolean placeholderAPIEnabled;
    
    // Кэш кулдаунов: ключ = "игрок:регион:команда", значение = время последнего выполнения
    private final Map<String, Long> cooldownCache = new ConcurrentHashMap<>();

    public PlayerListener(WGCommandsAddon plugin, boolean placeholderAPIEnabled) {
        this.plugin = plugin;
        this.placeholderAPIEnabled = placeholderAPIEnabled;
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
                if (region.getFlag(WGCommandsAddon.MORE_CMD_PLAYER_FLAG) != null ||
                    region.getFlag(WGCommandsAddon.MORE_CMD_CONSOLE_FLAG) != null ||
                    region.getFlag(WGCommandsAddon.MORE_PERM_CMD_PLAYER_FLAG) != null ||
                    region.getFlag(WGCommandsAddon.MORE_PERM_CMD_CONSOLE_FLAG) != null) {
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

    private boolean isAdmin(Player player) {
        return player.hasPermission("*") || player.hasPermission("bukkit.command.*");
    }

    /**
     * Парсит строку флага в список команд с задержками
     * Формат: "задержка_сек команда||задержка_сек команда"
     * Пример: "10 give {player} bone 1||30 give {player} diamond 1"
     */
    private List<CommandWithCooldown> parseCommands(String flagValue) {
        List<CommandWithCooldown> result = new ArrayList<>();
        if (flagValue == null || flagValue.isEmpty()) return result;
        
        String[] parts = flagValue.split("\\|\\|");
        for (String part : parts) {
            part = part.trim();
            if (part.isEmpty()) continue;
            
            int cooldown = 0;
            String command = part;
            
            // Проверяем, есть ли задержка в начале
            // Формат: "10 команда" или просто "команда"
            String[] spaceSplit = part.split(" ", 2);
            if (spaceSplit.length == 2) {
                try {
                    int parsedCooldown = Integer.parseInt(spaceSplit[0]);
                    if (parsedCooldown >= 0) {
                        cooldown = parsedCooldown;
                        command = spaceSplit[1];
                    }
                } catch (NumberFormatException ignored) {
                    // Если не число, то вся строка - команда
                }
            }
            
            result.add(new CommandWithCooldown(command, cooldown));
        }
        
        return result;
    }

    /**
     * Проверяет кулдаун для конкретной команды
     */
    private boolean checkCooldown(Player player, String regionName, String command, int cooldownSeconds) {
        if (cooldownSeconds <= 0) return true; // Нет кулдауна
        
        String key = player.getUniqueId() + ":" + regionName + ":" + command;
        Long lastExecution = cooldownCache.get(key);
        long currentTime = System.currentTimeMillis();
        
        if (lastExecution == null) {
            cooldownCache.put(key, currentTime);
            return true;
        }
        
        long elapsedSeconds = (currentTime - lastExecution) / 1000;
        if (elapsedSeconds >= cooldownSeconds) {
            cooldownCache.put(key, currentTime);
            return true;
        }
        
        return false; // Кулдаун еще активен
    }

    private void handleRegionEntry(Player player, String regionName) {
        try {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionManager regionManager = container.get(BukkitAdapter.adapt(player.getWorld()));
            if (regionManager == null) return;
            
            ProtectedRegion region = regionManager.getRegion(regionName);
            if (region == null) return;

            boolean isAdmin = isAdmin(player);

            // 1. more-cmd-player (игрок, все)
            String playerCmdFlag = region.getFlag(WGCommandsAddon.MORE_CMD_PLAYER_FLAG);
            if (playerCmdFlag != null && !playerCmdFlag.isEmpty()) {
                List<CommandWithCooldown> commands = parseCommands(playerCmdFlag);
                for (CommandWithCooldown cmd : commands) {
                    if (checkCooldown(player, regionName, cmd.command, cmd.cooldownSeconds)) {
                        String parsed = replacePlaceholders(player, cmd.command, regionName);
                        player.performCommand(parsed);
                        plugin.getLogger().info("[more-cmd-player] " + player.getName() + " -> " + parsed + " (кулдаун " + cmd.cooldownSeconds + "с)");
                    }
                }
            }

            // 2. more-cmd-console (консоль, все)
            String consoleCmdFlag = region.getFlag(WGCommandsAddon.MORE_CMD_CONSOLE_FLAG);
            if (consoleCmdFlag != null && !consoleCmdFlag.isEmpty()) {
                List<CommandWithCooldown> commands = parseCommands(consoleCmdFlag);
                for (CommandWithCooldown cmd : commands) {
                    if (checkCooldown(player, regionName, cmd.command, cmd.cooldownSeconds)) {
                        String parsed = replacePlaceholders(player, cmd.command, regionName);
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
                        plugin.getLogger().info("[more-cmd-console] " + player.getName() + " -> " + parsed + " (кулдаун " + cmd.cooldownSeconds + "с)");
                    }
                }
            }

            // 3. more-perm-cmd-player (игрок, только обычные)
            String permPlayerCmdFlag = region.getFlag(WGCommandsAddon.MORE_PERM_CMD_PLAYER_FLAG);
            if (permPlayerCmdFlag != null && !permPlayerCmdFlag.isEmpty() && !isAdmin) {
                List<CommandWithCooldown> commands = parseCommands(permPlayerCmdFlag);
                for (CommandWithCooldown cmd : commands) {
                    if (checkCooldown(player, regionName, cmd.command, cmd.cooldownSeconds)) {
                        String parsed = replacePlaceholders(player, cmd.command, regionName);
                        player.performCommand(parsed);
                        plugin.getLogger().info("[more-perm-cmd-player] " + player.getName() + " -> " + parsed + " (кулдаун " + cmd.cooldownSeconds + "с)");
                    }
                }
            }

            // 4. more-perm-cmd-console (консоль, только обычные)
            String permConsoleCmdFlag = region.getFlag(WGCommandsAddon.MORE_PERM_CMD_CONSOLE_FLAG);
            if (permConsoleCmdFlag != null && !permConsoleCmdFlag.isEmpty() && !isAdmin) {
                List<CommandWithCooldown> commands = parseCommands(permConsoleCmdFlag);
                for (CommandWithCooldown cmd : commands) {
                    if (checkCooldown(player, regionName, cmd.command, cmd.cooldownSeconds)) {
                        String parsed = replacePlaceholders(player, cmd.command, regionName);
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
                        plugin.getLogger().info("[more-perm-cmd-console] " + player.getName() + " -> " + parsed + " (кулдаун " + cmd.cooldownSeconds + "с)");
                    }
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

            boolean isAdmin = isAdmin(player);

            // 1. more-cmd-player (игрок, все)
            String playerCmdFlag = region.getFlag(WGCommandsAddon.MORE_CMD_PLAYER_FLAG);
            if (playerCmdFlag != null && !playerCmdFlag.isEmpty()) {
                List<CommandWithCooldown> commands = parseCommands(playerCmdFlag);
                for (CommandWithCooldown cmd : commands) {
                    if (checkCooldown(player, regionName, cmd.command, cmd.cooldownSeconds)) {
                        String parsed = replacePlaceholders(player, cmd.command, regionName);
                        player.performCommand(parsed);
                        plugin.getLogger().info("[more-cmd-player] " + player.getName() + " вышел -> " + parsed);
                    }
                }
            }

            // 2. more-cmd-console (консоль, все)
            String consoleCmdFlag = region.getFlag(WGCommandsAddon.MORE_CMD_CONSOLE_FLAG);
            if (consoleCmdFlag != null && !consoleCmdFlag.isEmpty()) {
                List<CommandWithCooldown> commands = parseCommands(consoleCmdFlag);
                for (CommandWithCooldown cmd : commands) {
                    if (checkCooldown(player, regionName, cmd.command, cmd.cooldownSeconds)) {
                        String parsed = replacePlaceholders(player, cmd.command, regionName);
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
                        plugin.getLogger().info("[more-cmd-console] " + player.getName() + " вышел -> " + parsed);
                    }
                }
            }

            // 3. more-perm-cmd-player (игрок, только обычные)
            String permPlayerCmdFlag = region.getFlag(WGCommandsAddon.MORE_PERM_CMD_PLAYER_FLAG);
            if (permPlayerCmdFlag != null && !permPlayerCmdFlag.isEmpty() && !isAdmin) {
                List<CommandWithCooldown> commands = parseCommands(permPlayerCmdFlag);
                for (CommandWithCooldown cmd : commands) {
                    if (checkCooldown(player, regionName, cmd.command, cmd.cooldownSeconds)) {
                        String parsed = replacePlaceholders(player, cmd.command, regionName);
                        player.performCommand(parsed);
                        plugin.getLogger().info("[more-perm-cmd-player] " + player.getName() + " вышел -> " + parsed);
                    }
                }
            }

            // 4. more-perm-cmd-console (консоль, только обычные)
            String permConsoleCmdFlag = region.getFlag(WGCommandsAddon.MORE_PERM_CMD_CONSOLE_FLAG);
            if (permConsoleCmdFlag != null && !permConsoleCmdFlag.isEmpty() && !isAdmin) {
                List<CommandWithCooldown> commands = parseCommands(permConsoleCmdFlag);
                for (CommandWithCooldown cmd : commands) {
                    if (checkCooldown(player, regionName, cmd.command, cmd.cooldownSeconds)) {
                        String parsed = replacePlaceholders(player, cmd.command, regionName);
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
                        plugin.getLogger().info("[more-perm-cmd-console] " + player.getName() + " вышел -> " + parsed);
                    }
                }
            }

        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка при выполнении команд выхода: " + e.getMessage());
        }
    }

    private String replacePlaceholders(Player player, String command, String regionName) {
        String result = command;
        result = result.replace("{player}", player.getName());
        result = result.replace("{region}", regionName);
        
        if (placeholderAPIEnabled) {
            try {
                result = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, result);
            } catch (Exception e) {
                plugin.getLogger().warning("Ошибка PlaceholderAPI: " + e.getMessage());
            }
        }
        
        return result;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        playerRegionStates.remove(player);
        lastCheckTime.remove(player);
        // Очищаем кулдауны игрока
        cooldownCache.keySet().removeIf(key -> key.startsWith(player.getUniqueId() + ":"));
    }

    /**
     * Вспомогательный класс для хранения команды с кулдауном
     */
    private static class CommandWithCooldown {
        final String command;
        final int cooldownSeconds;

        CommandWithCooldown(String command, int cooldownSeconds) {
            this.command = command;
            this.cooldownSeconds = cooldownSeconds;
        }
    }
}
