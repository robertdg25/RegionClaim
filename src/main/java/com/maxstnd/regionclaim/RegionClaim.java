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

import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.WorldConfiguration;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class RegionClaim extends JavaPlugin
{
	private Logger m_log;
	private PluginManager m_pm;
	private WorldGuardPlugin m_wg;
    
    public void onEnable() {
    	m_log = Logger.getLogger("Minecraft");
    	m_pm = this.getServer().getPluginManager();
		
    	m_log.info("RegionClaim enabled.");
    }
    
    public void onDisable() {
    	m_log.info("RegionClaim disabled.");
    }
    
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
    	if(m_wg == null) {
	    	try {
				m_wg = getWorldGuard();
			} catch(Exception ex) {
				sender.sendMessage(ChatColor.RED + ex.getMessage());
	    		ex.printStackTrace();
	    	}
    	}
		
    	try {
	    	if(cmd.getName().equalsIgnoreCase("regionclaim")) {
	    		Player player;
	    		player = m_wg.checkPlayer(sender);
	    		LocalPlayer localPlayer = m_wg.wrapPlayer(player);
	    		World world = player.getWorld();
	    		String id = args[0];
	    		
	    		if(!ProtectedRegion.isValidId(id)) {
	    			player.sendMessage(ChatColor.RED + "Invalid region ID specified!");
	    			return false;
	    		}
	    		
	    		if (id.equalsIgnoreCase("__global__")) {
	    			player.sendMessage(ChatColor.RED + "A region cannot be named __global__");
	    			return false;
	            }
	    		
	    		RegionManager mgr = m_wg.getGlobalRegionManager().get(world);
	            ProtectedRegion region = mgr.getRegion(id);

	            if (region == null) {
	            	player.sendMessage(ChatColor.RED + "Could not find a region by that ID.");
	            	return false;
	            }
	            
	            WorldConfiguration wcfg = m_wg.getGlobalConfiguration().get(player.getWorld());
	            
	            // Check whether the player has created too many regions 
	            if (wcfg.maxRegionCountPerPlayer >= 0
	                    && mgr.getRegionCountOfPlayer(localPlayer) >= wcfg.maxRegionCountPerPlayer) {
	            	player.sendMessage(ChatColor.RED + "You own too many regions, delete one first to claim a new one.");
	            	return false;
	            }

	            // Check whether the region has an owner
	            if (region.getOwners().getPlayers().size() > 0) {
	            	player.sendMessage(ChatColor.RED + "This region already has an owner - you can't claim it.");
	            	return false;
	            }
	            
	            ApplicableRegionSet regions = mgr.getApplicableRegions(region);
	            
	            // Check if this region overlaps any other region
	            if (regions.size() == 0) {
	                if (wcfg.claimOnlyInsideExistingRegions) {
	                	player.sendMessage(ChatColor.RED + "You may only claim regions inside " +
	                    		"existing regions that you or your group own.");
	                	return false;
	                }
	            }

	            if (region.volume() > wcfg.maxClaimVolume) {
	                player.sendMessage(ChatColor.RED + "This region is to large to claim.");
	                player.sendMessage(ChatColor.RED +
	                        "Max. volume: " + wcfg.maxClaimVolume + ", your volume: " + region.volume());
	                return false;
	            }

	            region.getOwners().addPlayer(player.getName());
	            mgr.addRegion(region);
	            
	            try {
	                mgr.save();
	                sender.sendMessage(ChatColor.YELLOW + "Congratulations, you now own the region " + id + ".");
	            } catch (IOException e) {
	                throw new Exception("Failed to write regions file: "
	                        + e.getMessage());
	            }
	    		
	    		return true;
	    	}
	    } catch (Exception ex) {
	    	ex.printStackTrace();
			return false;
		}
    	
    	// Command not found
    	return false;
    }
    
    private WorldGuardPlugin getWorldGuard() throws Exception {
    	Plugin worldGuard = m_pm.getPlugin("WorldGuard");
    	
        if (worldGuard == null) {
            throw new Exception("WorldGuard does not appear to be installed.");
        }
        
        if (worldGuard instanceof WorldGuardPlugin) {
            return (WorldGuardPlugin) worldGuard;
        } else {
            throw new Exception("WorldGuard detection failed (report error).");
        }
    }
}