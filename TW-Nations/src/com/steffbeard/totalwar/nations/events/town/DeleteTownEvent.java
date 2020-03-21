package com.steffbeard.totalwar.nations.events.town;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;


public class DeleteTownEvent extends Event  {

    private static final HandlerList handlers = new HandlerList();
    
    private String townName;

    @Override
    public HandlerList getHandlers() {
    	
        return handlers;
    }
    
    public static HandlerList getHandlerList() {

		return handlers;
	}

    public DeleteTownEvent(String town) {
        super(!Bukkit.getServer().isPrimaryThread());
        this.townName = town;
    }

    /**
     *
     * @return the deleted town name.
     */
    public String getTownName() {
        return townName;
    }
    
}