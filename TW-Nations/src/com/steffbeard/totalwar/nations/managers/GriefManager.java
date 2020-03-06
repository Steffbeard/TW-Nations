package com.steffbeard.totalwar.nations.managers;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.FilenameUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.material.Attachable;
import org.bukkit.material.PistonExtensionMaterial;

import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyUniverse;

import com.steffbeard.totalwar.nations.Main;
import com.steffbeard.totalwar.nations.listeners.GriefListener;
import com.steffbeard.totalwar.nations.tasks.DelayedRegenTask;
import com.steffbeard.totalwar.nations.utils.BlockSerialization;
import com.steffbeard.totalwar.nations.utils.BlockUtils;
import com.steffbeard.totalwar.nations.utils.SBlock;

//Make into a class that stores the blocks in data files per town among other uses
public class GriefManager {
	
	private Main plugin;
	private File townDataFolder;
	private File townData;
	private FileConfiguration blocks;
	
	public GriefManager(Main plugin){
		this.plugin = plugin;
	}
	
	public ConcurrentHashMap<Town, Set<SBlock>> loadData(){
		Set<SBlock> blocksBroken = new HashSet<SBlock>();
		ConcurrentHashMap<Town, Set<SBlock>> data = new ConcurrentHashMap<Town, Set<SBlock>>();
		String listSerial = "";
		int size;		
		townDataFolder = new File(plugin.getDataFolder().toString() + "/towndata");
		if (!townDataFolder.exists()) {
			Bukkit.getServer().getLogger().info("Directory Doesn't Exist, Creating...");
			townDataFolder.mkdir();
		}
		File[] listOfFiles = townDataFolder.listFiles();
	    for (int i = 0; i < listOfFiles.length; i++) {
	    	if (listOfFiles[i].isFile()) {
	    		String town = listOfFiles[i].getName();
	    		townData = new File(townDataFolder, (town));
	    		if (townData.exists()) {
	    			blocks = YamlConfiguration.loadConfiguration(townData);
	    			if(blocks!=null){
	    				if(!(blocks.getString("blocks") == "") && (blocks.getString("blocks") != null)){
	    					listSerial = blocks.getString("blocks");
	    					size = blocks.getInt("size");
	    					try {
	    						if(!listSerial.equals("") && !listSerial.equals(null)){
	    							blocksBroken = BlockSerialization.fromBase64(listSerial, size);
	    							Town towny;
									try {
										towny = TownyUniverse.getDataSource().getTown(FilenameUtils.removeExtension(town));
										if(blocksBroken!=null && towny!=null){
		    								data.put(towny, blocksBroken);
		    							}
									} catch (NotRegisteredException e) {
										Bukkit.getServer().getLogger().info("Town is no longer a town");
										townData.delete();
									}
	    						}else{
	    							townData.delete();
	    						}			
	    					} catch (IOException e) {
	    						Bukkit.getServer().getLogger().info("Can't load BlocksFile");
	    						e.printStackTrace();
	    					}
	    				}		
	    			}
	    		}
	    	}
	    }	
		return data;
	}
	
	public void saveData(ConcurrentHashMap<Town, Set<SBlock>> Sblocks){
		int size;
		Set<SBlock> blocksBroken;
		for(Town town : Sblocks.keySet()){
			townDataFolder = new File(plugin.getDataFolder().toString() + "/towndata");
			townData = new File(townDataFolder, (town.getName().toLowerCase() + ".yml"));
			blocks = YamlConfiguration.loadConfiguration(townData);
			if(blocks!=null){
				if (!townDataFolder.exists()) {
					Bukkit.getServer().getLogger().info("Directory Doesn't Exist, Creating...");
					townDataFolder.mkdir();
				}
				if (!townData.exists()) {
					try {
						townData.createNewFile();
					} catch (IOException e) {
						plugin.getServer().getLogger().info("An Error has occured. Please see the stacktrace below.");
						e.printStackTrace();
					}
				}else{
					townData.delete();
					try {
						townData.createNewFile();
					} catch (IOException e) {
						plugin.getServer().getLogger().info("An Error has occured. Please see the stacktrace below.");
						e.printStackTrace();
					}
				}
				blocksBroken = Sblocks.get(town);
				if(blocksBroken!=null){
					if(blocksBroken.isEmpty()){
						size = 0;
					}else{
						size = blocksBroken.size();
					}
					String listSerial = BlockSerialization.toBase64(blocksBroken);
					if(listSerial!=null && size!=0){
						blocks.set("blocks", listSerial);
						blocks.set("size", size);
						try {
							blocks.save(townData);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}				
				}
			}
		}
	}
	
	@SuppressWarnings({ "deprecation" })
	public void rollbackBlocks(Town town){
		Set<SBlock> sBlocks = GriefListener.getGriefedBlocks().get(town);
		townDataFolder = new File(plugin.getDataFolder().toString() + "/towndata");
		townData = new File(townDataFolder, (town.getName().toLowerCase() + ".yml"));
		int delay = 1;
    	for(final SBlock sb : sBlocks){
    		Location l = new Location(Bukkit.getServer().getWorld(sb.world),sb.x,sb.y,sb.z);
    		Block bl = l.getBlock();
    		Material mat = Material.valueOf(sb.mat);
    		bl.setTypeIdAndData(mat.getId(), sb.data, true);
    		BlockState blockState = bl.getState();
    		/*if(BlockUtils.isOtherAttachable(mat) || mat.equals(Material.CACTUS) || mat.equals(Material.SUGAR_CANE_BLOCK) || blockState.getData() instanceof PistonExtensionMaterial || blockState instanceof Attachable){
    			new DelayedRegenTask(sb).runTaskLater(plugin, delay+20);*/

    		if(BlockUtils.isOtherAttachable(mat) || mat.equals(Material.CACTUS) || mat.equals(Material.SUGAR_CANE_BLOCK) || blockState.getData() instanceof PistonExtensionMaterial || blockState instanceof Attachable){
    			plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
      			  public void run() {
      				  //Bukkit.getServer().broadcastMessage("Area 1");
      				new DelayedRegenTask(sb).run();
      			  }
      			}, delay + 20);
    		}else{
    			plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
        			  public void run() {
          				 // Bukkit.getServer().broadcastMessage("Area 2");
        				new DelayedRegenTask(sb).run();
        			  }
        			}, delay);
    		}
    		delay++;
    	}
    	GriefListener.removeTownGriefedBlocks(town);
    	if(townData.exists()){
    		townData.delete();
    	}	
	}	
}