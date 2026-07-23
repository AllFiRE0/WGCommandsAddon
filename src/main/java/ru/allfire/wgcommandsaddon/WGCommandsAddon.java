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
import ru.allfire.wgcommandsaddon.listeners.PlayerListener;

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
            getLogger().info("  - one-command-asconsole");
            getLogger().info("  - one-command-asplayer");
            getLogger().info("  - one-perm-command-asconsole");
            getLogger().info("  - one-perm-command-asplayer");
        } catch (FlagConflictException e) {
            getLogger().warning("Ошибка регистрации флагов!");
            e.printStackTrace();
        }
    }

    @Override
    public void onEnable() {
        // Регистрируем слушатель
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        
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
}
