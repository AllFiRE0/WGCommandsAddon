package ru.allfire.wgcommandsaddon;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.StringFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import ru.allfire.wgcommandsaddon.events.RegionEnterEvent;
import ru.allfire.wgcommandsaddon.events.RegionLeaveEvent;
import ru.allfire.wgcommandsaddon.handlers.RegionSessionHandler;

public final class WGCommandsAddon extends JavaPlugin implements Listener {

    public static StringFlag ONE_COMMAND_ASCONSOLE_FLAG;
    public static StringFlag ONE_COMMAND_ASPLAYER_FLAG;
    public static StringFlag ONE_PERM_COMMAND_ASCONSOLE_FLAG;
    public static StringFlag ONE_PERM_COMMAND_ASPLAYER_FLAG;
    
    private static WGCommandsAddon instance;

    @Override
    public void onLoad() {
        instance = this;
        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
        
        try {
            ONE_COMMAND_ASCONSOLE_FLAG = new StringFlag("one-command-asconsole");
            registry.register(ONE_COMMAND_ASCONSOLE_FLAG);

            ONE_COMMAND_ASPLAYER_FLAG = new StringFlag("one-command-asplayer");
            registry.register(ONE_COMMAND_ASPLAYER_FLAG);

            ONE_PERM_COMMAND_ASCONSOLE_FLAG = new StringFlag("one-perm-command-asconsole");
            registry.register(ONE_PERM_COMMAND_ASCONSOLE_FLAG);

            ONE_PERM_COMMAND_ASPLAYER_FLAG = new StringFlag("one-perm-command-asplayer");
            registry.register(ONE_PERM_COMMAND_ASPLAYER_FLAG);
            
            getLogger().info("Все флаги успешно зарегистрированы!");
        } catch (FlagConflictException e) {
            getLogger().warning("Ошибка регистрации флагов!");
            e.printStackTrace();
        }
    }

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        
        try {
            com.sk89q.worldguard.session.SessionManager sessionManager = 
                WorldGuard.getInstance().getPlatform().getSessionManager();
            sessionManager.registerHandler(RegionSessionHandler.FACTORY, null);
            getLogger().info("Обработчик сессий WorldGuard зарегистрирован!");
        } catch (Exception e) {
            getLogger().warning("Ошибка регистрации обработчика сессий!");
            e.printStackTrace();
        }
        
        getLogger().info("========================================");
        getLogger().info("ДополнительныеКомандыWG v" + getDescription().getVersion());
        getLogger().info("Автор: AllF1RE");
        getLogger().info("Флаги:");
        getLogger().info("  - one-command-asconsole (консоль, без прав)");
        getLogger().info("  - one-command-asplayer (игрок, без прав)");
        getLogger().info("  - one-perm-command-asconsole (консоль, с правом wgca.onecommand)");
        getLogger().info("  - one-perm-command-asplayer (игрок, с правом wgca.onecommand)");
        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("ДополнительныеКомандыWG выключен.");
    }

    public static WGCommandsAddon getInstance() {
        return instance;
    }

    @EventHandler
    public void onRegionEnter(RegionEnterEvent event) {
        handleCommands(event.getPlayer(), event.getRegion(), true);
    }

    @EventHandler
    public void onRegionLeave(RegionLeaveEvent event) {
        handleCommands(event.getPlayer(), event.getRegion(), false);
    }

    private void handleCommands(Player player, ProtectedRegion region, boolean isEnter) {
        String action = isEnter ? "вход" : "выход";
        String regionName = region.getId();

        String consoleCommand = region.getFlag(ONE_COMMAND_ASCONSOLE_FLAG);
        if (consoleCommand != null && !consoleCommand.isEmpty()) {
            executeCommand(player, consoleCommand, false);
            getLogger().info("[one-command-asconsole] " + player.getName() + " при " + action + " в " + regionName);
        }

        String playerCommand = region.getFlag(ONE_COMMAND_ASPLAYER_FLAG);
        if (playerCommand != null && !playerCommand.isEmpty()) {
            executeCommandAsPlayer(player, playerCommand);
            getLogger().info("[one-command-asplayer] " + player.getName() + " при " + action + " в " + regionName);
        }

        String permConsoleCommand = region.getFlag(ONE_PERM_COMMAND_ASCONSOLE_FLAG);
        if (permConsoleCommand != null && !permConsoleCommand.isEmpty()) {
            if (player.hasPermission("wgca.onecommand")) {
                executeCommand(player, permConsoleCommand, true);
                getLogger().info("[one-perm-command-asconsole] " + player.getName() + " при " + action + " в " + regionName);
            }
        }

        String permPlayerCommand = region.getFlag(ONE_PERM_COMMAND_ASPLAYER_FLAG);
        if (permPlayerCommand != null && !permPlayerCommand.isEmpty()) {
            if (player.hasPermission("wgca.onecommand")) {
                executeCommandAsPlayer(player, permPlayerCommand);
                getLogger().info("[one-perm-command-asplayer] " + player.getName() + " при " + action + " в " + regionName);
            }
        }
    }

    private void executeCommand(Player player, String command, boolean checkPermission) {
        String parsedCommand = command.replace("{player}", player.getName());
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsedCommand);
    }

    private void executeCommandAsPlayer(Player player, String command) {
        String parsedCommand = command.replace("{player}", player.getName());
        player.performCommand(parsedCommand);
    }
}
