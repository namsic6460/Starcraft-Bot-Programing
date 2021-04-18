
import bwapi.Race;
import bwapi.TechType;
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
	
	/// static singleton 객체를 리턴합니다
	public static StrategyManager Instance() {
		return instance;
	}

	public StrategyManager() {
		isFullScaleAttackStarted = false;
	}

	/// 경기가 시작될 때 일회적으로 전략 초기 세팅 관련 로직을 실행합니다
	public void onStart() {

		// BasicBot 1.1 Patch End //////////////////////////////////////////////////

		setInitialBuildOrder();
	}

	///  경기가 종료될 때 일회적으로 전략 결과 정리 관련 로직을 실행합니다
	public void onEnd(boolean isWinner) {
	}

	/// 경기 진행 중 매 프레임마다 경기 전략 관련 로직을 실행합니다
	public void update() {
		executeCombat();
	}

	public void setInitialBuildOrder() {
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Probe,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Probe,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Probe,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);

		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Pylon,
				BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
//		if (MyBotModule.Broodwar.self().getRace() == Race.Protoss) {
//			BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
//					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
//			BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
//					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
//			BuildManager.Instance().buildQueue.queueAsLowestPriority(
//					InformationManager.Instance().getBasicSupplyProviderUnitType(),
//					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
//			BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
//					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
//			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Gateway,
//					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
//			BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
//					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
//			BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
//					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
//			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot,
//					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
//
//			
//			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Assimilator,
//					BuildOrderItem.SeedPositionStrategy.MainBaseLocation);
//
//			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Forge,
//					BuildOrderItem.SeedPositionStrategy.MainBaseLocation);
//			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Photon_Cannon,
//					BuildOrderItem.SeedPositionStrategy.MainBaseLocation);
//
//			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Gateway,
//					BuildOrderItem.SeedPositionStrategy.MainBaseLocation);
//			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
//
//			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Cybernetics_Core,
//					BuildOrderItem.SeedPositionStrategy.MainBaseLocation);
//			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Dragoon);
//			// 드라군 사정거리 업그레이드
//			BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.Singularity_Charge);
//
//			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Citadel_of_Adun);
//			// 질럿 속도 업그레이드
//			BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.Leg_Enhancements);
//
//			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Shield_Battery);
//
//			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Templar_Archives);
//			// 하이템플러
//			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_High_Templar);
//			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_High_Templar);
//			BuildManager.Instance().buildQueue.queueAsLowestPriority(TechType.Psionic_Storm);
//			BuildManager.Instance().buildQueue.queueAsLowestPriority(TechType.Hallucination);
//			BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.Khaydarin_Amulet);
//			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Archon);
//
//			// 다크아칸
//			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Dark_Templar);
//			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Dark_Templar);
//			BuildManager.Instance().buildQueue.queueAsLowestPriority(TechType.Maelstrom);
//			BuildManager.Instance().buildQueue.queueAsLowestPriority(TechType.Mind_Control);
//			BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.Argus_Talisman);
//			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Dark_Archon);
//
//			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Robotics_Facility);
//
//			// 셔틀
//			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Shuttle);
//			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Robotics_Support_Bay);
//			BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.Gravitic_Drive);
//
//			// 리버
//			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Reaver);
//			BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.Scarab_Damage);
//			BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.Reaver_Capacity);
//			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Scarab);
//
//			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Observatory);
//			// 옵저버
//			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Observer);
//			BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.Gravitic_Boosters);
//			BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.Sensor_Array);
//
//			// 공중유닛
//			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Stargate);
//			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Fleet_Beacon);
//
//			// 스카우트
//			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Scout);
//			BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.Apial_Sensors);
//			BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.Gravitic_Thrusters);
//
//			// 커세어
//			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Corsair);
//			BuildManager.Instance().buildQueue.queueAsLowestPriority(TechType.Disruption_Web);
//			BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.Argus_Jewel);
//
//			// 캐리어
//			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Carrier);
//			BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.Carrier_Capacity);
//			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Interceptor);
//			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Interceptor);
//			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Interceptor);
//			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Interceptor);
//			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Interceptor);
//			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Interceptor);
//			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Interceptor);
//			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Interceptor);
//
//			// 아비터
//			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Arbiter_Tribunal);
//			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Arbiter);
//			BuildManager.Instance().buildQueue.queueAsLowestPriority(TechType.Recall);
//			BuildManager.Instance().buildQueue.queueAsLowestPriority(TechType.Stasis_Field);
//			BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.Khaydarin_Core);
//
//			// 포지 - 지상 유닛 업그레이드
//			BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.Protoss_Ground_Weapons);
//			BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.Protoss_Plasma_Shields);
//			BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.Protoss_Ground_Armor);
//
//			// 사이버네틱스코어 - 공중 유닛 업그레이드
//			BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.Protoss_Air_Weapons);
//			BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.Protoss_Air_Armor);
//			
//		}
	}
	
	public void executeCombat() {

		// 공격 모드가 아닐 때에는 전투유닛들을 아군 진영 길목에 집결시켜서 방어
		if (isFullScaleAttackStarted == false) {
//			Chokepoint firstChokePoint = BWTA.getNearestChokepoint(InformationManager.Instance().getMainBaseLocation(InformationManager.Instance().selfPlayer).getTilePosition());
			Chokepoint firstChokePoint = BWTA.getNearestChokepoint(InformationManager.Instance().getFirstExpansionLocation(InformationManager.Instance().selfPlayer).getPoint());
			
			for (Unit unit : MyBotModule.Broodwar.self().getUnits()) {
				if (unit.getType() == InformationManager.Instance().getBasicCombatUnitType() && unit.isIdle()) {
					commandUtil.attackMove(unit, firstChokePoint.getCenter());
				}
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

				for (BaseLocation baseLocation : InformationManager.Instance().getOccupiedBaseLocations(InformationManager.Instance().enemyPlayer)) {
					double distance = BWTA.getGroundDistance(
						InformationManager.Instance().getMainBaseLocation(InformationManager.Instance().selfPlayer).getTilePosition(), 
						baseLocation.getTilePosition());

					if (distance < closestDistance) {
						closestDistance = distance;
						targetBaseLocation = baseLocation;
					}
				}

				if (targetBaseLocation != null) {
					for (Unit unit : MyBotModule.Broodwar.self().getUnits()) {
						// 건물은 제외
						if (unit.getType().isBuilding()) {
							continue;
						}
						// 모든 일꾼은 제외
						if (unit.getType().isWorker()) {
							continue;
						}
											
						// canAttack 유닛은 attackMove Command 로 공격을 보냅니다
						if (unit.canAttack()) {
							
							if (unit.isIdle()) {
								commandUtil.attackMove(unit, targetBaseLocation.getPosition());
							}
						} 
					}
				}
			}
		}
	}
	
}