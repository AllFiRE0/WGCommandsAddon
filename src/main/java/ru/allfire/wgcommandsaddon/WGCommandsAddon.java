package ru.allfire.wgcommandsaddon;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.StringFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import ru.allfire.wgcommandsaddon.listeners.PlayerListener;

public final class WGCommandsAddon extends JavaPlugin {

    public static StringFlag MORE_CMD_PLAYER_FLAG;
    public static StringFlag MORE_CMD_CONSOLE_FLAG;
    public static StringFlag MORE_PERM_CMD_PLAYER_FLAG;
    public static StringFlag MORE_PERM_CMD_CONSOLE_FLAG;
    
    private static WGCommandsAddon instance;
    private boolean placeholderAPIEnabled = false;
    private boolean disableLogs = false;

    @Override
    public void onLoad() {
        instance = this;
        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
        
        try {
            MORE_CMD_PLAYER_FLAG = new StringFlag("more-cmd-player");
            registry.register(MORE_CMD_PLAYER_FLAG);

            MORE_CMD_CONSOLE_FLAG = new StringFlag("more-cmd-console");
            registry.register(MORE_CMD_CONSOLE_FLAG);

            MORE_PERM_CMD_PLAYER_FLAG = new StringFlag("more-perm-cmd-player");
            registry.register(MORE_PERM_CMD_PLAYER_FLAG);

            MORE_PERM_CMD_CONSOLE_FLAG = new StringFlag("more-perm-cmd-console");
            registry.register(MORE_PERM_CMD_CONSOLE_FLAG);
            
            getLogger().info("Все флаги успешно зарегистрированы!");
        } catch (FlagConflictException e) {
            getLogger().warning("Ошибка регистрации флагов!");
            e.printStackTrace();
        }
    }

    @Override
    public void onEnable() {
        // Загружаем конфиг
        saveDefaultConfig();
        reloadConfig();
        disableLogs = getConfig().getBoolean("disable-logs", false);
        
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            placeholderAPIEnabled = true;
            if (!disableLogs) getLogger().info("PlaceholderAPI найден!");
        } else {
            if (!disableLogs) getLogger().warning("PlaceholderAPI НЕ найден!");
        }
        
        getServer().getPluginManager().registerEvents(new PlayerListener(this, placeholderAPIEnabled, disableLogs), this);
        
        if (!disableLogs) {
            getLogger().info("========================================");
            getLogger().info("WGCommandsAddon v" + getDescription().getVersion());
            getLogger().info("Автор: AllF1RE");
            getLogger().info("Флаги:");
            getLogger().info("  - more-cmd-player (игрок, все)");
            getLogger().info("  - more-cmd-console (консоль, все)");
            getLogger().info("  - more-perm-cmd-player (игрок, право wgca.use)");
            getLogger().info("  - more-perm-cmd-console (консоль, право wgca.use)");
            getLogger().info("Формат: 'задержка_сек команда||задержка_сек команда'");
            getLogger().info("Пример: '10 give {player} bone 1||30 give {player} diamond 1'");
            getLogger().info("Логи в консоль: " + (disableLogs ? "ВЫКЛЮЧЕНЫ" : "ВКЛЮЧЕНЫ"));
            getLogger().info("========================================");
        }
    }

    @Override
    public void onDisable() {
        if (!disableLogs) getLogger().info("WGCommandsAddon выключен.");
    }

    public static WGCommandsAddon getInstance() {
        return instance;
    }
    
    public boolean isPlaceholderAPIEnabled() {
        return placeholderAPIEnabled;
    }
    
    public boolean isLogsDisabled() {
        return disableLogs;
    }
}
