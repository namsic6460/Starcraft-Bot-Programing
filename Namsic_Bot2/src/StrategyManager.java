import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
	private int chokePointType = 2; // 1이면 본진 집합, 2면 앞마당 집합

	private ArrayList<UnitType> canMoving = new ArrayList<UnitType>();
	private ArrayList<UnitType> detector = new ArrayList<UnitType>(); 
	private ArrayList<UnitType> canAttack = new ArrayList<UnitType>();
	private int timing = 0;
	
	private Map<Integer, Integer> movingCount = new HashMap<Integer, Integer>();
	
	BuildOrderItem buildOrderItem;

	// BasicBot 1.1 Patch End //////////////////////////////////////////////////

	/// static singleton 객체를 리턴합니다
	public static StrategyManager Instance() {
		return instance;
	}

	public StrategyManager() {
		isFullScaleAttackStarted = false;
	}

	/// 경기가 시작될 때 일회적으로 전략 초기 세팅 관련 로직을 실행합니다
	public void onStart() {
		setInitialBuildOrder();
		
		canMoving.add(UnitType.Protoss_Archon);
		canMoving.add(UnitType.Protoss_Probe);
		canMoving.add(UnitType.Protoss_Zealot);
		canMoving.add(UnitType.Terran_Firebat);
		canMoving.add(UnitType.Terran_SCV);
		canMoving.add(UnitType.Zerg_Drone);
		canMoving.add(UnitType.Zerg_Infested_Terran);
		canMoving.add(UnitType.Zerg_Ultralisk);
		canMoving.add(UnitType.Zerg_Zergling);
		
		detector.add(UnitType.Terran_Science_Vessel);
		detector.add(UnitType.Protoss_Observer);
		detector.add(UnitType.Zerg_Overlord);
		
		canAttack.add(UnitType.Protoss_Zealot);
		canAttack.add(UnitType.Protoss_Dragoon);
		canAttack.add(UnitType.Protoss_Dark_Templar);
		canAttack.add(UnitType.Protoss_Scout);
		canAttack.add(UnitType.Protoss_Carrier);
		canAttack.add(UnitType.Protoss_Observer);
	}

	/// 경기 진행 중 매 프레임마다 경기 전략 관련 로직을 실행합니다
	public void update() {
		if(MyBotModule.Broodwar.getFrameCount() % 240 == 0)
			System.out.println(BuildManager.Instance().getBuildQueue().getQueue().element().metaType.getName());
		
		executeCombat();
		
		if(BuildManager.Instance().getBuildQueue().getQueue().element().metaType.getName().equals("Protoss_Interceptor")) {
			BuildManager.Instance().buildQueue.removeHighestPriorityItem();
			System.out.println("Deleted Interceptor");
		}
		if(BuildManager.Instance().getBuildQueue().getQueue().element().metaType.getName().equals("Protoss_Interceptor")) {
			BuildManager.Instance().buildQueue.removeHighestPriorityItem();
			System.out.println("Deleted Interceptor");
		}

		for (Unit unit : MyBotModule.Broodwar.self().getUnits()) {
			if(movingCount.containsKey(unit.getID())) {
				int id = unit.getID();
				
				if(movingCount.get(id) == 0) {
					movingCount.remove(id);
					continue;
				}
				
				else if(movingCount.get(id) != 0)
					movingCount.replace(id, (movingCount.get(id) - 1));
				
				if(movingCount.get(id) == 2) {
					Unit enemy = getNearestEnemy(unit);
					
					if(enemy != null) {
						System.out.println(unit.getType().toString() + " - Unit ID : " + id + " - Moved to attack - Distance : " + getDistance(unit, enemy));
						unit.attack(enemy);
					}
				}				
			}
			
			if (unit.getType() == UnitType.Protoss_Dragoon) {
				Unit enemy = getNearestEnemy(unit);

				if(enemy != null ) {
					if (!unit.isMoving() && unit.isUnderAttack() && enemy.isAttacking() && !unit.isStuck()) {
						if (canMoving.contains(enemy.getType()) && !movingCount.containsKey(unit.getID())) {
							Position pos = movingPosition(unit, enemy, 1.5);

							if (pos != null) {
								System.out.println("Dragoon - Unit ID : " + unit.getID() + " - Moved to " + pos.getX() + ", " + pos.getY());
								movingCount.put(unit.getID(), 34);
							
								commandUtil.move(unit, pos);
							}
						}
					}
				}
			}
			
			else if(unit.getType() == UnitType.Protoss_Probe && unit.isUnderAttack()) {
				Unit enemy = getNearestEnemy(unit);
				
				if(enemy != null) {
					if(!unit.isAttacking() && getDistance(unit, enemy) < 70 && enemy.getType() == UnitType.Zerg_Zergling)
						unit.attack(enemy);
				}
			}
			
			else if(unit.getType() == UnitType.Protoss_Carrier) {
				if(unit.isCompleted() && unit.isFlying()) {
					if(!BuildManager.Instance().getBuildQueue().getQueue().element().metaType.getName().equals("Protoss_Interceptor")) {
						if(unit.isCompleted() && !unit.isConstructing() && !unit.isTraining() && unit.getInterceptorCount() < 8) {
							BuildManager.Instance().buildQueue.queueAsHighestPriority(UnitType.Protoss_Interceptor, true);
							System.out.println("Created Interceptor - unit ID : " + unit.getID());
						}
					}
					
					Unit enemy = getNearestEnemy(unit);
					
					if(enemy != null ) {
						if (!unit.isMoving() && unit.isUnderAttack() && enemy.isAttacking()) {
							if (!movingCount.containsKey(unit.getID())) {
								Position pos = movingPosition(unit, enemy, 1);

								if (pos != null) {
									System.out.println("Carrier - Unit ID : " + unit.getID() + " - Moved to " + pos.getX() + ", " + pos.getY());
									movingCount.put(unit.getID(), 24);
								
									commandUtil.move(unit, pos);
								}
							}
						}
					}
				}
			}
			
			else if(unit.getType() == UnitType.Protoss_Dark_Templar) {
				Unit enemy = getNearestFindEnemy(unit);
				
				if(enemy != null ) {
					if (unit.isUnderAttack() && unit.isDetected()) {
						if (!movingCount.containsKey(unit.getID())) {
							Position pos = movingPosition(unit, enemy, 1);

							if (pos != null) {
								System.out.println("Dark_Templar - Unit ID : " + unit.getID() + " - Moved to " + pos.getX() + ", " + pos.getY());
								movingCount.put(unit.getID(), 24);
							
								commandUtil.move(unit, pos);
							}
						}
					}
				}
			}

			else
				continue;
		}

		if (MyBotModule.Broodwar.self().gas() >= 550 && !WorkerManager.nonGasTiming) {
			WorkerManager.nonGasTiming = true;
			System.out.println("nonGasTiming is True");
		}
		else if (MyBotModule.Broodwar.self().gas() <= 300 && WorkerManager.nonGasTiming) {
			WorkerManager.nonGasTiming = false;
			System.out.println("nonGasTiming is False");
		}

		
		if (GameCommander.createdDragoonCount == 6 && timing == 0) {
			isFullScaleAttackStarted = true;
			timing++;
			printIsFullScaleAttack();
		}
		
		if(GameCommander.createdDragoonCount == 8 && timing == 1) {
			isFullScaleAttackStarted = false;
			timing++;
			printIsFullScaleAttack();
		}
		
		else if(GameCommander.createdScoutCount == 8 && timing == 2) {
			isFullScaleAttackStarted = true;
			timing++;
			printIsFullScaleAttack();
		}
	}

	private void printIsFullScaleAttack() {
		System.out.println("isFullScaleAttack is " + isFullScaleAttackStarted + " - Timing : " + timing);
	}

	private Position movingPosition(Unit unit, Unit enemy, double coe) {
		if (enemy == null)
			return null;

		int uX = unit.getX(), uY = unit.getY();
		int eX = enemy.getX(), eY = enemy.getY();

		int X = uX + (int)((uX - eX) * coe);
		int Y = uY + (int)((uY - eY) * coe);

		Position output = new Position(X, Y);
		
		return output;
	}

	private Unit getNearestEnemy(Unit unit) {
		Unit nearestEnemy = null;
		double nearestDistance = 100000000;

		for (Unit enemy : MyBotModule.Broodwar.enemy().getUnits()) {
			if (enemy == null)
				continue;
			if(enemy.getType().isBuilding())
				continue;
			if(enemy.getType().isWorker())
				continue;

			double distance = getDistance(unit, enemy);

			if (distance < nearestDistance) {
				nearestDistance = distance;
				nearestEnemy = enemy;
			}
		}

		return nearestEnemy;
	}
	
	private Unit getNearestFindEnemy(Unit unit) {
		Unit nearestEnemy = null;
		double nearestDistance = 100000000;
		
		for (Unit enemy : MyBotModule.Broodwar.enemy().getUnits()) {
			if (enemy == null)
				continue;
			if(enemy.getType().isBuilding())
				continue;
			if(enemy.getType().isWorker())
				continue;
			
			if(!detector.contains(unit.getType()))
				continue;

			double distance = getDistance(unit, enemy);

			if (distance < nearestDistance) {
				nearestDistance = distance;
				nearestEnemy = enemy;
			}
		}

		return nearestEnemy;
	}
	
//	private Unit getNearestAlly(Unit unit) {
//		Unit nearestAlly = null;
//		double nearestDistance = 100000000;
//		
//		for (Unit ally : MyBotModule.Broodwar.self().getUnits()) {
//			if (ally == null)
//				continue;
//
//			else {
//				double distance = getDistance(unit, ally);
//			
//				if (distance < nearestDistance) {
//					nearestDistance = distance;
//					nearestAlly = ally;
//				}
//			}
//		}
//
//		return nearestAlly;
//	}
	
	private Unit getNearestAlly(Unit unit, UnitType exception) {
		Unit nearestAlly = null;
		double nearestDistance = 100000000;
		
		for (Unit ally : MyBotModule.Broodwar.self().getUnits()) {
			if (ally == null)
				continue;
			if(ally.getType().isBuilding())
				continue;
			if(ally.getType().isWorker())
				continue;
			if(unit.getType() == exception)
				continue;
			
			else {
				double distance = getDistance(unit, ally);

				if (distance < nearestDistance) {
					nearestDistance = distance;
					nearestAlly = ally;
				}
			}
		}

		return nearestAlly;
	}

	private double getDistance(Unit unit, Unit enemy) {
		double uX = unit.getX(), uY = unit.getY();
		double eX = enemy.getX(), eY = enemy.getY();

		return Math.sqrt(Math.pow(eX - uX, 2) + Math.pow(eY - uY, 2));
	}

	public void setInitialBuildOrder() {
		if (MyBotModule.Broodwar.self().getRace() == Race.Protoss) {
			BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);

			BuildManager.Instance().buildQueue.queueAsLowestPriority(
					InformationManager.Instance().getBasicSupplyProviderUnitType(),
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);

			BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Gateway,
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);

			BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(
					InformationManager.Instance().getBasicSupplyProviderUnitType(),
					BuildOrderItem.SeedPositionStrategy.FirstExpansionLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);

			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Gateway,
					BuildOrderItem.SeedPositionStrategy.FirstExpansionLocation, true);

			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Assimilator,
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation);

			BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);

			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Cybernetics_Core,
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation);

			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);

			BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.
					Protoss_Forge, BuildOrderItem.SeedPositionStrategy.MainBaseLocation);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(
					InformationManager.Instance().getBasicSupplyProviderUnitType(),
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);

			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Dragoon);

			BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.Singularity_Charge);

			BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);

			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.
					 Protoss_Photon_Cannon, BuildOrderItem.SeedPositionStrategy.SecondChokePoint);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.
					 Protoss_Photon_Cannon, BuildOrderItem.SeedPositionStrategy.SecondChokePoint);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.
					 Protoss_Photon_Cannon, BuildOrderItem.SeedPositionStrategy.SecondChokePoint);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(
					InformationManager.Instance().getBasicSupplyProviderUnitType(),
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);

			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Dragoon);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);

			BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);

			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Dragoon);

			BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Gateway,
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);

			BuildManager.Instance().buildQueue.queueAsLowestPriority(
					InformationManager.Instance().getBasicSupplyProviderUnitType(),
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);

			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Dragoon);

			BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);

			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);

			BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(
					InformationManager.Instance().getBasicSupplyProviderUnitType(),
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);

			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Gateway,
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);

			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Dragoon);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Dragoon);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(
					InformationManager.Instance().getBasicSupplyProviderUnitType(),
					BuildOrderItem.SeedPositionStrategy.FirstExpansionLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);

			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Dragoon);

			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);

			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Nexus,
					BuildOrderItem.SeedPositionStrategy.FirstExpansionLocation);			

			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Dragoon);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(
					InformationManager.Instance().getBasicSupplyProviderUnitType(),
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Robotics_Facility);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Gateway,
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);

			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Dragoon);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Stargate);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(
					InformationManager.Instance().getBasicSupplyProviderUnitType(),
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(
					InformationManager.Instance().getBasicSupplyProviderUnitType(),
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Citadel_of_Adun);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(
					InformationManager.Instance().getBasicSupplyProviderUnitType(),
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.Leg_Enhancements);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Assimilator,
					BuildOrderItem.SeedPositionStrategy.FirstExpansionLocation);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Stargate);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
					BuildOrderItem.SeedPositionStrategy.FirstExpansionLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(
					InformationManager.Instance().getBasicSupplyProviderUnitType(),
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
					BuildOrderItem.SeedPositionStrategy.FirstExpansionLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Stargate);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
					BuildOrderItem.SeedPositionStrategy.FirstExpansionLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
					BuildOrderItem.SeedPositionStrategy.FirstExpansionLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
					BuildOrderItem.SeedPositionStrategy.FirstExpansionLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(
					InformationManager.Instance().getBasicSupplyProviderUnitType(),
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
					BuildOrderItem.SeedPositionStrategy.FirstExpansionLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Stargate);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
					BuildOrderItem.SeedPositionStrategy.FirstExpansionLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Dragoon);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
					BuildOrderItem.SeedPositionStrategy.FirstExpansionLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Observatory);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
					BuildOrderItem.SeedPositionStrategy.FirstExpansionLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(
					InformationManager.Instance().getBasicSupplyProviderUnitType(),
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Fleet_Beacon);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
					BuildOrderItem.SeedPositionStrategy.FirstExpansionLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Scout);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Stargate);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
					BuildOrderItem.SeedPositionStrategy.FirstExpansionLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Observer);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Dragoon);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
					BuildOrderItem.SeedPositionStrategy.FirstExpansionLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Scout);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Scout);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
					BuildOrderItem.SeedPositionStrategy.FirstExpansionLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(
					InformationManager.Instance().getBasicSupplyProviderUnitType(),
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.Carrier_Capacity);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
					BuildOrderItem.SeedPositionStrategy.FirstExpansionLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.Gravitic_Boosters);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
					BuildOrderItem.SeedPositionStrategy.FirstExpansionLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Scout);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Carrier);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Dragoon);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(
					InformationManager.Instance().getBasicSupplyProviderUnitType(),
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Carrier);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Scout);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(
					InformationManager.Instance().getBasicSupplyProviderUnitType(),
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Observer);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Carrier);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Observer);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(
					InformationManager.Instance().getBasicSupplyProviderUnitType(),
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Dragoon);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Observer);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Dragoon);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(
					InformationManager.Instance().getBasicSupplyProviderUnitType(),
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Scout);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Scout);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Observer);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Scout);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(
					InformationManager.Instance().getBasicSupplyProviderUnitType(),
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Scout);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Scout);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Dragoon);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Dragoon);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Scout);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(
					InformationManager.Instance().getBasicSupplyProviderUnitType(),
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Scout);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Scout);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Dragoon);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Dragoon);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Scout);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Scout);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Scout);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Dragoon);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Dragoon);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Scout);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Scout);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Scout);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Dragoon);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Dragoon);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Scout);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Scout);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Scout);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Dragoon);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Dragoon);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Scout);
			
			/*
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.
			 * Protoss_Assimilator, BuildOrderItem.SeedPositionStrategy.MainBaseLocation);
			 * 
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.
			 * Protoss_Forge, BuildOrderItem.SeedPositionStrategy.MainBaseLocation);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.
			 * Protoss_Photon_Cannon, BuildOrderItem.SeedPositionStrategy.MainBaseLocation);
			 * 
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.
			 * Protoss_Gateway, BuildOrderItem.SeedPositionStrategy.MainBaseLocation);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.
			 * Protoss_Zealot);
			 * 
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.
			 * Protoss_Cybernetics_Core,
			 * BuildOrderItem.SeedPositionStrategy.MainBaseLocation);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.
			 * Protoss_Dragoon); // 드라군 사정거리 업그레이드
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.
			 * Singularity_Charge);
			 * 
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.
			 * Protoss_Citadel_of_Adun); // 질럿 속도 업그레이드
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.
			 * Leg_Enhancements);
			 * 
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.
			 * Protoss_Shield_Battery);
			 * 
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.
			 * Protoss_Templar_Archives); // 하이템플러
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.
			 * Protoss_High_Templar);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.
			 * Protoss_High_Templar);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(TechType.
			 * Psionic_Storm);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(TechType.
			 * Hallucination);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.
			 * Khaydarin_Amulet);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.
			 * Protoss_Archon);
			 * 
			 * // 다크아칸 BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.
			 * Protoss_Dark_Templar);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.
			 * Protoss_Dark_Templar);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(TechType.Maelstrom);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(TechType.
			 * Mind_Control);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.
			 * Argus_Talisman);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.
			 * Protoss_Dark_Archon);
			 * 
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.
			 * Protoss_Robotics_Facility);
			 * 
			 * // 셔틀 BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.
			 * Protoss_Shuttle);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.
			 * Protoss_Robotics_Support_Bay);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.
			 * Gravitic_Drive);
			 * 
			 * // 리버 BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.
			 * Protoss_Reaver);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.
			 * Scarab_Damage);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.
			 * Reaver_Capacity);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.
			 * Protoss_Scarab);
			 * 
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.
			 * Protoss_Observatory); // 옵저버
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.
			 * Protoss_Observer);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.
			 * Gravitic_Boosters);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.
			 * Sensor_Array);
			 * 
			 * // 공중유닛 BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.
			 * Protoss_Stargate);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.
			 * Protoss_Fleet_Beacon);
			 * 
			 * // 스카우트 BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.
			 * Protoss_Scout);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.
			 * Apial_Sensors);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.
			 * Gravitic_Thrusters);
			 * 
			 * // 커세어 BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.
			 * Protoss_Corsair);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(TechType.
			 * Disruption_Web);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.
			 * Argus_Jewel);
			 * 
			 * // 캐리어 BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.
			 * Protoss_Carrier);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.
			 * Carrier_Capacity);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.
			 * Protoss_Interceptor);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.
			 * Protoss_Interceptor);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.
			 * Protoss_Interceptor);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.
			 * Protoss_Interceptor);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.
			 * Protoss_Interceptor);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.
			 * Protoss_Interceptor);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.
			 * Protoss_Interceptor);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.
			 * Protoss_Interceptor);
			 * 
			 * // 아비터 BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.
			 * Protoss_Arbiter_Tribunal);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.
			 * Protoss_Arbiter);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(TechType.Recall);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(TechType.
			 * Stasis_Field);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.
			 * Khaydarin_Core);
			 * 
			 * // 포지 - 지상 유닛 업그레이드
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.
			 * Protoss_Ground_Weapons);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.
			 * Protoss_Plasma_Shields);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.
			 * Protoss_Ground_Armor);
			 * 
			 * // 사이버네틱스코어 - 공중 유닛 업그레이드
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.
			 * Protoss_Air_Weapons);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.
			 * Protoss_Air_Armor);
			 * 
			 */
		}
	}

	public void executeCombat() {
		// 공격 모드가 아닐 때에는 전투유닛들을 아군 진영 길목에 집결시켜서 방어
		if (isFullScaleAttackStarted == false) {
			Chokepoint chokePoint = null;
			if (chokePointType == 1)
				chokePoint = BWTA.getNearestChokepoint(InformationManager.Instance()
						.getMainBaseLocation(InformationManager.Instance().selfPlayer).getTilePosition());
			else if (chokePointType == 2)
				chokePoint = BWTA.getNearestChokepoint(InformationManager.Instance()
						.getSecondChokePoint(InformationManager.Instance().selfPlayer).getPoint());
			for (Unit unit : MyBotModule.Broodwar.self().getUnits()) {
				if(unit.getType() == UnitType.Protoss_Observer || unit.getType() == UnitType.Protoss_Carrier) {
					commandUtil.move(unit, chokePoint.getCenter());
				}
				
				else if (unit.canAttack() == true && unit.isAttacking() == false && unit.getType().isWorker() == false
						&& unit.getType().isBuilding() == false) {
					commandUtil.attackMove(unit, chokePoint.getCenter());
				}
			}
		}
		// 공격 모드가 되면, 모든 전투유닛들을 적군 Main BaseLocation 로 공격 가도록 합니다
		else {
			// std.cout + "enemy OccupiedBaseLocations : " +
			// InformationManager.Instance().getOccupiedBaseLocations(InformationManager.Instance()._enemy).size()
			// + std.endl;

			if (InformationManager.Instance().enemyPlayer != null
					&& InformationManager.Instance().enemyRace != Race.Unknown && InformationManager.Instance()
							.getOccupiedBaseLocations(InformationManager.Instance().enemyPlayer).size() > 0) {
				// 공격 대상 지역 결정
				BaseLocation targetBaseLocation = InformationManager.Instance().getMainBaseLocation(InformationManager.Instance().enemyPlayer);
				
				if (targetBaseLocation != null) {
					for (Unit unit : MyBotModule.Broodwar.self().getUnits()) {
						if (!canAttack.contains(unit.getType()))
							continue;
						
						if(unit.getType() == UnitType.Protoss_Observer) {
							for(Unit enemy : MyBotModule.Broodwar.enemy().getUnits()) {
								if(enemy != null) {
									if(enemy.getType().isBurrowable() || enemy.getType().isCloakable())
										commandUtil.move(unit, enemy.getPosition());
									
									else {
										Unit ally = getNearestAlly(unit, UnitType.Protoss_Observer);
										
										if(ally != null)
											commandUtil.move(unit, ally.getPosition());
										
										break;
									}
								}
								
								else {
									Unit ally = getNearestAlly(unit, UnitType.Protoss_Observer);
									
									commandUtil.move(unit, ally.getPosition());
								}
							}
						}
						
						else if(unit.getType() == UnitType.Protoss_Carrier && unit.getInterceptorCount() > 3)
							commandUtil.attackMove(unit, targetBaseLocation.getPosition());
						else
							commandUtil.attackMove(unit, targetBaseLocation.getPosition());
					}
				}
			}
		}
	}
}