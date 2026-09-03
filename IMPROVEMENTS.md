# ChzzkAbility → CasterAbility 이식 메모

ChzzkAbility(Skript)의 게임 루프를 CasterAbility(Kotlin 플러그인)로 옮기면서
원본에서 발견한 문제와, 플러그인 쪽에서 고친 것을 적어둔 문서입니다.

능력 자체는 플러그인에 하드코딩하지 않습니다. **Skript로 정의합니다.**
플러그인은 게임 진행(추첨 · 월드 · 무적 · 월드보더 · 쿨타임)만 담당하고,
능력의 내용은 스크립트가 씁니다.

검증 범위는 `mvn clean package` 통과까지입니다. 서버 실플레이 검증은 하지 않았습니다.

---

## 1. 플러그인 쪽에서 고친 것

### 1.1 DSL이 파싱되지 않던 문제 (가장 중요)

`ability class "..."` 와 `ability "..."` 는 들여쓴 블록을 받습니다.
그런데 두 구문이 `Skript.registerEffect`로 등록되어 있었습니다.

Skript는 SectionNode(콜론 + 들여쓴 블록)를 Effect로 파싱하지 않습니다.
Effect는 한 줄짜리 SimpleNode에서만 매칭됩니다. 즉 스크립트에 이렇게 쓰면
"can't understand this section" 류의 오류가 났습니다.

`Skript.registerSection` + `Section`으로 바꿨습니다.

| 이전 | 지금 |
|---|---|
| `skript/effect/EffAbilityClassDef.kt` | `skript/section/SecAbilityClass.kt` |
| `skript/effect/EffAbilityDef.kt` | `skript/section/SecAbility.kt` |

부수적으로:
- `Section.init()`은 SectionNode를 인자로 받으므로 `SkriptLogger.getNode()`를
  캐스팅하던 코드가 사라졌습니다.
- 자식 노드가 EntryNode든 SimpleNode든 읽도록 `SectionEntries.kt`를 뒀습니다.
- tier 범위, trigger 이름, item 이름, cooldown 값이 잘못되면 **파싱 단계에서**
  오류를 냅니다. 이전에는 조용히 기본값으로 넘어갔습니다.

### 1.2 이벤트 값이 없어 능력을 쓸 수 없던 문제

`on ability use "..."` 안에서 대상과 피해량을 읽을 방법이 없었습니다.
(`EventValues.registerEventValue` 호출이 한 군데도 없었습니다.)

`SkriptRegistry.registerEventValues()`에서 등록했습니다.

| 스크립트에서 | 값 |
|---|---|
| `player` | 능력을 쓴 플레이어 (PlayerEvent 상속으로 원래 가능) |
| `event-entity` | 대상. on_hit이면 맞은 쪽, on_damaged면 때린 쪽 |
| `event-number` | 피해량 |
| `event-string` | 발동한 능력 id |

읽기만 가능한 값이라 피해량을 **바꾸려면** 별도 구문이 필요합니다.
`ExprAbilityDamage`(`ability damage`)를 추가했습니다. set이 됩니다.
여기서 바꾼 값은 `AbilityDispatcher`가 원래 Bukkit 이벤트에 반영합니다.

### 1.3 cancel event 를 해도 쿨타임이 걸리던 문제

`AbilityDispatcher`가 이벤트를 쏜 직후 취소 여부를 보지 않고 쿨타임을 걸었습니다.
조건을 못 맞춰 스크립트가 거부해도 쿨타임만 날아갔습니다.
`isCancelled`를 확인하도록 고쳤습니다. 이제 스크립트에서 이렇게 쓸 수 있습니다.

    on ability use "menhera_selfharm":
        if player's health <= 5:
            send "&c체력이 부족합니다." to player
            cancel event
            stop

### 1.4 스케줄러

`GameScheduler.kt`가 주기 작업을 한 곳에서 관리하고, 태스크를 들고 있다가
`stop()`에서 전부 취소합니다.

| 작업 | 주기 | 이유 |
|---|---|---|
| 이펙트 감소 | 2틱 | 남은 시간을 0.1초씩 깎음 |
| 쿨타임 감소 | 1틱 | `AbilityRegistry`가 **틱** 단위로 저장하고 호출당 1씩 깎음 |
| 액션바 갱신 | 2틱 | 한 곳에서만 |

쿨타임을 20틱 주기로 돌리면 쿨타임이 20배가 됩니다.
`AbilityDispatcher`가 `cooldownSeconds * 20`(틱)을 넣기 때문입니다.

`stopGame()`은 **스케줄러를 먼저 멈춘 뒤** 액션바를 지웁니다.
순서가 반대면 지운 직후 UI 태스크가 다시 그립니다.

---

## 2. ChzzkAbility 스크립트에서 발견한 문제와 이식 결과

| 원본 | 이식처 | 원본의 문제 |
|---|---|---|
| `ability/core/event.sk` | `game/PlayerStateManager.kt` | `addPlayerVar`가 `add ... to {...::*}` 라 덧셈이 아니라 리스트 적재였음 |
| `ability/core/scheduler.sk` | `game/GameScheduler.kt` | `every 2 ticks`와 `every 20 ticks`가 똑같이 `caUI()`를 호출해 UI 이중 갱신. `#여기다가 쿨타임 감소 넣기#` 미완성 |
| `game/UI.sk` | `game/GameUIRenderer.kt` | 항목마다 문자열 전체를 다시 만듦 |
| `game/Invincibility.sk` | `game/GameManager.kt` | 무적 동안 매초 `hide` 재호출. 안 쓰는 `{ca.invincibility}` 설정 |
| `game/teleport.sk`, `game/World.sk` | `game/SpawnLocator.kt` | 재시도할 때마다 직전 좌표 기준으로 다시 뽑아 중심에서 계속 멀어짐. `{_loop}` 미초기화 |
| `game/border.sk` | `game/WorldBorderController.kt` | 수축량을 두 칸 전 값에 누적(`{_shrink::n-2}`)해 단계별 수축량이 들쭉날쭉하고 최종 반지름이 min과 안 맞음. 다음 중앙을 보더 밖에서 뽑을 수 있었음 |
| `game/Game.sk` | `game/GameManager.kt` | 없는 키 `worldborder::max_size`를 읽음 (실제 키는 `max_radius`) |
| `config/Config.sk` | `resources/config.yml` + `config/GameConfig.kt` | 기본값 정의뿐이라 그대로 대응됨 |
| `gui/Inventory.sk` | `gui/GuiManager.kt` | 범용 프레임 헬퍼. 플러그인 인벤토리 API로 대체됨 |
| `ability/core/loader.sk` | (불필요) | `if` 없이 `{...} is not true` 한 줄을 써서 중복 등록을 못 막음. 디버그용 `broadcast` 잔재. YAML 기반 능력 등록은 Skript DSL이 대신함 |

### SpawnLocator
바다 · 강 · 사막을 피해 지점을 고릅니다. 후보를 항상 중심 기준으로 다시 뽑고,
정해진 횟수 안에 못 찾으면 마지막 후보를 그대로 씁니다(원본은 아무것도
돌려주지 않는 경로가 있었습니다). 텔레포트 전에 청크를 불러옵니다.

### WorldBorderController
코사인 이징(처음과 끝은 천천히, 중간은 빠르게)이라는 원본 의도는 유지하고,
각 단계의 목표 반지름을 직접 계산해 그 차이를 수축량으로 씁니다.
`count` 단계를 마치면 정확히 `min`이 됩니다.
랜덤 중앙은 수축 후 반지름 안쪽에서만 뽑습니다.
대기 → 수축 두 구간의 보스바를 각각 관리하고 `stop()`에서 함께 지웁니다.

---

## 3. 기본 능력(classic) 자동 설치

`util/ClassicAbilities.kt`, JAR 리소스 `resources/classic/`

처음 켜면 능력 `.sk` 파일을 Skript의 스크립트 폴더에 깝니다.

    plugins/Skript/scripts/CasterAbility/classic/yandere.sk
    plugins/Skript/scripts/CasterAbility/classic/menhera.sk

Skript는 자기 scripts 폴더 아래만 읽기 때문에, 설치하자마자 기본 능력으로
잡히려면 여기여야 합니다. Skript를 못 찾으면 `plugins/CasterAbility/classic/`
에 꺼내두고 옮기라고 경고를 남깁니다.

설치 판단은 **classic 폴더의 존재 여부**로 합니다. 파일 단위가 아닙니다.

- 폴더가 없으면 처음 설치로 보고 전부 씁니다.
- 폴더가 있으면 손대지 않습니다. 그래서 능력 파일 하나를 지워도 재시작할 때
  되살아나지 않습니다.
- 되돌리려면 폴더째 지우고 재시작합니다.
- `config.yml` 의 `classic.install: false` 로 끌 수 있습니다.

문법 문서 `SYNTAX.txt` 는 스크립트가 아니므로 플러그인 폴더에 둡니다.

> 파일을 쓰는 시점이 Skript의 스크립트 로딩보다 늦을 수 있어서, 처음 설치한
> 직후에는 `/sk reload all` 이나 재시작이 필요하다고 로그에 남깁니다.

> 이 두 .sk 파일은 문법을 맞춰 작성했지만 **서버에서 실행해 검증하지는
> 않았습니다.** 특히 `on heal of player:` + `heal reason is satiated or regen`
> 부분은 Skript 버전에 따라 다를 수 있습니다.

---

## 4. 남은 것

- 스크립트에서 능력을 지우고 `/sk reload all` 만 하면 지운 능력이 그대로 남습니다.
  정의를 비우는 `AbilityRegistry.clearDefinitions()`는 `/ca reload`에서만
  호출되므로, 순서대로 `/ca reload` → `/sk reload all` 을 해야 합니다.
  Skript 리로드를 직접 감지해 정의를 비우면 이 순서를 신경 쓰지 않아도 됩니다.
- `game/bossbar.sk`의 `caTimerBossbar` 세부 옵션 중 일부는
  `BossBarManager.startTimer`와 표시 형식이 다릅니다.
- 서버 실플레이 검증.
