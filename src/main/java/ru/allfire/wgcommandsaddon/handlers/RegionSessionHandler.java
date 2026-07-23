package ru.allfire.wgcommandsaddon.handlers;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.session.MoveType;
import com.sk89q.worldguard.session.Session;
import com.sk89q.worldguard.session.handler.Handler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import ru.allfire.wgcommandsaddon.WGCommandsAddon;
import ru.allfire.wgcommandsaddon.events.RegionEnterEvent;
import ru.allfire.wgcommandsaddon.events.RegionLeaveEvent;

import java.util.HashSet;
import java.util.Set;

public class RegionSessionHandler extends Handler {

    public static final Factory FACTORY = new Factory();
    private Set<ProtectedRegion> lastRegions = new HashSet<>();

    public static class Factory extends Handler.Factory<RegionSessionHandler> {
        @Override
        public RegionSessionHandler create(Session session) {
            return new RegionSessionHandler(session);
        }
    }

    public RegionSessionHandler(Session session) {
        super(session);
    }

    @Override
    public void initialize(Session session, Player player) {
        try {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            
            if (container != null && player != null && player.isOnline()) {
                ApplicableRegionSet set = container.createQuery()
                    .getApplicableRegions(player.getLocation());
                this.lastRegions = set.getRegions();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onMove(Player player, com.sk89q.worldedit.util.Location from, 
                       com.sk89q.worldedit.util.Location to, MoveType moveType) {
        if (player == null || !player.isOnline()) return;

        try {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            
            if (container == null) return;
            
            // Конвертируем WorldEdit Location в Bukkit Location
            Location bukkitLocation = new Location(
                Bukkit.getWorld(to.getWorld().getName()),
                to.getX(), to.getY(), to.getZ()
            );
            
            ApplicableRegionSet currentSet = container.createQuery()
                .getApplicableRegions(bukkitLocation);
            Set<ProtectedRegion> currentRegions = currentSet.getRegions();

            // Проверяем вход в новые регионы
            for (ProtectedRegion region : currentRegions) {
                if (!lastRegions.contains(region)) {
                    Bukkit.getPluginManager().callEvent(new RegionEnterEvent(player, region));
                }
            }

            // Проверяем выход из старых регионов
            for (ProtectedRegion region : lastRegions) {
                if (!currentRegions.contains(region)) {
                    Bukkit.getPluginManager().callEvent(new RegionLeaveEvent(player, region));
                }
            }

            this.lastRegions = currentRegions;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
