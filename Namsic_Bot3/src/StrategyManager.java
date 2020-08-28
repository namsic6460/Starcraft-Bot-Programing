import java.util.ArrayList;
import java.util.List;

import bwapi.Race;
import bwapi.Unit;
import bwapi.UnitType;
import bwta.BWTA;
import bwta.BaseLocation;

/// 상황을 판단하여, 정찰, 빌드, 공격, 방어 등을 수행하도록 총괄 지휘를 하는 class <br>
/// InformationManager 에 있는 정보들로부터 상황을 판단하고, <br>
/// BuildManager 의 buildQueue에 빌드 (건물 건설 / 유닛 훈련 / 테크 리서치 / 업그레이드) 명령을 입력합니다.<br>
/// 정찰, 빌드, 공격, 방어 등을 수행하는 코드가 들어가는 class
public class StrategyManager {

	private static StrategyManager instance = new StrategyManager();

	private CommandUtil commandUtil = new CommandUtil();

	public static int check = 0;
	public static List<Unit> checked = new ArrayList<Unit>();
	public static List<BaseLocation> bases = new ArrayList<BaseLocation>();
	public static boolean gotoExpansion = false;

	/// static singleton 객체를 리턴합니다
	public static StrategyManager Instance() {
		return instance;
	}

	/// 경기가 시작될 때 일회적으로 전략 초기 세팅 관련 로직을 실행합니다
	public void onStart() {
		setInitialBuildOrder();
	}

	/// 경기 진행 중 매 프레임마다 경기 전략 관련 로직을 실행합니다
	public void update() {
		if (MyBotModule.Broodwar.getFrameCount() == 10) {
			for (BaseLocation startLocation : BWTA.getStartLocations()) {
				if (startLocation == null)
					continue;

				if (startLocation.getX() == ScoutManager.pos.getX() && startLocation.getY() == ScoutManager.pos.getY())
					continue;

				if (startLocation.getX() == InformationManager.Instance()
						.getMainBaseLocation(MyBotModule.Broodwar.self()).getX()
						&& startLocation.getY() == InformationManager.Instance()
								.getMainBaseLocation(MyBotModule.Broodwar.self()).getY())
					continue;

				bases.add(startLocation);
			}
			
			if(bases.get(0).getGroundDistance(InformationManager.Instance().getMainBaseLocation(
					MyBotModule.Broodwar.self())) >=
					bases.get(1).getGroundDistance(InformationManager.Instance().getMainBaseLocation(
					MyBotModule.Broodwar.self())))
				bases.remove(0);
		}

		if (MyBotModule.Broodwar.getFrameCount() >= 10 && MyBotModule.Broodwar.getFrameCount() % 24 == 0) {
			if(!gotoExpansion)
				commandUtil.move(ScoutManager.currentScoutUnit, ScoutManager.pos);
			
			else
				commandUtil.move(ScoutManager.currentScoutUnit, InformationManager.Instance().getFirstExpansionLocation(
						MyBotModule.Broodwar.enemy()).getPosition());
		}

		for (Unit unit : MyBotModule.Broodwar.self().getUnits()) {
			if (unit.getType() == UnitType.Zerg_Spawning_Pool && !unit.isConstructing()) {
				check = 1;
			}

			else if (unit.getType() == UnitType.Zerg_Drone && !unit.isMoving()) {
				commandUtil.rightClick(unit, getNearestMineral(unit));
			}
		}

		if (check >= 1 && MyBotModule.Broodwar.getFrameCount() % 24 == 0) {
			Lets_Go();
		}
	}

	private Unit getNearestMineral(Unit unit) {
		int distant = 1000;
		Unit output = null;

		for (Unit u : MyBotModule.Broodwar.self().getUnits()) {
			if (u.getType() == UnitType.Resource_Mineral_Field) {
				if (u.getDistance(unit) < distant) {
					distant = u.getDistance(unit);
					output = u;
				}
			}
		}

		return output;
	}

	private void Lets_Go() {
		if (InformationManager.Instance().getMainBaseLocation(MyBotModule.Broodwar.enemy()) == null) {
			for (Unit unit : MyBotModule.Broodwar.self().getUnits()) {
				if (unit == null)
					continue;

				if (unit.getType() == UnitType.Zerg_Zergling && !unit.isAttacking())
					commandUtil.attackMove(unit, bases.get(0).getPosition());

				if (checked.contains(unit))
					continue;

				else {
					if (unit.getType() == UnitType.Zerg_Drone) {
						if (check == 4)
							commandUtil.move(unit, bases.get(0).getPosition());

						check++;
					}

					checked.add(unit);
				}
			}
		}

		else {
			for (Unit unit : MyBotModule.Broodwar.self().getUnits()) {
				if (unit == null)
					continue;

				if (unit.getType() == UnitType.Zerg_Zergling) {
					for (Unit u : unit.getUnitsInRadius(50)) {
						if (u == null)
							continue;

						if (u.getPlayer() == MyBotModule.Broodwar.self() && u.getType() == UnitType.Zerg_Hatchery) {
							commandUtil.attackMove(unit, InformationManager.Instance()
									.getMainBaseLocation(MyBotModule.Broodwar.enemy()).getPosition());
						}
					}
				}

				else if (unit.getType() == UnitType.Zerg_Hatchery && unit.isUnderAttack()) {
					for (Unit u : MyBotModule.Broodwar.self().getUnits()) {
						if (u == null)
							continue;

						if (u.getType() == UnitType.Zerg_Drone) {
							Unit enemy = getNearestEnemy(u, 200);

							if (enemy == null)
								continue;
							else
								u.attack(enemy);
						}
					}
				}

				if (checked.contains(unit)) {
					if (unit.getType() == UnitType.Zerg_Zergling) {
						if (!unit.isUnderAttack() && !unit.isAttacking() && !unit.isMoving()) {
							Unit u = getNearestEnemyBuilding(unit);

							if (u == null)
								u = getNearestEnemy(unit, 400);

							if (u == null)
								continue;

							unit.attack(u);
						}
					}
				}

				else {
					if (unit.getType() == UnitType.Zerg_Zergling)
						commandUtil.attackMove(unit, InformationManager.Instance().
								getMainBaseLocation(MyBotModule.Broodwar.enemy()).getPosition());

					checked.add(unit);
				}
			}
		}
	}

	public Unit getNearestEnemyBuilding(Unit unit) {
		Unit enemy = null;
		int distance = 100000;

		for (Unit u : MyBotModule.Broodwar.enemy().getUnits()) {
			if (u.getPlayer() == MyBotModule.Broodwar.enemy()) {
				if (!unit.getType().isBuilding())
					continue;

				if (unit.getDistance(u) < distance) {
					enemy = u;
					distance = unit.getDistance(u);
				}
			}
		}

		return enemy;
	}

	public Unit getNearestEnemy(Unit unit, int r) {
		Unit enemy = null;
		int distance = 100000;

		for (Unit u : MyBotModule.Broodwar.getUnitsInRadius(unit.getPosition(), r)) {
			if (u.getPlayer() == MyBotModule.Broodwar.enemy()) {
				if (unit.getDistance(u) < distance) {
					enemy = u;
					distance = unit.getDistance(u);
				}
			}
		}

		return enemy;
	}

	public void setInitialBuildOrder() {
		if (MyBotModule.Broodwar.self().getRace() == Race.Zerg) {
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Zerg_Spawning_Pool,
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);

			BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);

			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Zerg_Zergling,
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Zerg_Zergling,
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Zerg_Zergling,
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Zerg_Zergling,
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);

			BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);

			BuildManager.Instance().buildQueue.queueAsLowestPriority(
					InformationManager.Instance().getBasicSupplyProviderUnitType(),
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);

			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Zerg_Zergling,
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Zerg_Zergling,
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Zerg_Zergling,
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Zerg_Zergling,
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Zerg_Zergling,
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);

			BuildManager.Instance().buildQueue.queueAsLowestPriority(
					InformationManager.Instance().getBasicSupplyProviderUnitType(),
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);

			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Zerg_Zergling,
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Zerg_Zergling,
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Zerg_Zergling,
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Zerg_Zergling,
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Zerg_Zergling,
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Zerg_Zergling,
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Zerg_Zergling,
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Zerg_Zergling,
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);

			BuildManager.Instance().buildQueue.queueAsLowestPriority(
					InformationManager.Instance().getBasicSupplyProviderUnitType(),
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);

			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Zerg_Zergling,
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Zerg_Zergling,
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Zerg_Zergling,
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Zerg_Zergling,
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Zerg_Zergling,
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Zerg_Zergling,
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Zerg_Zergling,
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Zerg_Zergling,
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);

			BuildManager.Instance().buildQueue.queueAsLowestPriority(
					InformationManager.Instance().getBasicSupplyProviderUnitType(),
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);

			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Zerg_Zergling,
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Zerg_Zergling,
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Zerg_Zergling,
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Zerg_Zergling,
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Zerg_Zergling,
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Zerg_Zergling,
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Zerg_Zergling,
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Zerg_Zergling,
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);

			BuildManager.Instance().buildQueue.queueAsLowestPriority(
					InformationManager.Instance().getBasicSupplyProviderUnitType(),
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);

			/*
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.
			 * Instance().getWorkerType(),
			 * BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.
			 * Instance().getWorkerType(),
			 * BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.
			 * Zerg_Spawning_Pool, BuildOrderItem.SeedPositionStrategy.MainBaseLocation,
			 * true);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.
			 * Instance().getWorkerType(),
			 * BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.
			 * Zerg_Zergling, BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.
			 * Zerg_Zergling, BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.
			 * Zerg_Zergling, BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.
			 * Instance().getBasicSupplyProviderUnitType(),
			 * BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			 * 
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.
			 * Zerg_Hatchery, BuildOrderItem.SeedPositionStrategy.MainBaseLocation);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.
			 * Instance().getWorkerType(),
			 * BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.
			 * Zerg_Hatchery, BuildOrderItem.SeedPositionStrategy.FirstExpansionLocation);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.
			 * Instance().getWorkerType(),
			 * BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.
			 * Zerg_Hatchery, BuildOrderItem.SeedPositionStrategy.FirstExpansionLocation);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.
			 * Instance().getWorkerType(),
			 * BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(
			 * InformationManager.Instance().getBasicSupplyProviderUnitType(),
			 * BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.
			 * Instance().getWorkerType(),
			 * BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.
			 * Instance().getWorkerType(),
			 * BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.
			 * Instance().getWorkerType(),
			 * BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.
			 * Instance().getWorkerType(),
			 * BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.
			 * Instance().getWorkerType(),
			 * BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.
			 * Instance().getWorkerType(),
			 * BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			 * 
			 * // 가스 익스트랙터
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.
			 * Zerg_Extractor, BuildOrderItem.SeedPositionStrategy.MainBaseLocation);
			 * 
			 * // 성큰 콜로니 BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.
			 * Zerg_Creep_Colony, BuildOrderItem.SeedPositionStrategy.MainBaseLocation);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.
			 * Zerg_Sunken_Colony, BuildOrderItem.SeedPositionStrategy.MainBaseLocation);
			 * 
			 * BuildManager.Instance().buildQueue
			 * .queueAsLowestPriority(InformationManager.Instance().getRefineryBuildingType(
			 * ));
			 * 
			 * // 저글링 이동속도 업
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.
			 * Metabolic_Boost);
			 * 
			 * // 에볼루션 챔버 BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.
			 * Zerg_Evolution_Chamber); // 에볼루션 챔버 . 지상유닛 업그레이드
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.
			 * Zerg_Melee_Attacks, false);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.
			 * Zerg_Missile_Attacks, false);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.
			 * Zerg_Carapace, false);
			 * 
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.
			 * Instance().getWorkerType());
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.
			 * Instance().getWorkerType());
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.
			 * Instance().getWorkerType());
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.
			 * Instance().getWorkerType());
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.
			 * Instance().getWorkerType());
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.
			 * Instance().getWorkerType());
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.
			 * Instance().getWorkerType());
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.
			 * Instance().getWorkerType());
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.
			 * Instance().getWorkerType());
			 * 
			 * // 스포어 코로니 BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.
			 * Zerg_Creep_Colony, BuildOrderItem.SeedPositionStrategy.MainBaseLocation);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.
			 * Zerg_Spore_Colony, BuildOrderItem.SeedPositionStrategy.MainBaseLocation);
			 * 
			 * // 히드라 BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.
			 * Zerg_Hydralisk_Den);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.
			 * Zerg_Hydralisk);
			 * 
			 * // 레어
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Zerg_Lair);
			 * 
			 * // 오버로드 운반가능
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.
			 * Ventral_Sacs); // 오버로드 시야 증가
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.Antennae
			 * ); // 오버로드 속도 증가
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.
			 * Pneumatized_Carapace);
			 * 
			 * // 히드라 이동속도 업
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.
			 * Muscular_Augments, false); // 히드라 공격 사정거리 업
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.
			 * Grooved_Spines, false);
			 * 
			 * // 럴커 BuildManager.Instance().buildQueue.queueAsLowestPriority(TechType.
			 * Lurker_Aspect);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.
			 * Zerg_Hydralisk);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Zerg_Lurker
			 * );
			 * 
			 * // 스파이어
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Zerg_Spire,
			 * true); BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.
			 * Zerg_Mutalisk, true);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.
			 * Zerg_Scourge, true);
			 * 
			 * // 스파이어 . 공중유닛 업그레이드
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.
			 * Zerg_Flyer_Attacks, false);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.
			 * Zerg_Flyer_Carapace, false);
			 * 
			 * // 퀸 BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.
			 * Zerg_Queens_Nest);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Zerg_Queen)
			 * ; BuildManager.Instance().buildQueue.queueAsLowestPriority(TechType.
			 * Spawn_Broodlings, false);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(TechType.Ensnare,
			 * false); BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.
			 * Gamete_Meiosis, false);
			 * 
			 * // 하이브
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Zerg_Hive);
			 * // 저글링 공격 속도 업
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.
			 * Adrenal_Glands, false);
			 * 
			 * // 스파이어 . 그레이트 스파이어
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.
			 * Zerg_Greater_Spire, true);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.
			 * Zerg_Mutalisk, true);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.
			 * Zerg_Guardian, true);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.
			 * Zerg_Mutalisk, true);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.
			 * Zerg_Devourer, true);
			 * 
			 * // 울트라리스크 BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.
			 * Zerg_Ultralisk_Cavern);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.
			 * Zerg_Ultralisk); // 울트라리스크 이동속도 업
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.
			 * Anabolic_Synthesis, false); // 울트라리스크 방어력 업
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.
			 * Chitinous_Plating, false);
			 * 
			 * // 디파일러 BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.
			 * Zerg_Defiler_Mound);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.
			 * Zerg_Defiler);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(TechType.Consume,
			 * false);
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(TechType.Plague,
			 * false); // 디파일러 에너지 업
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.
			 * Metasynaptic_Node, false);
			 * 
			 * // 나이더스 캐널 BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.
			 * Zerg_Nydus_Canal);
			 * 
			 * // 참고로, Zerg_Nydus_Canal 건물로부터 Nydus Canal Exit를 만드는 방법은 다음과 같습니다 //if
			 * (MyBotModule.Broodwar.self().completedUnitCount(UnitType.Zerg_Nydus_Canal) >
			 * 0) { // for (Unit unit : MyBotModule.Broodwar.self().getUnits()) { // if
			 * (unit.getType() == UnitType.Zerg_Nydus_Canal) { // TilePosition
			 * targetTilePosition = new TilePosition(unit.getTilePosition().getX() + 6,
			 * unit.getTilePosition().getY()); // Creep 이 있는 곳이어야 한다 //
			 * unit.build(UnitType.Zerg_Nydus_Canal, targetTilePosition); // } // } //}
			 * 
			 * // 퀸 - 인페스티드 테란 : 테란 Terran_Command_Center 건물의 HitPoint가 낮을 때, 퀸을 들여보내서
			 * Zerg_Infested_Command_Center 로 바꾸면, 그 건물에서 실행 됨
			 * BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.
			 * Zerg_Infested_Terran);
			 */
		}
	}

}