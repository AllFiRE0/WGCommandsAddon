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
            getLogger().info("  - more-cmd-player");
            getLogger().info("  - more-cmd-console");
            getLogger().info("  - more-perm-cmd-player");
            getLogger().info("  - more-perm-cmd-console");
        } catch (FlagConflictException e) {
            getLogger().warning("Ошибка регистрации флагов!");
            e.printStackTrace();
        }
    }

    @Override
    public void onEnable() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            placeholderAPIEnabled = true;
            getLogger().info("PlaceholderAPI найден! Внешние заполнители поддерживаются.");
        } else {
            getLogger().warning("PlaceholderAPI НЕ найден! Внешние заполнители не будут работать.");
        }
        
        getServer().getPluginManager().registerEvents(new PlayerListener(this, placeholderAPIEnabled), this);
        
        getLogger().info("========================================");
        getLogger().info("WGCommandsAddon v" + getDescription().getVersion());
        getLogger().info("Автор: AllF1RE");
        getLogger().info("Флаги:");
        getLogger().info("  - more-cmd-player (игрок, все)");
        getLogger().info("  - more-cmd-console (консоль, все)");
        getLogger().info("  - more-perm-cmd-player (игрок, только обычные игроки)");
        getLogger().info("  - more-perm-cmd-console (консоль, только обычные игроки)");
        getLogger().info("Формат: 'задержка_сек команда||задержка_сек команда'");
        getLogger().info("Пример: '10 give {player} bone 1||30 give {player} diamond 1'");
        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("WGCommandsAddon выключен.");
    }

    public static WGCommandsAddon getInstance() {
        return instance;
    }
    
    public boolean isPlaceholderAPIEnabled() {
        return placeholderAPIEnabled;
    }
}
