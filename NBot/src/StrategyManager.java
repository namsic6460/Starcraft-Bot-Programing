import java.util.ArrayList;
import java.util.List;

import bwapi.Player;
import bwapi.Race;
import bwapi.Unit;
import bwapi.UnitType;
import bwapi.UpgradeType;
import bwta.BWTA;
import bwta.BaseLocation;
import bwta.Chokepoint;

public class StrategyManager {

	private static StrategyManager instance = new StrategyManager();

	private CommandUtil commandUtil = new CommandUtil();
	
	private int createdCarrier;
	private boolean isFullScaleAttackStarted;

	public int upgradePlasma;
	public ArrayList<Unit> notConstructed;

	// BasicBot 1.1 Patch End //////////////////////////////////////////////////

	/// static singleton 객체를 리턴합니다
	public static StrategyManager Instance() {
		return instance;
	}

	public StrategyManager() {
		createdCarrier = 0;
		upgradePlasma = 0;
		isFullScaleAttackStarted = false;
		notConstructed = new ArrayList<>();
	}

	/// 경기가 시작될 때 일회적으로 전략 초기 세팅 관련 로직을 실행합니다
	public void onStart() {
//		MyBotModule.Broodwar.sendText("show me the money");
//		MyBotModule.Broodwar.sendText("operation cwal");
//		MyBotModule.Broodwar.sendText("black sheep wall");
//		BuildManager.Instance().buildQueue.queueAsLowestPriority(
//				InformationManager.Instance().getBasicSupplyProviderUnitType(),
//				BuildOrderItem.SeedPositionStrategy.FirstChokePoint, true);
//		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Gateway,
//				BuildOrderItem.SeedPositionStrategy.MainBaseLocation);
//		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Cybernetics_Core,
//				BuildOrderItem.SeedPositionStrategy.MainBaseLocation);
//		BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Dragoon);
		
		setInitialBuildOrder();
	}

	/// 경기 진행 중 매 프레임마다 경기 전략 관련 로직을 실행합니다
	public void update() {
		List<Unit> units = MyBotModule.Broodwar.self().getUnits();
		List<Unit> enemies = MyBotModule.Broodwar.enemy().getUnits();

		executeCombat(units, enemies);
		upgradePlasma();
		trainInterceptor(units);
		checkPylon(units);
		
//		dragoonMoving(GameCommander.dragoons, enemies);
	}

	public void setInitialBuildOrder() {
		if (MyBotModule.Broodwar.self().getRace() == Race.Protoss) {
			BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
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
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Forge,
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Photon_Cannon,
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Photon_Cannon,
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(
					InformationManager.Instance().getBasicSupplyProviderUnitType(),
					BuildOrderItem.SeedPositionStrategy.SecondChokePoint, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Photon_Cannon,
					BuildOrderItem.SeedPositionStrategy.SecondChokePoint);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Gateway,
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Photon_Cannon,
					BuildOrderItem.SeedPositionStrategy.SecondChokePoint);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Cybernetics_Core,
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Photon_Cannon,
					BuildOrderItem.SeedPositionStrategy.SecondChokePoint);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Assimilator,
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Gateway,
					BuildOrderItem.SeedPositionStrategy.FirstChokePoint);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Dragoon);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Nexus,
					BuildOrderItem.SeedPositionStrategy.FirstExpansionLocation);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(
					InformationManager.Instance().getBasicSupplyProviderUnitType(),
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);

			BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.Singularity_Charge);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Dragoon);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Dragoon);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Photon_Cannon,
					BuildOrderItem.SeedPositionStrategy.SecondChokePoint);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Stargate,
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Forge,
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Gateway,
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
					BuildOrderItem.SeedPositionStrategy.FirstExpansionLocation, true);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
					BuildOrderItem.SeedPositionStrategy.FirstExpansionLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Dragoon);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.Protoss_Ground_Armor);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
					BuildOrderItem.SeedPositionStrategy.FirstExpansionLocation, true);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
					BuildOrderItem.SeedPositionStrategy.FirstExpansionLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Assimilator,
					BuildOrderItem.SeedPositionStrategy.FirstExpansionLocation);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Dragoon);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(
					InformationManager.Instance().getBasicSupplyProviderUnitType(),
					BuildOrderItem.SeedPositionStrategy.FirstChokePoint, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(
					InformationManager.Instance().getBasicSupplyProviderUnitType(),
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
					BuildOrderItem.SeedPositionStrategy.FirstExpansionLocation, true);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
					BuildOrderItem.SeedPositionStrategy.FirstExpansionLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Dragoon);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
					BuildOrderItem.SeedPositionStrategy.FirstExpansionLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Dragoon);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
					BuildOrderItem.SeedPositionStrategy.FirstExpansionLocation, true);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
					BuildOrderItem.SeedPositionStrategy.FirstExpansionLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Dragoon);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(
					InformationManager.Instance().getBasicSupplyProviderUnitType(),
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Stargate,
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
					BuildOrderItem.SeedPositionStrategy.FirstExpansionLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.Protoss_Air_Weapons);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Dragoon);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
					BuildOrderItem.SeedPositionStrategy.FirstExpansionLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Fleet_Beacon);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
					BuildOrderItem.SeedPositionStrategy.FirstExpansionLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(
					InformationManager.Instance().getBasicSupplyProviderUnitType(),
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(
					InformationManager.Instance().getBasicSupplyProviderUnitType(),
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
					BuildOrderItem.SeedPositionStrategy.FirstExpansionLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
					BuildOrderItem.SeedPositionStrategy.FirstExpansionLocation, true);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
					BuildOrderItem.SeedPositionStrategy.FirstExpansionLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(
					InformationManager.Instance().getBasicSupplyProviderUnitType(),
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(
					InformationManager.Instance().getBasicSupplyProviderUnitType(),
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.Carrier_Capacity);
			
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
					BuildOrderItem.SeedPositionStrategy.FirstExpansionLocation, true);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
					BuildOrderItem.SeedPositionStrategy.FirstExpansionLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(
					InformationManager.Instance().getBasicSupplyProviderUnitType(),
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(
					InformationManager.Instance().getBasicSupplyProviderUnitType(),
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Stargate,
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Carrier);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(InformationManager.Instance().getWorkerType(),
					BuildOrderItem.SeedPositionStrategy.FirstExpansionLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Carrier);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(
					InformationManager.Instance().getBasicSupplyProviderUnitType(),
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(
					InformationManager.Instance().getBasicSupplyProviderUnitType(),
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Carrier);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(
					InformationManager.Instance().getBasicSupplyProviderUnitType(),
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Stargate,
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.Protoss_Air_Armor);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(
					InformationManager.Instance().getBasicSupplyProviderUnitType(),
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Carrier);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(
					InformationManager.Instance().getBasicSupplyProviderUnitType(),
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Carrier);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Carrier);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(
					InformationManager.Instance().getBasicSupplyProviderUnitType(),
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);

			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);

			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Carrier);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Carrier);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(
					InformationManager.Instance().getBasicSupplyProviderUnitType(),
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation, true);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(
					InformationManager.Instance().getBasicSupplyProviderUnitType(),
					BuildOrderItem.SeedPositionStrategy.FirstExpansionLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Carrier);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Carrier);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(
					InformationManager.Instance().getBasicSupplyProviderUnitType(),
					BuildOrderItem.SeedPositionStrategy.FirstExpansionLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Carrier);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(
					InformationManager.Instance().getBasicSupplyProviderUnitType(),
					BuildOrderItem.SeedPositionStrategy.FirstExpansionLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Carrier);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(
					InformationManager.Instance().getBasicSupplyProviderUnitType(),
					BuildOrderItem.SeedPositionStrategy.FirstExpansionLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Carrier);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(
					InformationManager.Instance().getBasicSupplyProviderUnitType(),
					BuildOrderItem.SeedPositionStrategy.FirstExpansionLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Carrier);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(
					InformationManager.Instance().getBasicSupplyProviderUnitType(),
					BuildOrderItem.SeedPositionStrategy.FirstExpansionLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Carrier);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(
					InformationManager.Instance().getBasicSupplyProviderUnitType(),
					BuildOrderItem.SeedPositionStrategy.FirstExpansionLocation, true);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);
			
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Carrier);
			/*
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Assimilator,
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation);

			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Forge,
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Photon_Cannon,
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation);

			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Gateway,
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Zealot);

			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Cybernetics_Core,
					BuildOrderItem.SeedPositionStrategy.MainBaseLocation);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Dragoon);
			// 드라군 사정거리 업그레이드
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.Singularity_Charge);

			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Citadel_of_Adun);
			// 질럿 속도 업그레이드
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.Leg_Enhancements);

			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Shield_Battery);

			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Templar_Archives);
			// 하이템플러
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_High_Templar);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_High_Templar);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(TechType.Psionic_Storm);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(TechType.Hallucination);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.Khaydarin_Amulet);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Archon);

			// 다크아칸
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Dark_Templar);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Dark_Templar);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(TechType.Maelstrom);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(TechType.Mind_Control);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.Argus_Talisman);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Dark_Archon);

			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Robotics_Facility);

			// 셔틀
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Shuttle);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Robotics_Support_Bay);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.Gravitic_Drive);

			// 리버
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Reaver);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.Scarab_Damage);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.Reaver_Capacity);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Scarab);

			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Observatory);
			// 옵저버
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Observer);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.Gravitic_Boosters);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.Sensor_Array);

			// 공중유닛
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Stargate);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Fleet_Beacon);

			// 스카우트
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Scout);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.Apial_Sensors);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.Gravitic_Thrusters);

			// 커세어
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Corsair);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(TechType.Disruption_Web);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.Argus_Jewel);

			// 캐리어
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Carrier);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.Carrier_Capacity);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Interceptor);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Interceptor);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Interceptor);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Interceptor);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Interceptor);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Interceptor);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Interceptor);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Interceptor);

			// 아비터
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Arbiter_Tribunal);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UnitType.Protoss_Arbiter);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(TechType.Recall);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(TechType.Stasis_Field);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.Khaydarin_Core);

			// 포지 - 지상 유닛 업그레이드
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.Protoss_Ground_Weapons);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.Protoss_Plasma_Shields);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.Protoss_Ground_Armor);

			// 사이버네틱스코어 - 공중 유닛 업그레이드
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.Protoss_Air_Weapons);
			BuildManager.Instance().buildQueue.queueAsLowestPriority(UpgradeType.Protoss_Air_Armor);

			*/
		} 
	}

//	public void executeCombat(List<Unit> units, List<Unit> enemies) {
//		// 공격 모드가 아닐 때에는 전투유닛들을 아군 진영 길목에 집결시켜서 방어
//		if (isFullScaleAttackStarted == false) {
//			Chokepoint chokePoint = null;
//			if (chokePointType == 1)
//				chokePoint = BWTA.getNearestChokepoint(InformationManager.Instance()
//						.getMainBaseLocation(InformationManager.Instance().selfPlayer).getTilePosition());
//			else if (chokePointType == 2)
//				chokePoint = BWTA.getNearestChokepoint(InformationManager.Instance()
//						.getSecondChokePoint(InformationManager.Instance().selfPlayer).getPoint());
//			for (Unit unit : units) {
//				if (unit.canAttack() && !unit.isAttacking() && !unit.getType().isWorker() && !unit.getType().isBuilding()
//						&& !moving.containsKey(unit) && !unit.isMoving() && unit.isIdle())	
//					commandUtil.attackMove(unit, chokePoint.getCenter());
//			}
//		}
//		// 공격 모드가 되면, 모든 전투유닛들을 적군 Main BaseLocation 로 공격 가도록 합니다
//		else {
//			if (InformationManager.Instance().enemyPlayer != null
//					&& InformationManager.Instance().enemyRace != Race.Unknown && InformationManager.Instance()
//							.getOccupiedBaseLocations(InformationManager.Instance().enemyPlayer).size() > 0) {
//				// 공격 대상 지역 결정
//				BaseLocation targetBaseLocation = null;
//				double closestDistance = 100000000;
//
//				for (BaseLocation baseLocation : InformationManager.Instance()
//						.getOccupiedBaseLocations(InformationManager.Instance().enemyPlayer)) {
//					double distance = BWTA.getGroundDistance(InformationManager.Instance()
//							.getMainBaseLocation(InformationManager.Instance().selfPlayer).getTilePosition(),
//							baseLocation.getTilePosition());
//
//					if (distance < closestDistance) {
//						closestDistance = distance;
//						targetBaseLocation = baseLocation;
//					}
//				}
//
//				if (targetBaseLocation != null) {
//					for (Unit unit : MyBotModule.Broodwar.self().getUnits()) {
//						// 건물은 제외
//						if (unit.getType().isBuilding()) {
//							continue;
//						}
//						// 모든 일꾼은 제외
//						if (unit.getType().isWorker()) {
//							continue;
//						}
//
//						// canAttack 유닛은 attackMove Command 로 공격을 보냅니다
//						if (unit.canAttack()) {
//
//							if (unit.isIdle()) {
//								commandUtil.attackMove(unit, targetBaseLocation.getPosition());
//							}
//						}
//					}
//				}
//			}
//		}
//	}
	
	private void upgradePlasma() {
		if(MyBotModule.Broodwar.self().getUpgradeLevel(UpgradeType.Protoss_Ground_Armor) == 0)
			return;
		
		if(upgradePlasma == 1) {
			if(GameCommander.firstForge != null && GameCommander.firstForge.canUpgrade(UpgradeType.Protoss_Plasma_Shields)) {
				GameCommander.firstForge.upgrade(UpgradeType.Protoss_Plasma_Shields);
				upgradePlasma = 2;
			}
			
			else if (GameCommander.secondForge != null
					&& GameCommander.secondForge.canUpgrade(UpgradeType.Protoss_Plasma_Shields)) {
				GameCommander.secondForge.upgrade(UpgradeType.Protoss_Plasma_Shields);
				upgradePlasma = 2;
			}
		}
	}
	
	private void trainInterceptor(List<Unit> units) {
		for(Unit unit : units) {
			if(unit.getType() == UnitType.Protoss_Carrier) {
				if(unit.canTrain(UnitType.Protoss_Interceptor))
					unit.train(UnitType.Protoss_Interceptor);
			}
		}
	}
	
	private void checkPylon(List<Unit> units) {
		Player self = MyBotModule.Broodwar.self();
		int used = self.supplyUsed();
		int total = self.supplyTotal();
		
		UnitType highestUnit = BuildManager.Instance().buildQueue.getHighestPriorityItem().metaType.getUnitType();
		if(highestUnit == null || highestUnit == UnitType.Protoss_Pylon)
			return;
		
		used += highestUnit.supplyRequired();
		total += highestUnit.supplyProvided();
		
		for(Unit unit : units) {
			if(unit == null)
				continue;
			
			if(unit.isBeingConstructed()) {
				used += unit.getType().supplyRequired();
				total += unit.getType().supplyProvided();
			}
			
			if(unit.getType() == UnitType.Protoss_Probe) {
				UnitType build = unit.getBuildType();
				
				if (build != UnitType.None) {
					used += build.supplyRequired();
					total += build.supplyProvided();
				}
			}
		}
		
		if(total < used) 
			BuildManager.Instance().buildQueue.queueAsHighestPriority(UnitType.Protoss_Pylon, false);
		else if(highestUnit == UnitType.Protoss_Pylon && total >= used + 4) 
			BuildManager.Instance().buildQueue.removeHighestPriorityItem();
	}
	
	private Unit getNearestUnit(Unit unit, List<Unit> enemies) {
		Unit nearestEnemy = null;
		double minDistance = 9999999;
		for(Unit enemy : enemies) {
			if(enemy.isInvincible() || !enemy.isVisible() || enemy.isBurrowed())
				continue;
			
			int distance = unit.getDistance(enemy);
			
			if(distance < minDistance) {
				distance = unit.getDistance(enemy);
				nearestEnemy = enemy;
			}
		}
		
		if(nearestEnemy != null)
			System.out.println(unit.getType().toString() + " -> " + nearestEnemy.getID() + "(" + nearestEnemy.getType().toString() + ") : " + nearestEnemy.getX() + ", " + nearestEnemy.getY());
		return nearestEnemy;
	}
	
	private void executeCombat(List<Unit> units, List<Unit> enemies) {
		if(!isFullScaleAttackStarted) {
			Chokepoint chokePoint = BWTA.getNearestChokepoint(InformationManager.Instance()
					.getSecondChokePoint(InformationManager.Instance().selfPlayer).getPoint());
			
			ArrayList<Unit> removeList = new ArrayList<>();
			for(Unit unit : notConstructed) {
				if(!unit.isBeingConstructed()) {
					commandUtil.attackMove(unit, chokePoint.getCenter());
					removeList.add(unit);
					
					if(unit.getType() == UnitType.Protoss_Carrier)
						createdCarrier++;
				}
			}
			
			for(Unit unit : removeList)
				notConstructed.remove(unit);
			
			if(createdCarrier == 4)
				isFullScaleAttackStarted = true;
		}
		
		else if(InformationManager.Instance().getMainBaseLocation(MyBotModule.Broodwar.enemy()) != null) {
			BaseLocation base = InformationManager.Instance().getMainBaseLocation(MyBotModule.Broodwar.enemy());
			for(Unit unit : units) {
				if(!unit.canAttack() || unit.isAttacking() || unit.getType().isWorker() || unit.getType().isBuilding())
					continue;
				
				if(!unit.isAttacking() && !unit.isMoving() && unit.isIdle()) {
					Unit enemy = getNearestUnit(unit, enemies);
					
					if(enemy != null)
						commandUtil.attackUnit(unit, enemy);
				}
				
				else if(unit.getType() == UnitType.Protoss_Carrier) {
					if(unit.getInterceptorCount() >= 5)
						commandUtil.attackMove(unit, base.getPoint());
				}
				
				else
					commandUtil.attackMove(unit, base.getPoint());
			}
		}
	}
	
}