import java.util.List;

import bwapi.Player;
import bwapi.Position;
import bwapi.Race;
import bwapi.Unit;
import bwapi.UnitType;
import bwapi.UpgradeType;
import bwta.BWTA;
import bwta.BaseLocation;
import bwta.Chokepoint;

/// 상황을 판단하여, 정찰, 빌드, 공격, 방어 등을 수행하도록 총괄 지휘를 하는 class <br>
/// InformationManager 에 있는 정보들로부터 상황을 판단하고, <br>
/// BuildManager 의 buildQueue에 빌드 (건물 건설 / 유닛 훈련 / 테크 리서치 / 업그레이드) 명령을 입력합니다.<br>
/// 정찰, 빌드, 공격, 방어 등을 수행하는 코드가 들어가는 class
public class StrategyManager {

	private static StrategyManager instance = new StrategyManager();

	private CommandUtil commandUtil = new CommandUtil();

	private boolean isFullScaleAttackStarted;

	private boolean endGasDeny = false;
	public boolean waitBuild = false;
	public Unit enemyGeyser = null;
	public Unit gasWorker = null;
	
	private int createdCarrier = 0;
	
	private boolean addedTree = false;
	public boolean needsScout = false;
	
	private Unit nexus = null;
	private Unit battery1 = null;
	private Unit battery2 = null;

	public static StrategyManager Instance() {
		return instance;
	}

	public StrategyManager() {
		isFullScaleAttackStarted = false;
	}

	public void onStart() {
		setInitialBuildOrder();
	}

	public void onEnd(boolean isWinner) {
	}

	public void update() {
		try {
		Race race = InformationManager.Instance().enemyRace;
		
		if(!endGasDeny && (race == Race.Terran || race == Race.Zerg))
			gasDeny();
		
		if (!addedTree) {
			if (race != Race.Unknown) {
//				if (race == Race.Protoss)
//					protossTree();
//				else
					notProtossTree();
				
				addedTree = true;
			}
		}

		List<Unit> units = InformationManager.Instance().selfPlayer.getUnits();
		List<Unit> enemies = InformationManager.Instance().enemyPlayer.getUnits();
		
		for(Unit unit : units) {
			protectBase(unit, units, enemies);
			
			if(MyBotModule.Broodwar.getFrameCount() % 6 == 0) {
				batteryControl(unit);
				carrierControl(unit, enemies);
				
				if(MyBotModule.Broodwar.getFrameCount() % 24 == 0) {
					executeCombat(units, enemies);
					trainInterceptor(unit);
				}
			}
		}
		
		if(createdCarrier >= 8)
			isFullScaleAttackStarted = true;

		checkPylon();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private void checkPylon() {
		Player self = InformationManager.Instance().selfPlayer;
		
		boolean flag = false;
		for(ConstructionTask task : ConstructionManager.Instance().getConstructionQueue()) {
			if(task.getType() == UnitType.Protoss_Pylon) {
				flag = true;
				break;
			}
		}
		
		if(flag)
			return;
		
		if (self.supplyTotal() == self.supplyUsed() && BuildManager.Instance().buildQueue
				.getHighestPriorityItem().metaType.getUnitType() != UnitType.Protoss_Pylon)
			BuildManager.Instance().buildQueue.queueAsHighestPriority(UnitType.Protoss_Pylon, false);
	}
	
	private void cancleGasDeny() {
		if(endGasDeny)
			return;
		
		waitBuild = false;
		endGasDeny = true;
		
		WorkerManager.Instance().getData().setWorkerJob(gasWorker, WorkerData.WorkerJob.Minerals,
				GameCommander.units.get(UnitType.Protoss_Nexus).get(0));
		commandUtil.rightClick(gasWorker, InformationManager.Instance().getMainBaseLocation(InformationManager.Instance().selfPlayer).getMinerals().get(0));
		gasWorker = null;
		
		System.out.println("canceled gas deny");
	}
	
	private void gasDeny() {
		if(MyBotModule.Broodwar.getFrameCount() > 8000)
			cancleGasDeny();
		
		BaseLocation enemyBase = InformationManager.Instance().getMainBaseLocation(InformationManager.Instance().enemyPlayer);
		if(enemyBase == null)
			return;
		
		enemyGeyser = enemyBase.getGeysers().get(0);
		
		if (enemyGeyser != null) {
			if(enemyGeyser.getPlayer() == InformationManager.Instance().selfPlayer || gasWorker == null)
				return;
			if (enemyGeyser.getPlayer() == InformationManager.Instance().enemyPlayer || !gasWorker.isVisible())
				cancleGasDeny();

			try {
				if (MyBotModule.Broodwar.isExplored(enemyGeyser.getTilePosition()))
					gasWorker.build(UnitType.Protoss_Assimilator, enemyGeyser.getTilePosition());
				else
					commandUtil.move(gasWorker, enemyGeyser.getPoint());
			} catch (Exception e) {
				cancleGasDeny();
			}
		}
	}
	
	private void trainInterceptor(Unit unit) {
		if (unit.getType() == UnitType.Protoss_Carrier) {
			if (unit.canTrain(UnitType.Protoss_Interceptor))
				unit.train(UnitType.Protoss_Interceptor);
		}
	}
	
	private void protectBase(Unit target, List<Unit> units, List<Unit> enemies) {
		if(isFullScaleAttackStarted)
			return;
		
		UnitType unitType = target.getType();
		char c = 'X';

		if(unitType == UnitType.Protoss_Probe && target != ScoutManager.Instance().getScoutUnit())
			c = WorkerManager.Instance().getWorkerData().getJobCode(target);
		
		if (unitType == UnitType.Protoss_Nexus || (Character.compare(c, 'X') != 0
				&& (Character.compare(c, 'G') == 0 || Character.compare(c, 'M') == 0))) {
			if (target.isUnderAttack()) {
				for (Unit unit : units) {
					if ((unit.canAttack()
							|| (unit.getType() == UnitType.Protoss_Carrier && unit.getInterceptorCount() > 1))) {
						if (unit.isIdle())
							commandUtil.attackMove(unit, target.getPoint());
					}
					
					else if(unit.getType() == UnitType.Protoss_Probe) {
						if(unit.getDistance(target) > 300)
							continue;
						
						if (unit.isGatheringMinerals() || unit.isGatheringGas()) {
							unit.stop();
							WorkerManager.Instance().getData().setWorkerJob(unit, WorkerData.WorkerJob.Combat, (Unit)null);
							unit.attack(getNearestUnit(target, enemies));
						}
					}
				}
			}
		}
	}
	
	private void batteryControl(Unit unit) { 
		if(isFullScaleAttackStarted || (battery1 == null && battery2 == null))
			return;
		
		if(unit.getShields() < 10) {
			if(battery1 != null && battery1.getEnergy() >= 20)
				unit.rightClick(battery1);
			else if(battery2 != null && battery2.getEnergy() >= 20)
				unit.rightClick(battery2);
		}
	}
	
	private void carrierControl(Unit unit, List<Unit> enemies) {
		if (unit.getType() != UnitType.Protoss_Carrier)
			return;
		
		if (unit.isUnderAttack() && !unit.isMoving()) {
			Unit enemy = getNearestUnit(unit, enemies);
			
			if(enemy == null)
				return;
			
			int tempX = enemy.getX() - unit.getX();
			int tempY = enemy.getY() - unit.getY();
			
			tempX = tempX > 0 ? 20 : -20;
			tempY = tempY > 0 ? 20 : -20;
			
			Position pos = new Position(unit.getX() - tempX, unit.getY() - tempY);
			commandUtil.move(unit, pos);
		}
	}
	
	private Unit getNearestUnit(Unit unit, List<Unit> enemies) {
		Unit nearestEnemy = null;
		double minDistance = 9999999;

		for(Unit enemy : enemies) {
			if(enemy.isInvincible() || enemy.isBurrowed())
				continue;
			
			int distance = unit.getDistance(enemy);
			
			if(distance < minDistance) {
				minDistance = distance;
				nearestEnemy = enemy;
			}
		}

		return nearestEnemy;
	}
	
	public void onUnitCreate(Unit unit) {
		UnitType unitType = unit.getType();
		
		if (unitType == UnitType.Protoss_Assimilator) {
			if (waitBuild && !endGasDeny) {
				if (enemyGeyser.getPlayer() == InformationManager.Instance().selfPlayer) {
					waitBuild = false;
					endGasDeny = true;

					WorkerManager.Instance().getData().setWorkerJob(gasWorker, WorkerData.WorkerJob.Minerals, (Unit) null);
					commandUtil.rightClick(gasWorker, InformationManager.Instance()
							.getMainBaseLocation(InformationManager.Instance().selfPlayer).getMinerals().get(0));
					gasWorker = null;
				}
			}
		}
		
		else if(unitType == UnitType.Protoss_Nexus)
			nexus = unit;
		
		else if(unitType == UnitType.Protoss_Shield_Battery) {
			if(battery1 == null)
				battery1 = unit;
			else
				battery2 = unit;
		}
		
		else if(unitType == UnitType.Protoss_Carrier)
			createdCarrier++;
	}
	
	public void onUnitDestroy(Unit unit) {
		UnitType unitType = unit.getType();
		
		if(unitType == UnitType.Protoss_Shield_Battery) {
			if(unit.getID() == battery1.getID())
				battery1 = null;
			else
				battery2 = null;
		}
	}
	
//	private Unit getNearestUnit(TilePosition pos, UnitType unitType) {
//		double distance = 9999999;
//		Unit nearestUnit = null;
//		
//		for(Unit unit : GameCommander.units.get(unitType)) {
//			double temp = BWTA.getGroundDistance(pos, unit.getTilePosition());
//			
//			if(temp < distance) {
//				distance = temp;
//				nearestUnit = unit;
//			}
//		}
//		
//		System.out.println("found!");
//		return nearestUnit;
//	}

	public void setInitialBuildOrder() {
		BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(
				InformationManager.Instance().getBasicSupplyProviderUnitType(),
				BuildOrderItem.SeedPositionStrategy.FirstChokePoint, true);
		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Forge,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);

		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Photon_Cannon,
				BuildOrderItem.SeedPositionStrategy.FirstChokePoint, true);
		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		
		//아마 정찰 완료
	}
	
	private void notProtossTree() {
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Nexus,
				BuildOrderItem.SeedPositionStrategy.FirstExpansionLocation, true);

		BuildManager.Instance().buildQueue.queueAsLowestPriority(
				InformationManager.Instance().getBasicSupplyProviderUnitType(),
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Gateway,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);

		BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);

		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Assimilator,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);

		BuildManager.Instance().buildQueue.queueAsLowestPriority(
				InformationManager.Instance().getBasicSupplyProviderUnitType(),
				BuildOrderItem.SeedPositionStrategy.SecondChokePoint, true);
		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Cybernetics_Core,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(
				InformationManager.Instance().getBasicSupplyProviderUnitType(),
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);

		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Photon_Cannon,
				BuildOrderItem.SeedPositionStrategy.SecondChokePoint, true);

		BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(
				InformationManager.Instance().getBasicSupplyProviderUnitType(),
				BuildOrderItem.SeedPositionStrategy.SecondChokePoint, true);
		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
				BuildOrderItem.SeedPositionStrategy.SecondChokePoint, true);

		BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
				BuildOrderItem.SeedPositionStrategy.FirstExpansionLocation, true);

		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Gateway,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Dragoon,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);

		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Photon_Cannon,
				BuildOrderItem.SeedPositionStrategy.SecondChokePoint, true);
		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);

		BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Shield_Battery,
				BuildOrderItem.SeedPositionStrategy.FirstExpansionLocation, true);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Shield_Battery,
				BuildOrderItem.SeedPositionStrategy.FirstExpansionLocation, true);

		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Gateway,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Dragoon,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Assimilator,
				BuildOrderItem.SeedPositionStrategy.FirstExpansionLocation, true);
		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
				BuildOrderItem.SeedPositionStrategy.FirstExpansionLocation, true);

		BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Stargate,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);

		BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
				BuildOrderItem.SeedPositionStrategy.FirstExpansionLocation, true);
		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Dragoon,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);

		BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
				BuildOrderItem.SeedPositionStrategy.FirstExpansionLocation, true);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
				BuildOrderItem.SeedPositionStrategy.FirstExpansionLocation, true);

		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Dragoon,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Dragoon,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);

		BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
				BuildOrderItem.SeedPositionStrategy.FirstExpansionLocation, true);

		BuildManager.Instance().buildQueue.queueAsLowestPriority(
				InformationManager.Instance().getBasicSupplyProviderUnitType(),
				BuildOrderItem.SeedPositionStrategy.SecondChokePoint, true);

		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Fleet_Beacon,
				BuildOrderItem.SeedPositionStrategy.MainBaseBackYard, true);
		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
				BuildOrderItem.SeedPositionStrategy.FirstExpansionLocation, true);

		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Stargate,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
				BuildOrderItem.SeedPositionStrategy.FirstExpansionLocation, true);

		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Dragoon,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
				BuildOrderItem.SeedPositionStrategy.FirstExpansionLocation, true);
		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Dragoon,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);

		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Stargate,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
				BuildOrderItem.SeedPositionStrategy.FirstExpansionLocation, true);
		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(
				InformationManager.Instance().getBasicSupplyProviderUnitType(),
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);

		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Dragoon,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
				BuildOrderItem.SeedPositionStrategy.FirstExpansionLocation, true);
		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(
				InformationManager.Instance().getBasicSupplyProviderUnitType(),
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);

		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(
				InformationManager.Instance().getBasicSupplyProviderUnitType(),
				BuildOrderItem.SeedPositionStrategy.SecondChokePoint, true);

		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Carrier,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);

		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Carrier,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(
				InformationManager.Instance().getBasicSupplyProviderUnitType(),
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);

		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Carrier,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);

		BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.Carrier_Capacity);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.Protoss_Plasma_Shields);
		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(
				InformationManager.Instance().getBasicSupplyProviderUnitType(),
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
				BuildOrderItem.SeedPositionStrategy.FirstExpansionLocation, true);
		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Stargate,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.Protoss_Air_Weapons);
		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(
				InformationManager.Instance().getBasicSupplyProviderUnitType(),
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Carrier,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(
				InformationManager.Instance().getBasicSupplyProviderUnitType(),
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Carrier,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(
				InformationManager.Instance().getBasicSupplyProviderUnitType(),
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Carrier,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(
				InformationManager.Instance().getBasicSupplyProviderUnitType(),
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(
				InformationManager.Instance().getBasicSupplyProviderUnitType(),
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);

		BuildManager.Instance().buildQueue.queueAsLowestPriority(
				InformationManager.Instance().getBasicSupplyProviderUnitType(),
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Carrier,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Carrier,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(
				InformationManager.Instance().getBasicSupplyProviderUnitType(),
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Carrier,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.Protoss_Air_Armor);

		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(
				InformationManager.Instance().getBasicSupplyProviderUnitType(),
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(
				InformationManager.Instance().getBasicSupplyProviderUnitType(),
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);

		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Carrier,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Carrier,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Carrier,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(
				InformationManager.Instance().getBasicSupplyProviderUnitType(),
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(
				InformationManager.Instance().getBasicSupplyProviderUnitType(),
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);

		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Carrier,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Carrier,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Carrier,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);

		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Carrier,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Carrier,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Carrier,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);

		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Carrier,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Carrier,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Carrier,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);		
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);

		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Carrier,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Carrier,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Carrier,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
	}
	
	private void protossTree() {
		
	}
	
	private void executeCombat(List<Unit> units, List<Unit> enemies) {

		// 공격 모드가 아닐 때에는 전투유닛들을 아군 진영 길목에 집결시켜서 방어
		if (isFullScaleAttackStarted == false) {
			Chokepoint firstChokePoint = BWTA.getNearestChokepoint(InformationManager.Instance()
						.getSecondChokePoint(InformationManager.Instance().selfPlayer).getPoint());

			for (Unit unit : units) {
				UnitType unitType = unit.getType();
				
				if (unitType.isBuilding() || unitType.isWorker()) 
					continue;
									
				if ((unit.canAttack() && unit.isIdle()) || unit.getType() == UnitType.Protoss_Carrier) 
					commandUtil.attackMove(unit, firstChokePoint.getCenter());
			}
		}
		
		// 공격 모드가 되면, 모든 전투유닛들을 적군 Main BaseLocation 로 공격 가도록 합니다
		else {
			//std.cout + "enemy OccupiedBaseLocations : " + InformationManager.Instance().getOccupiedBaseLocations(InformationManager.Instance()._enemy).size() + std.endl;
			
			if (InformationManager.Instance().enemyPlayer != null
					&& InformationManager.Instance().enemyRace != Race.Unknown 
					&& InformationManager.Instance().getOccupiedBaseLocations(InformationManager.Instance().enemyPlayer).size() > 0) 
			{					
				// 공격 대상 지역 결정
				BaseLocation targetBaseLocation = null;
				double closestDistance = 100000000;
				
				if(nexus == null)
					targetBaseLocation = InformationManager.Instance().getMainBaseLocation(InformationManager.Instance().enemyPlayer);

				else {
					for (BaseLocation baseLocation : InformationManager.Instance()
							.getOccupiedBaseLocations(InformationManager.Instance().enemyPlayer)) {
						double distance = nexus.getDistance(baseLocation.getPoint());

						if (distance < closestDistance) {
							closestDistance = distance;
							targetBaseLocation = baseLocation;
						}
					}
				}

				if (targetBaseLocation != null) {
					for (Unit unit : units) {
						// 건물은 제외
						if (unit.getType().isBuilding())
							continue;
						// 모든 일꾼은 제외
						if (unit.getType().isWorker())
							continue;
											
						// canAttack 유닛은 attackMove Command 로 공격을 보냅니다
						if (unit.canAttack() || unit.getType() == UnitType.Protoss_Carrier) {
							if(unit.isIdle()) {
								Unit enemy = getNearestUnit(unit, enemies);
								
								if(enemy == null) {
									commandUtil.attackMove(unit, targetBaseLocation.getPosition());
									
									if(!needsScout && ScoutManager.Instance().getScoutUnit() == null)
										needsScout = true;
								}
								
								else {
									if(!unit.isAttacking() && !unit.isStartingAttack() && !unit.isUnderAttack()) 
										unit.attack(enemy);
								}
							}
						} 
					}
				}
			}
		}
	}
	
}