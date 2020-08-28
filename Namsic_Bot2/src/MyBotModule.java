/*
+----------------------------------------------------------------------+
| BasicBot                                                             |
+----------------------------------------------------------------------+
| Samsung SDS - 2017 Algorithm Contest                                 |
+----------------------------------------------------------------------+
|                                                                      |
+----------------------------------------------------------------------+
| Author: Tekseon Shin  <tekseon.shin@gmail.com>                       |
| Author: Duckhwan Kim  <duckhwan1982.kim@gmail.com>                   |
+----------------------------------------------------------------------+
*/

/*
+----------------------------------------------------------------------+
| UAlbertaBot                                                          |
+----------------------------------------------------------------------+
| University of Alberta - AIIDE StarCraft Competition                  |
+----------------------------------------------------------------------+
|                                                                      |
+----------------------------------------------------------------------+
| Author: David Churchill <dave.churchill@gmail.com>                   |
+----------------------------------------------------------------------+
*/

import java.util.ArrayList;
import java.util.Arrays;

import bwapi.Color;
import bwapi.DefaultBWListener;
import bwapi.Game;
import bwapi.Mirror;
import bwapi.Player;
import bwapi.Position;
import bwapi.Unit;
import bwapi.UnitType;
import bwapi.Flag.Enum;
import bwta.BWTA;

public class MyBotModule extends DefaultBWListener {

	private Mirror mirror = new Mirror();
	public static Game Broodwar;	
	private GameCommander gameCommander;
	
	private boolean isExceptionLostConditionSatisfied = false;	/// Exception 으로 인한 패배 체크 결과
	private int exceptionLostConditionSatisfiedFrame = 0;		/// Exception 패배 조건이 시작된 프레임 시점
	private int maxDurationForExceptionLostCondition = 20;		/// Exception 패배 조건이 만족된채 게임을 유지시키는 최대 프레임 수
	
	private ArrayList<Integer> timerLimits = new ArrayList<Integer>();			///< 타임 아웃 한계시간 (ms/frame)
	private ArrayList<Integer> timerLimitsBound = new ArrayList<Integer>();		///< 타임 아웃 초과한계횟수
	private ArrayList<Integer> timerLimitsExceeded = new ArrayList<Integer>();	///< 타임 아웃 초과횟수
	private long[] timeStartedAtFrame = new long[100000];		///< 해당 프레임을 시작한 시각
	private long[] timeElapsedAtFrame = new long[100000];		///< 해당 프레임에서 사용한 시간 (ms)
	
	public void run() {
		mirror.getModule().setEventListener(this);
		mirror.startGame();
	}

	/// 경기가 시작될 때 일회적으로 발생하는 이벤트를 처리합니다
	@Override
	public void onStart() {

		Broodwar = mirror.getGame();
		
		gameCommander = new GameCommander();

		if (Broodwar.isReplay()) {
			return;
		}

		initializeLostConditionVariables();

		// Config 파일 관리가 번거롭고, 배포 및 사용시 Config 파일 위치를 지정해주는 것이 번거롭기 때문에, 
		// Config 를 파일로부터 읽어들이지 않고, Config 클래스의 값을 사용하도록 한다.
		if(Config.EnableCompleteMapInformation){
			Broodwar.enableFlag(Enum.CompleteMapInformation.getValue());
		}

		if(Config.EnableUserInput){
			Broodwar.enableFlag(Enum.UserInput.getValue());
		}

		Broodwar.setCommandOptimizationLevel(1);

		// Speedups for automated play, sets the number of milliseconds bwapi spends in each frame
		// Fastest: 42 ms/frame.  1초에 24 frame. 일반적으로 1초에 24frame을 기준 게임속도로 한다
		// Normal: 67 ms/frame. 1초에 15 frame
		// As fast as possible : 0 ms/frame. CPU가 할수있는 가장 빠른 속도. 
		Broodwar.setLocalSpeed(0);
		// frameskip을 늘리면 화면 표시도 업데이트 안하므로 훨씬 빠르다
		Broodwar.setFrameSkip(Config.SetFrameSkip);

		System.out.println("Map analyzing started");
		BWTA.readMap();
		BWTA.analyze();
		BWTA.buildChokeNodes();
		System.out.println("Map analyzing finished");

		gameCommander.onStart();
	}

	///  경기가 종료될 때 일회적으로 발생하는 이벤트를 처리합니다
	@Override
	public void onEnd(boolean isWinner) {
		if (isWinner){
			System.out.println("I won the game");
		} else {
			System.out.println("I lost the game");
		}
		
        System.out.println("Match ended");
        System.exit(0);		
	}

	/// 경기 진행 중 매 프레임마다 발생하는 이벤트를 처리합니다
	@Override
	public void onFrame() {
		if (Broodwar.isReplay()) {
			return;
		}

		// BasicBot 1.1 Patch Start ////////////////////////////////////////////////
		// 타임아웃 패배, 자동 패배 체크 추가

		// timeStartedAtFrame 를 갱신한다
		if (timeStartedAtFrame[Broodwar.getFrameCount()] == 0) {
			timeStartedAtFrame[Broodwar.getFrameCount()] = System.currentTimeMillis();
		}
			
		// 타임아웃 체크 메모리 부족시 증설
		if ((int)timeStartedAtFrame.length < Broodwar.getFrameCount() + 10)
		{
			timeStartedAtFrame = Arrays.copyOf(timeStartedAtFrame, timeStartedAtFrame.length+10000);
			timeElapsedAtFrame = Arrays.copyOf(timeElapsedAtFrame, timeElapsedAtFrame.length+10000);
		}

		// Pause 상태에서는 timeStartedAtFrame 를 계속 갱신해서, timeElapsedAtFrame 이 제대로 계산되도록 한다
		if (Broodwar.isPaused()) {
			timeStartedAtFrame[Broodwar.getFrameCount()] = System.currentTimeMillis();
		}
		else {
			
			try {
				gameCommander.onFrame();
			} 
			catch (Exception e) {
				
				Broodwar.sendText("[Error Stack Trace]");
				System.out.println("[Error Stack Trace]");
				for (StackTraceElement ste : e.getStackTrace()) {
					Broodwar.sendText(ste.toString());
					System.out.println(ste.toString());
				}
				Broodwar.sendText("GG");

				isExceptionLostConditionSatisfied = true;
				
				exceptionLostConditionSatisfiedFrame = Broodwar.getFrameCount();
			}
			
			if (isExceptionLostConditionSatisfied) {
				MyBotModule.Broodwar.drawTextScreen(250, 100, "I lost because of EXCEPTION");

				if (MyBotModule.Broodwar.getFrameCount() - exceptionLostConditionSatisfiedFrame >= maxDurationForExceptionLostCondition) {
					MyBotModule.Broodwar.leaveGame();
				}
			}
	    }
		
		// 화면 출력 및 사용자 입력 처리
		// 빌드서버에서는 Dependency가 없는 빌드서버 전용 UXManager 를 실행시킵니다
		UXManager.Instance().update();

		// BasicBot 1.1 Patch End //////////////////////////////////////////////////
	}

	// BasicBot 1.1 Patch Start ////////////////////////////////////////////////
	// 타임아웃 패배, 자동 패배 체크 추가

	/// 유닛(건물/지상유닛/공중유닛)이 Create 될 때 발생하는 이벤트를 처리합니다
	@Override
	public void onUnitCreate(Unit unit){
		if (!Broodwar.isReplay()) {
			if (timeStartedAtFrame[Broodwar.getFrameCount()] == 0) {
				timeStartedAtFrame[Broodwar.getFrameCount()] = System.currentTimeMillis();
			}
			
			gameCommander.onUnitCreate(unit);
		} 
	}

	///  유닛(건물/지상유닛/공중유닛)이 Destroy 될 때 발생하는 이벤트를 처리합니다
	@Override
	public void onUnitDestroy(Unit unit){
		if (!Broodwar.isReplay()) {
			if (timeStartedAtFrame[Broodwar.getFrameCount()] == 0) {
				timeStartedAtFrame[Broodwar.getFrameCount()] = System.currentTimeMillis();
			}
			
			gameCommander.onUnitDestroy(unit);
		}
	}

	/// 유닛(건물/지상유닛/공중유닛)이 Morph 될 때 발생하는 이벤트를 처리합니다<br>
	/// Zerg 종족의 유닛은 건물 건설이나 지상유닛/공중유닛 생산에서 거의 대부분 Morph 형태로 진행됩니다
	@Override
	public void onUnitMorph(Unit unit){
		if (!Broodwar.isReplay()) {
			if (timeStartedAtFrame[Broodwar.getFrameCount()] == 0) {
				timeStartedAtFrame[Broodwar.getFrameCount()] = System.currentTimeMillis();
			}
			
			gameCommander.onUnitMorph(unit);
		} 
	}
	
	/// 유닛(건물/지상유닛/공중유닛)의 하던 일 (건물 건설, 업그레이드, 지상유닛 훈련 등)이 끝났을 때 발생하는 이벤트를 처리합니다
	@Override
	public void onUnitComplete(Unit unit){
		if (!Broodwar.isReplay()) {
			if (timeStartedAtFrame[Broodwar.getFrameCount()] == 0) {
				timeStartedAtFrame[Broodwar.getFrameCount()] = System.currentTimeMillis();
			}
			
			gameCommander.onUnitComplete(unit);
		}
	}

	/// 유닛(건물/지상유닛/공중유닛)의 소속 플레이어가 바뀔 때 발생하는 이벤트를 처리합니다<br>
	/// Gas Geyser에 어떤 플레이어가 Refinery 건물을 건설했을 때, Refinery 건물이 파괴되었을 때, Protoss 종족 Dark Archon 의 Mind Control 에 의해 소속 플레이어가 바뀔 때 발생합니다
	@Override
	public void onUnitRenegade(Unit unit){
		if (!Broodwar.isReplay()) {
			if (timeStartedAtFrame[Broodwar.getFrameCount()] == 0) {
				timeStartedAtFrame[Broodwar.getFrameCount()] = System.currentTimeMillis();
			}
			
			gameCommander.onUnitRenegade(unit);
		}
	}

	/// 유닛(건물/지상유닛/공중유닛)이 Discover 될 때 발생하는 이벤트를 처리합니다<br>
	/// 아군 유닛이 Create 되었을 때 라든가, 적군 유닛이 Discover 되었을 때 발생합니다
	@Override
	public void onUnitDiscover(Unit unit){
		if (!Broodwar.isReplay()) {
			if (timeStartedAtFrame[Broodwar.getFrameCount()] == 0) {
				timeStartedAtFrame[Broodwar.getFrameCount()] = System.currentTimeMillis();
			}
			
			gameCommander.onUnitDiscover(unit);
		}
	}

	/// 유닛(건물/지상유닛/공중유닛)이 Evade 될 때 발생하는 이벤트를 처리합니다<br>
	/// 유닛이 Destroy 될 때 발생합니다
	@Override
	public void onUnitEvade(Unit unit){
		if (!Broodwar.isReplay()) {
			if (timeStartedAtFrame[Broodwar.getFrameCount()] == 0) {
				timeStartedAtFrame[Broodwar.getFrameCount()] = System.currentTimeMillis();
			}
			
			gameCommander.onUnitEvade(unit);
		}
	}

	/// 유닛(건물/지상유닛/공중유닛)이 Show 될 때 발생하는 이벤트를 처리합니다<br>
	/// 아군 유닛이 Create 되었을 때 라든가, 적군 유닛이 Discover 되었을 때 발생합니다
	@Override
	public void onUnitShow(Unit unit){
		if (!Broodwar.isReplay()) {
			if (timeStartedAtFrame[Broodwar.getFrameCount()] == 0) {
				timeStartedAtFrame[Broodwar.getFrameCount()] = System.currentTimeMillis();
			}
			
			gameCommander.onUnitShow(unit);
		}
	}

	/// 유닛(건물/지상유닛/공중유닛)이 Hide 될 때 발생하는 이벤트를 처리합니다<br>
	/// 보이던 유닛이 Hide 될 때 발생합니다
	@Override
	public void onUnitHide(Unit unit){
		if (!Broodwar.isReplay()) {
			if (timeStartedAtFrame[Broodwar.getFrameCount()] == 0) {
				timeStartedAtFrame[Broodwar.getFrameCount()] = System.currentTimeMillis();
			}
			
			gameCommander.onUnitHide(unit);
		}
	}

	/// 핵미사일 발사가 감지되었을 때 발생하는 이벤트를 처리합니다
	@Override
	public void onNukeDetect(Position target){
		if (!Broodwar.isReplay()) {
			if (timeStartedAtFrame[Broodwar.getFrameCount()] == 0) {
				timeStartedAtFrame[Broodwar.getFrameCount()] = System.currentTimeMillis();
			}

			// 빌드서버에서는 향후 적용
			gameCommander.onNukeDetect(target);
		}
	}

	/// 다른 플레이어가 대결을 나갔을 때 발생하는 이벤트를 처리합니다
	@Override
	public void onPlayerLeft(Player player){
		if (!Broodwar.isReplay()) {
			if (timeStartedAtFrame[Broodwar.getFrameCount()] == 0) {
				timeStartedAtFrame[Broodwar.getFrameCount()] = System.currentTimeMillis();
			}
			
			// 빌드서버에서는 향후 적용
			gameCommander.onPlayerLeft(player);
		}
	}

	/// 게임을 저장할 때 발생하는 이벤트를 처리합니다
	@Override
	public void onSaveGame(String gameName){
		if (!Broodwar.isReplay()) {
			if (timeStartedAtFrame[Broodwar.getFrameCount()] == 0) {
				timeStartedAtFrame[Broodwar.getFrameCount()] = System.currentTimeMillis();
			}

			// 빌드서버에서는 향후 적용
			gameCommander.onSaveGame(gameName);
		}
	}
	

	/// 텍스트를 입력 후 엔터를 하여 다른 플레이어들에게 텍스트를 전달하려 할 때 발생하는 이벤트를 처리합니다
	@Override
	public void onSendText(String text){		
		
		if (timeStartedAtFrame[Broodwar.getFrameCount()] == 0) {
			timeStartedAtFrame[Broodwar.getFrameCount()] = System.currentTimeMillis();
		}
		
		gameCommander.onSendText(text);

		// Display the text to the game
		Broodwar.sendText(text);
	}

	/// 다른 플레이어로부터 텍스트를 전달받았을 때 발생하는 이벤트를 처리합니다
	@Override
	public void onReceiveText(Player player, String text){
		if (timeStartedAtFrame[Broodwar.getFrameCount()] == 0) {
			timeStartedAtFrame[Broodwar.getFrameCount()] = System.currentTimeMillis();
		}
		
		Broodwar.printf(player.getName() + " said \"" + text + "\"");

		gameCommander.onReceiveText(player, text);
	}
	
	private void initializeLostConditionVariables(){
		
		timerLimits.add(55);
		timerLimitsBound.add(320);
		timerLimitsExceeded.add(0);

		timerLimits.add(1000);
		timerLimitsBound.add(10);
		timerLimitsExceeded.add(0);

		timerLimits.add(10000);
		timerLimitsBound.add(2);
		timerLimitsExceeded.add(0);
	}
	
}