# ChzzkAbility → CasterAbility 이식 메모

ChzzkAbility(Skript)의 게임 루프를 CasterAbility(Kotlin)로 옮기면서
원본에서 발견한 문제를 정리하고, 어떻게 대체했는지 적어둔 문서입니다.

`mvn clean package` 통과 기준으로 작성되었습니다.

---

## 1. ChzzkAbility에서 발견한 문제

### 1.1 `ability/core/loader.sk` — 검사만 하고 아무 일도 안 하는 줄
```skript
function caDescription(description: text):
    {ca.loaded.ability::%{ca.ability.load}%} is not true   # 조건문이 아님
    broadcast {ca.loaded.ability::%{ca.ability.load}%}     # 디버그 잔재
```
조건 검사처럼 보이지만 `if` 없이 쓰여 중복 등록을 막지 못하고,
`broadcast`가 남아 리로드 때마다 콘솔에 값이 찍힙니다.

### 1.2 `ability/core/scheduler.sk` — UI 이중 갱신
`every 2 ticks`와 `every 20 ticks` 두 블록이 똑같이 `caUI({_p})`를 호출합니다.
1초에 한 번은 같은 액션바를 두 번 그립니다.

### 1.3 `ability/core/event.sk` — `addPlayerVar`가 덧셈이 아님
```skript
function addPlayerVar(player: player, name: text, value: number):
    add {_value} to {ca.player::%{_player}%::%{_name}%::*}
```
`::*` 때문에 숫자를 더하는 게 아니라 리스트에 원소가 쌓입니다.
`getPlayerVar`는 리스트가 아닌 단일 변수를 읽으므로 값이 영영 안 보입니다.

### 1.4 `game/UI.sk` — 문자열을 매 항목마다 새로 만듦
```skript
set {_msg} to "%{_msg}% &7| %loop-value%"
```
항목 수만큼 문자열 전체를 다시 만듭니다. 2틱마다 전 인원에 대해 돌아갑니다.

### 1.5 `game/Invincibility.sk` — 무적 동안 매초 `hide` 재호출
```skript
while {_time} > 0:
    wait 1 second
    hide({ca.game.players::*})
```
이미 숨긴 대상을 1초마다 다시 숨깁니다.
그리고 루프가 끝난 뒤 `set {ca.invincibility} to false`를 하는데,
이 변수는 다른 어디에서도 읽지 않습니다.

### 1.6 `game/Game.sk` — 없는 설정 키를 읽음
```skript
set {_size} to {ca.config.worldborder::max_size}
```
설정에 있는 키는 `max_radius`입니다. `{_size}`는 계속 비어 있게 됩니다.

### 1.7 `ability/core/scheduler.sk` — 미완성 표시
```skript
#여기다가 쿨타임 감소 넣기#
```

---

## 2. CasterAbility 쪽 대응

| ChzzkAbility | CasterAbility |
|---|---|
| `{ca.player::*}` / `{ca.effect::*}` / `{ca.ui.custom::*}` | `game/PlayerStateManager.kt` |
| `scheduler.sk`의 주기 블록 | `game/GameScheduler.kt` |
| `UI.sk`의 `caUI()` | `game/GameUIRenderer.kt` |
| `Yandere.sk` / `menhera.sk` | `ability/examples/*.kt` |

### PlayerStateManager
- 능력 변수 · 이펙트 남은 시간 · 커스텀 UI를 UUID 키로 보관합니다.
- `addPlayerVar()`는 실제 덧셈을 하고 결과를 돌려줍니다 (1.3 대응).
- 다른 플레이어를 값으로 저장할 때는 `setPlayerRefVar()`가 `Player`가 아닌
  `UUID`를 저장합니다. `Player` 객체를 맵에 붙들면 퇴장한 플레이어가
  GC되지 않습니다.
- `tickEffects()`는 비게 된 플레이어 맵을 같이 지웁니다.

### GameScheduler
주기가 서로 다른 세 작업을 각자 맞는 주기로 돌리고, 태스크를 필드로 들고
있다가 `stop()`에서 전부 취소합니다.

| 작업 | 주기 | 이유 |
|---|---|---|
| 이펙트 감소 | 2틱 | 남은 시간을 0.1초씩 깎음 |
| 쿨타임 감소 | 1틱 | `AbilityRegistry`가 **틱** 단위로 저장하고 호출당 1씩 깎음 |
| 액션바 갱신 | 2틱 | 한 곳에서만 (1.2 대응) |

> 쿨타임을 20틱 주기로 돌리면 쿨타임이 20배로 늘어납니다.
> `AbilityDispatcher`가 `setCooldown(p, id, cooldownSeconds * 20)`으로
> 틱 값을 넣기 때문입니다.

### GameUIRenderer
- `StringBuilder`로 한 번에 조립합니다 (1.4 대응).
- 표시할 게 없어진 순간에만 액션바를 한 번 비우고, 그 뒤로는 매 틱
  빈 문자열을 보내지 않습니다.

### GameManager 변경점
- `startCooldownTicker()` → `GameScheduler.start()` 한 줄로 위임.
  대상 플레이어는 람다로 넘겨서 `IN_GAME`이 아닐 때 빈 리스트가 됩니다.
- `stopGame()`에서 **스케줄러를 먼저 멈춘 뒤** 액션바를 지웁니다.
  순서가 반대면 지운 직후 UI 태스크가 다시 그립니다.
- 무적 시간은 시작할 때 한 번 투명화, 끝날 때 한 번 해제입니다 (1.5 대응).

---

## 3. 예제 능력

`ability/examples/`의 두 파일은 이 플러그인의 실제 구조를 따릅니다.

- `register()`가 `AbilityClass` / `AbilityDefinition`을 `AbilityRegistry`에 등록
- 동작은 `AbilityDispatcher`가 쏘는 `AbilityUseEvent`를 `Listener`로 받아 처리

### YandereAbility (얀데레, tier 2)
| 능력 | 트리거 | 쿨타임 |
|---|---|---|
| `yandere_obsession` | `ON_HIT` | – |
| `yandere_devotion` | `ON_DAMAGED` | – |
| `yandere_jealousy` | `RIGHT_CLICK` (블레이즈 막대) | 45초 |

집착 대상은 `UUID`로 저장합니다. 질투 스킬이 참조하는
"집착 대상이 마지막으로 때린 상대"는 `EntityDamageByEntityEvent`를
`MONITOR`로 받아 따로 기록합니다 — `AbilityDispatcher`의 `ON_HIT`은
공격자 본인의 능력만 발동시키므로 남의 공격은 그 경로로 볼 수 없습니다.

### MenheraAbility (멘헤라, tier 3)
| 능력 | 트리거 | 쿨타임 |
|---|---|---|
| `menhera_love` | `ON_DAMAGED` | – |
| `menhera_selfharm` | `RIGHT_CLICK` (블레이즈 막대) | 50초 |

자연 회복 차단은 `EntityRegainHealthEvent`에서 `SATIATED` / `REGEN`만
취소합니다. 포션이나 능력에 의한 회복은 그대로 둡니다.

### 조건 미달일 때의 쿨타임
`AbilityDispatcher`는 이벤트를 쏜 **직후** 취소 여부와 무관하게 쿨타임을 겁니다.
그래서 두 예제 모두 조건을 못 맞추면 다음 틱에
`AbilityRegistry.setCooldown(p, id, 0)`으로 되돌립니다.

### 끄는 법
`CasterAbility.onEnable()`의 `// ── 예제 능력` 블록 4줄을 지우면 됩니다.

---

## 4. 남은 것

- 원본의 나머지 스크립트(`config/Config.sk`, `game/border.sk`,
  `game/teleport.sk`, `gui/Inventory.sk`)는 아직 옮기지 않았습니다.
- 서버에 올려서 하는 실제 플레이 검증은 안 했습니다.
  확인한 것은 `mvn clean package` 통과까지입니다.
