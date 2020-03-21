package com.steffbeard.totalwar.nations.war.siege;

import org.bukkit.entity.Player;

import com.steffbeard.totalwar.nations.NationsUniverse;
import com.steffbeard.totalwar.nations.config.Settings;
import com.steffbeard.totalwar.nations.exceptions.NotRegisteredException;
import com.steffbeard.totalwar.nations.objects.NationsObject;
import com.steffbeard.totalwar.nations.objects.resident.Resident;
import com.steffbeard.totalwar.nations.objects.town.Town;
import com.steffbeard.totalwar.nations.permissions.PermissionNodes;
import com.steffbeard.totalwar.nations.war.siege.location.SiegeZone;

/**
 * This class intercepts 'player death' events coming from the towny entity monitor listener class.
 *
 * This class evaluates the death, and determines if the player is involved in any nearby sieges.
 * If so, their opponents gain siege points.
 * 
 * @author Goosius
 */
public class SiegeWarDeathController {

	/**
	 * Evaluates a PVP death event.
	 * 
	 * If both players are directly involved in a nearby siege, the killer's side gains siege points:
	 * 
	 * NOTE: 
	 * Allied nations or friendly towns can still be involved in sieges,
	 * (e.g. via resource support, scouting, spying, diversions, or attacking enemy combatants),
	 * but they cannot directly affect the siege points totals. 
	 *
	 * @param deadPlayer The player who died
	 * @param killerPlayer The player who did the killing
	 * @param deadResident The resident who died
	 * @param killerResident The resident who did the killing
	 *  
	 */
	public static void evaluateSiegePvPDeath(Player deadPlayer, Player killerPlayer, Resident deadResident, Resident killerResident)  {
		
		System.out.println("Now evaluating pvp death");
		try {
			if (!deadResident.hasTown())
				return;

			if (!killerResident.hasTown())
				return;

			Town deadResidentTown = deadResident.getTown();
			Town killerResidentTown = killerResident.getTown();

			//Residents of occupied towns cannot affect siege points.
			if (deadResidentTown.isOccupied() || killerResidentTown.isOccupied())
				return;

			SiegeZone siegeZone;

			if ((siegeZone = getSiegeZoneForAttackingSoldierKilledDefendingGuard(deadPlayer, killerPlayer, deadResidentTown, killerResidentTown)) != null) {
				awardSiegePvpPenaltyPoints(false, siegeZone.getAttackingNation(), deadResident, siegeZone);

			} else if ((siegeZone = getSiegeZoneForAttackingSoldierKilledDefendingSoldier(deadPlayer, killerPlayer, deadResidentTown, killerResidentTown)) != null) {
				awardSiegePvpPenaltyPoints(false, siegeZone.getAttackingNation(), deadResident, siegeZone);

			} else if ((siegeZone = getSiegeZoneForDefendingGuardKilledAttackingSoldier(deadPlayer, killerPlayer, deadResidentTown, killerResidentTown)) != null) {
				awardSiegePvpPenaltyPoints(true, siegeZone.getDefendingTown(), deadResident, siegeZone);

			} else if ((siegeZone = getSiegeZoneForDefendingSoldierKilledAttackingSoldier(deadPlayer, killerPlayer, deadResidentTown, killerResidentTown)) != null) {
				awardSiegePvpPenaltyPoints(true, siegeZone.getDefendingTown(), deadResident, siegeZone);
			}

		} catch (NotRegisteredException e) {
			e.printStackTrace();
			System.out.println("Error evaluating siege pvp death");
		}

	}

	private static SiegeZone getSiegeZoneForAttackingSoldierKilledDefendingGuard(
		Player deadPlayer, Player killerPlayer, Town deadResidentTown, Town killerResidentTown) throws NotRegisteredException
	{
		NationsUniverse universe = NationsUniverse.getInstance();
		if (killerResidentTown.hasNation()
			&& deadResidentTown.hasSiege()
			&& deadResidentTown.getSiege().getStatus() == SiegeStatus.IN_PROGRESS
			&& universe.getPermissionSource().testPermission(deadPlayer, PermissionNodes.TOWNY_TOWN_SIEGE_POINTS.getNode())
			&& universe.getPermissionSource().testPermission(killerPlayer, PermissionNodes.TOWNY_NATION_SIEGE_POINTS.getNode())) {

			for (SiegeZone siegeZone : deadResidentTown.getSiege().getSiegeZones().values()) {
				if ((killerResidentTown.getNation() == siegeZone.getAttackingNation() || killerResidentTown.getNation().hasMutualAlly(siegeZone.getAttackingNation()))  //Is the killer player attacking in this siege zone ?
					&& deadPlayer.getLocation().distance(siegeZone.getFlagLocation()) < Settings.getWarSiegeZoneDeathRadiusBlocks()) {
					
					return siegeZone;
				}
			}
		}
		return null;
	}

	private static SiegeZone getSiegeZoneForAttackingSoldierKilledDefendingSoldier(
		Player deadPlayer, Player killerPlayer, Town deadResidentTown, Town killerResidentTown) throws NotRegisteredException 
	{
		NationsUniverse universe = NationsUniverse.getInstance();
		if (killerResidentTown.hasNation()
			&& deadResidentTown.hasNation()
			&& universe.getPermissionSource().testPermission(deadPlayer, PermissionNodes.TOWNY_NATION_SIEGE_POINTS.getNode())
			&& universe.getPermissionSource().testPermission(killerPlayer, PermissionNodes.TOWNY_NATION_SIEGE_POINTS.getNode())) {

			for (SiegeZone siegeZone : universe.getDataSource().getSiegeZones()) {
				if (siegeZone.getSiege().getStatus() == SiegeStatus.IN_PROGRESS
					&& (killerResidentTown.getNation() == siegeZone.getAttackingNation() || killerResidentTown.getNation().hasMutualAlly(siegeZone.getAttackingNation())) //Is the killer player attacking in this siege zone ?
					&& (deadResidentTown.getNation() == siegeZone.getDefendingTown().getNation() || deadResidentTown.getNation().hasMutualAlly(siegeZone.getDefendingTown().getNation())) //Was the dead player defending in this siege zone ?
					&& deadPlayer.getLocation().distance(siegeZone.getFlagLocation()) < Settings.getWarSiegeZoneDeathRadiusBlocks()) {

					return siegeZone;
				}
			}
		}
		return null;
	}

	private static SiegeZone getSiegeZoneForDefendingGuardKilledAttackingSoldier(
		Player deadPlayer, Player killerPlayer, Town deadResidentTown, Town killerResidentTown) throws NotRegisteredException
	{
		NationsUniverse universe = NationsUniverse.getInstance();
		if (killerResidentTown.hasSiege()
			&& killerResidentTown.getSiege().getStatus() == SiegeStatus.IN_PROGRESS
			&& deadResidentTown.hasNation()
			&& universe.getPermissionSource().testPermission(deadPlayer, PermissionNodes.TOWNY_NATION_SIEGE_POINTS.getNode())
			&& universe.getPermissionSource().testPermission(killerPlayer, PermissionNodes.TOWNY_TOWN_SIEGE_POINTS.getNode())) {

			for (SiegeZone siegeZone : killerResidentTown.getSiege().getSiegeZones().values()) {
				if ((siegeZone.getAttackingNation() == deadResidentTown.getNation() || siegeZone.getAttackingNation().hasMutualAlly(deadResidentTown.getNation()))  //Was the dead player attacking in this siege zone ?
					&& deadPlayer.getLocation().distance(siegeZone.getFlagLocation()) < Settings.getWarSiegeZoneDeathRadiusBlocks()) {

					return siegeZone;
				}
			}
		}
		return null;
	}

	private static SiegeZone getSiegeZoneForDefendingSoldierKilledAttackingSoldier(
		Player deadPlayer, Player killerPlayer, Town deadResidentTown, Town killerResidentTown) throws NotRegisteredException
	{
		NationsUniverse universe = NationsUniverse.getInstance();
		if (killerResidentTown.hasNation()
			&& deadResidentTown.hasNation()
			&& universe.getPermissionSource().testPermission(deadPlayer, PermissionNodes.TOWNY_NATION_SIEGE_POINTS.getNode())
			&& universe.getPermissionSource().testPermission(killerPlayer, PermissionNodes.TOWNY_NATION_SIEGE_POINTS.getNode())) {

			for (SiegeZone siegeZone : universe.getDataSource().getSiegeZones()) {
				if (siegeZone.getSiege().getStatus() == SiegeStatus.IN_PROGRESS
					&& (killerResidentTown.getNation() == siegeZone.getDefendingTown().getNation() || killerResidentTown.getNation().hasMutualAlly(siegeZone.getDefendingTown().getNation())) //Is the killer player defending in this siege zone ?
					&& (deadResidentTown.getNation() == siegeZone.getAttackingNation() || deadResidentTown.getNation().hasMutualAlly(siegeZone.getAttackingNation())) //Was the dead player attacking in this siege zone ?
					&& deadPlayer.getLocation().distance(siegeZone.getFlagLocation()) < Settings.getWarSiegeZoneDeathRadiusBlocks()) {

					return siegeZone;
				}
			}
		}
		return null;
	}

	private static void awardSiegePvpPenaltyPoints(boolean attackerDeath,
											   NationsObject pointsRecipient,
											   Resident deadResident,
											   SiegeZone siegeZone) throws NotRegisteredException {
		/*
		 * If a defender was killed, reward all attackers.
		 * This prevents the exploit of a siege being joined by a 'pretend enemy' nation,
		 * and the defenders repeatedly dying to these soldiers,
		 * something which could be used if a siege is going badly, so that the town would fall to an actually friendly nation.
		 */
		if(attackerDeath) {
			SiegeWarPointsUtil.awardSiegePenaltyPoints(
				attackerDeath,
				pointsRecipient,
				deadResident,
				siegeZone,
				Settings.getLangString("msg_siege_war_attacker_death"));
		} else {
			for(SiegeZone siegeZoneInCollection: siegeZone.getSiege().getSiegeZones().values()) {
				SiegeWarPointsUtil.awardSiegePenaltyPoints(
					attackerDeath,
					siegeZoneInCollection.getAttackingNation(),
					deadResident,
					siegeZoneInCollection,
					Settings.getLangString("msg_siege_war_defender_death"));
			}
		}
	}
}