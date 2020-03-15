package com.steffbeard.totalwar.nations.util;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;

import com.steffbeard.totalwar.nations.Messages;
import com.steffbeard.totalwar.nations.NationsUniverse;
import com.steffbeard.totalwar.nations.exceptions.NationsException;
import com.steffbeard.totalwar.nations.exceptions.NotRegisteredException;
import com.steffbeard.totalwar.nations.objects.resident.Resident;

public class ResidentUtil {
	
	/** 
	 * Return a list of Residents that can be seen (not vanished) by the viewer.
	 * 
	 * @param viewer - Player who is looking.
	 * @param residentList - List of Residents which could be viewed.
	 * @return - List of residents that can actually be seen.
	 */
	public static List<Resident> getOnlineResidentsViewable(Player viewer, ResidentList residentList) {
		
		List<Resident> onlineResidents = new ArrayList<>();
		for (Player player : BukkitTools.getOnlinePlayers()) {
			if (player != null) {
				/*
				 * Loop town/nation resident list
				 */
				for (Resident resident : residentList.getResidents()) {
					if (resident.getName().equalsIgnoreCase(player.getName()))
						if ((viewer == null) || (viewer.canSee(BukkitTools.getPlayerExact(resident.getName())))) {
							onlineResidents.add(resident);
						}
				}
			}
		}
		
		return onlineResidents;
	}
	
	/**
	 * Transforms a String[] of names to a list of Residents.
	 * Uses the BukkitTools.matchPlayer() rather than BukkitTools.getPlayerExact();
	 * Used for:
	 *  - Inviting
	 *  - Kicking
	 * 
	 * @param sender - CommandSender.
	 * @param names - Names to be converted.
	 * @return - List of residents to be used later.
	 */
	public static List<Resident> getValidatedResidents(Object sender, String[] names) {
		NationsUniverse townyUniverse = NationsUniverse.getInstance();
		List<Resident> residents = new ArrayList<>();
		for (String name : names) {
			List<Player> matches = BukkitTools.matchPlayer(name);
			if (matches.size() > 1) {
				StringBuilder line = new StringBuilder("Multiple players selected: ");
				for (Player p : matches)
					line.append(", ").append(p.getName());
				Messages.sendErrorMsg(sender, line.toString());
			} else if (matches.size() == 1) {
				// Match found online
				try {
					Resident target = townyUniverse.getDataSource().getResident(matches.get(0).getName());
					residents.add(target);
				} catch (NationsException x) {
					Messages.sendErrorMsg(sender, x.getMessage());
				}
			} else {
				// No online matches so test for offline.
				Resident target;
				try {
					target = townyUniverse.getDataSource().getResident(name);
					residents.add(target);
				} catch (NotRegisteredException x) {
					Messages.sendErrorMsg(sender, x.getMessage());
				}
			}
		}
		return residents;
	}
}