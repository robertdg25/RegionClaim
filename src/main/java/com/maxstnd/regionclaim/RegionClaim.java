package com.maxstnd.regionclaim;

import java.io.IOException;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.WorldConfiguration;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class RegionClaim extends JavaPlugin {
    private Logger m_log;
    private PluginManager m_pm;
    private WorldGuardPlugin m_wg;

    public void onEnable() {
        // Get the logger and plugin manager
        m_log = Logger.getLogger("Minecraft");
        m_pm = this.getServer().getPluginManager();

        m_log.info("RegionClaim enabled.");
    }

    public void onDisable() {
        m_log.info("RegionClaim disabled.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {

        if (cmd.getName().equalsIgnoreCase("regionclaim")) {
            try {
                // Ensure we have WorldGuard else abort softly
                if (!getWorldGuard(sender)) {
                    return true;
                }
                
                // Get player/world information
                Player player = m_wg.checkPlayer(sender);
                LocalPlayer localPlayer = m_wg.wrapPlayer(player);
                World world = player.getWorld();
                
                // Check number of arguments passed
                if(args.length != 1) {
                    player.sendMessage(ChatColor.YELLOW + "/regionclaim <id>");
                    return true;
                }
                String id = args[0];                
    
                // Check that the region id specified is not __global__
                if (id.equalsIgnoreCase("__global__")) {
                    player.sendMessage(ChatColor.RED + "A region cannot be named __global__");
                    return true;
                }
    
                // Get the region manager and region
                RegionManager mgr = m_wg.getGlobalRegionManager().get(world);
                ProtectedRegion region = mgr.getRegion(id);
    
                // Check if the region id supplied is a valid one
                if (region == null) {
                    player.sendMessage(ChatColor.RED + "Could not find a region by that ID.");
                    return true;
                }
    
                // Get the world configuration
                WorldConfiguration wcfg = m_wg.getGlobalConfiguration().get(player.getWorld());
    
                // Check whether the player has created too many regions
                    // Note: Region count of 0 is "unlimited"
                if (wcfg.maxRegionCountPerPlayer >= 1 && mgr.getRegionCountOfPlayer(localPlayer) >= wcfg.maxRegionCountPerPlayer) {
                    player.sendMessage(ChatColor.RED + "You may only own one plot!");
                    return true;
                }
    
                // Check whether the region has an owner
                if (region.getOwners().getPlayers().size() > 0) {
                    player.sendMessage(ChatColor.RED + "This region already has an owner - you can't claim it.");
                    return true;
                }
    
                // Get the applicable regions for the region selection
                ApplicableRegionSet regions = mgr.getApplicableRegions(region);
    
                // Check if this region overlaps any other region
                if (regions.size() == 0) {
                    if (wcfg.claimOnlyInsideExistingRegions) {
                        player.sendMessage(ChatColor.RED + "You may only claim regions inside existing regions that you or your group own.");
                        return true;
                    }
                }
    
                // Check if the region size is too large to claim
                if (region.volume() > wcfg.maxClaimVolume) {
                    player.sendMessage(ChatColor.RED + "This region is to large to claim.");
                    player.sendMessage(ChatColor.RED + "Max. volume: " + wcfg.maxClaimVolume + ", your volume: " + region.volume());
                    return true;
                }
    
                // Add the player as the owner
                region.getOwners().addPlayer(player.getName());
                mgr.addRegion(region);
    
                // Attempt to save
                try {
                    mgr.save();
                    sender.sendMessage(ChatColor.YELLOW + "Congratulations, you now own the region " + id + ".");
                } catch (IOException e) {
                    throw new Exception("Failed to write regions file: " + e.getMessage());
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            return true;
        }

        // Command not found
        return false;
    }

    /*
     * Desc: Attempt to get the WorldGuard plugin
     */
    private boolean getWorldGuard(CommandSender sender) {
        // Only load WorldGuard if it isn't already loaded
        if (m_wg == null) {
            try {
                Plugin worldGuard = m_pm.getPlugin("WorldGuard");

                // Check of WorldGuard is running, softly abort
                if (worldGuard == null) {
                    sender.sendMessage(ChatColor.RED + "WorldGuard does not appear to be installed.");
                }

                // Check if returned object is WorldGuard
                if (worldGuard instanceof WorldGuardPlugin) {
                    m_wg = (WorldGuardPlugin) worldGuard;
                } else {
                    throw new Exception("WorldGuard detection failed (report error).");
                }
            } catch (Exception ex) {
                m_wg = null; // Reset WorldGuard
                ex.printStackTrace();
                return false;
            }
        }
        return true;
    }
}