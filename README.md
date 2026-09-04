<img src="https://capsule-render.vercel.app/api?type=waving&height=300&color=gradient&text=CasterAbility&fontSize=70&fontAlignY=45&textBg=false&reversal=false&section=header?color=gradient&customColorList=12"></img>
### 개발: [qmftm](https://github.com/qmftm)
<br/>

**📖 소개**

Skript로 능력을 만들 수 있는 능력자 전쟁 플러그인입니다.

플러그인은 게임 진행(능력 추첨 · 월드 · 무적 시간 · 월드보더 · 쿨타임)을 맡고,
능력의 내용은 여러분이 Skript로 작성합니다.

<br/>

**⌨️ 명령어**

`/ca` `/aw` `/va` `/능력자`

| 명령어 | 설명 |
|---|---|
| `/ca start` | 능력자 게임 시작 |
| `/ca stop` | 능력자 게임 종료 |
| `/ca list` | 등록된 능력 목록 보기 |
| `/ca config` | 설정 GUI 열기 |
| `/ca reload` | 설정과 능력 정의 리로드 |

<br/>

**🗺️ 게임 월드**

`config.yml` 의 `spawn.world_name`(기본 `casterability`) 폴더가 서버 루트에 있으면
그걸 원본(템플릿)으로 보고, 게임을 시작할 때마다 `<world_name>-game` 으로
통째로 복제해서 씁니다. 원본은 절대 건드리지 않습니다.

- 템플릿이 없으면 그때그때 새로 생성합니다.
- 게임이 끝나면 그 판에 쓴 월드는 통째로 지웁니다. 폭발 자국이나 부순 블록이
  다음 게임에 남지 않습니다.
- 미리 만들어둔 맵을 쓰고 싶으면 그 맵 폴더를 서버 루트에 `world_name` 이름으로
  두세요.

<br/>

**✨ 능력 추가**

능력은 `plugins/Skript/scripts/` 안의 `.sk` 파일에서 정의합니다.

```
on load:
    ability class "yandere":
        name: "얀데레"
        tier: 2

    ability "yandere_jealousy":
        class: "yandere"
        trigger: right_click
        item: blaze_rod
        cooldown: 45

on ability use "yandere_jealousy":
    send "&d질투!" to player
```

패시브는 주기 대신 이벤트로도 발동할 수 있습니다.
`event:` 에는 스크립트에서 `on ~~~:` 로 쓰는 이벤트 이름을 그대로 적습니다.

```
on load:
    ability "reflect":
        class: "menhera"
        trigger: passive
        event: damage
        cooldown: 5

on ability use "reflect":
    damage event-entity by 2
```

액션바는 `상태이상 | 능력 부가 정보 | 능력 쿨타임` 순서로 그려지고,
비어 있는 칸은 건너뜁니다.

```
§c기절: 48 §7| §e충전 3/5 §7| §b질투: 13
```

가운데 칸은 능력마다 id를 따로 잡아서 서로 덮어쓰지 않습니다.

```
set ability info "charge" of player to "&e충전 3/5"
clear ability info "charge" of player
```

기절·출혈 같은 상태이상은 플러그인이 남은 시간을 세고 액션바에 그려줍니다.
여러 능력이 같은 상태이상을 공유할 수 있고, 면역도 만들 수 있습니다.

```
on load:
    set the name of status effect "stun" to "&c기절"
    set the hologram of status effect "stun" to "&c기절!"

on ability use "yandere_jealousy":
    apply status effect "stun" to event-entity for 3 seconds

# 면역
on status effect apply "stun":
    if player has ability class "menhera":
        cancel event
```

홀로그램을 등록해두면 걸린 사람 머리 위에 자동으로 뜨고 따라다니다가 풀리면 지워집니다.

액션바 표시: `§c기절: 48` (상태이상은 tick, 쿨타임은 초)

스크립트만으로는 만들기 번거로운 것들도 구문으로 제공합니다.

```
# 등록된 능력 훑어보기
loop all ability classes:
    send "%loop-value% — %name of ability class loop-value%"

# 특정 클래스를 가진 사람 전부에게
loop players with ability class "yandere":
    send "&d들켰다" to loop-value

# 능력을 잠깐 봉인
disable ability "yandere_jealousy" for event-entity
wait 10 seconds
enable ability "yandere_jealousy" for event-entity

# 다른 능력을 직접 발동
force player to use ability "yandere_jealousy" on event-entity
```

문법 전체는 `plugins/CasterAbility/SYNTAX.txt` 에 정리되어 있습니다.

> 능력을 **지우거나 이름을 바꾼** 뒤에는 `/ca reload` → `/sk reload all` 순서로 하세요.
> `/sk reload all` 만 하면 지운 능력이 목록에 그대로 남습니다.

<br/>

**🧩 구문 문법 (전체)**

새 구문을 찾을 땐 이 표부터 보세요. `%player%` `%string%` `%number%` 는
Skript 표준 타입이고, `[...]` 는 생략 가능, `(a|b)` 는 둘 중 하나입니다.
자세한 예시와 주의사항은 위쪽 예제들과 `SYNTAX.txt` 를 참고하세요.

섹션 Section — `on load:` 안에서만 사용

```
// 능력 클래스 정의. name·tier·description 은 모두 선택
ability class %string%:
    name: %string%
    tier: %number%
    description:
        %strings%

// 능력 정의. class 만 필수
ability %string%:
    class: %string%
    name: %string%
    trigger: %string%
    item: %itemtype%
    cooldown: %number%
    passive_interval: %number%
    event: %string%
```

`trigger:` 값

```
right_click   손에 든 아이템으로 우클릭
left_click    손에 든 아이템으로 좌클릭 (허공/블록을 향한 스윙 + 실제 타격 모두 포함)
passive       passive_interval(tick) 마다 자동, 또는 event: 로 대체 가능
on_hit        내가 남을 때렸을 때
on_damaged    내가 맞았을 때
on_kill       내가 죽였을 때
on_death      내가 죽었을 때
```

이벤트 Event

```
// 능력이 발동됐을 때. player, ability damage, event-entity 사용 가능
ability use %string%
ability used %string%

// 무적 시간이 끝나고 능력이 실제로 도는 시점(IN_GAME)
[caster[ ]ability] game start[ed]

// 게임 종료. 능력·쿨타임·상태이상이 지워지기 전에 발생
[caster[ ]ability] game end[ed]
[caster[ ]ability] game stop[ped]

// 상태이상이 걸리기 직전. cancel event 로 막으면 면역이 됨
// event-string(상태이상 id), event-number(걸릴 tick) 사용 가능
status effect apply [%-string%]
status effect applied [%-string%]

// 시간이 다 되어 저절로 풀렸을 때만 발생 (remove로 지운 경우엔 안 뜸)
status effect expire [%-string%]
status effect expired [%-string%]
```

조건 Condition

```
// 능력 클래스 보유 여부
%player% has ability class %string%
%player% doesn't have ability class %string%

// 특정 능력 보유 여부
%player% has ability %string%
%player% doesn't have ability %string%

// 능력을 하나라도 가지고 있는지
%player% has [an] ability
%player% has no ability

// 쿨타임 여부
%player% is on cooldown for [ability] %string%
%player% is not on cooldown for [ability] %string%

// disable ability 로 꺼둔 상태인지
[ability] %string% is disabled for %player%
[ability] %string% is enabled for %player%

// 상태이상 보유 여부
%player% has status effect %string%
%player% doesn't have status effect %string%
```

표현식 Expression — 플레이어 기준

```
// 그 플레이어가 가진 능력 클래스
[the] ability class [id] of %player%          → "yandere"
[the] ability class name of %player%          → "얀데레"
[the] ability [class] tier of %player%        → 2
[the] tier display of %player%                → "§c§lA" (색 포함)

// 그 플레이어가 가진 능력 id 목록
[the] abilities of %player%
%player%'s abilities

// 특정 능력의 남은 쿨타임 (초, 올림)
[the] [ability] cooldown of %player% for [ability] %string%

// ability use 이벤트 안에서만. 피해량 — 읽기·쓰기 모두 가능
ability damage
```

표현식 Expression — 등록된 정의 기준 (플레이어와 무관)

```
[the] name of [ability] class %string%              → "얀데레"
[the] tier of [ability] class %string%               → 2
[the] tier display of [ability] class %string%       → "§c§lA"
[the] abilities of [ability] class %string%          → 그 클래스에 속한 능력 id 목록

[all] [registered] ability classes                   → 등록된 모든 클래스 id
[all] [registered] abilities                          → 등록된 모든 능력 id
[all] players with ability class %string%             → 그 클래스를 가진 접속 중인 플레이어

[the] trigger of [ability] %string%                   → "right_click"
[the] name of ability %string%                        → "질투" (없으면 id)
[the] event of [ability] %string%                     → event: 를 적은 passive만
[the] item of [ability] %string%                      → 없으면 빈 문자열
[the] max[imum] cooldown of [ability] %string%         → 정의된 값. 남은 쿨타임 아님
```

표현식 Expression — 상태이상

```
[the] remaining tick[s] of status effect %string% (for|of) %player%
%player%'s remaining tick[s] of status effect %string%

[all] status effects of %player%
%player%'s status effects

// status effect apply 이벤트 안에서만. 걸릴 시간(tick) — 읽기·쓰기 가능
status effect duration
```

효과 Effect — 능력 조작

```
set ability class of %player% to %string%
give %player% ability class %string%

remove ability [class] from %player%
clear ability [class] of %player%

set [ability] cooldown of %player% for [ability] %string% to %number%
reset [ability] cooldown of %player% for [ability] %string%
reset all [ability] cooldowns of %player%
clear all [ability] cooldowns of %player%

disable [ability] %string% for %player%
enable [ability] %string% for %player%
enable all abilities for %player%

// on ability use 이벤트를 그냥 발생시킴. 쿨타임을 걸거나 확인하지 않음
force %player% to use [ability] %string% [on %-entity%]
trigger [ability] %string% for %player% [on %-entity%]
```

효과 Effect — 상태이상

```
apply status effect %string% to %player% for %number% (seconds|ticks)
apply status effect %string% to %player% for %number% (seconds|ticks) if not active
stack status effect %string% on %player% for %number% (seconds|ticks)
refresh status effect %string% on %player% for %number% (seconds|ticks)

remove status effect %string% from %player%
clear [all] status effects (from|of) %player%

set [the] name of status effect %string% to %string%
set [the] hologram of status effect %string% to %string%
```

효과 Effect — 액션바 부가 정보

```
set ability info %string% (of|for) %player% to %string%
clear ability info %string% (of|for) %player%
clear all ability info (of|for) %player%
```

<br/>

**🎁 기본 능력**

플러그인을 처음 설치하면 기본 능력이 자동으로 깔립니다.

```
plugins/Skript/scripts/CasterAbility/classic/
├── yandere.sk     얀데레     (A등급)
├── menhera.sk     멘헤라     (B등급)
├── pig.sk         돼지       (B등급)
└── excalibur.sk   엑스칼리버 (A등급)
```

처음 설치한 직후에는 `/sk reload all` 을 한 번 하거나 서버를 재시작하세요.

- `classic` 폴더가 이미 있으면 건드리지 않습니다. 능력 파일 하나를 지워도 되살아나지 않습니다.
- 처음 상태로 되돌리려면 `classic` 폴더째 지우고 재시작하세요.
- 자동 설치가 싫으면 `config.yml` 의 `classic.install` 을 `false` 로 두세요.

<br/>

**📋 License**

본 프로젝트는 [AbilityWar 라이선스](https://github.com/DayBreak365/AbilityWar/blob/master/LICENSE.md)를 따릅니다.

- [원본 프로젝트](https://github.com/Daybreak365/AbilityWar)
- [원본 라이선스](https://github.com/Daybreak365/AbilityWar/blob/master/LICENSE.md)

<br/>

**🔗 Download**

Minecraft **26.2** 버전으로 제작되었습니다.

- 서버: PaperMC 26.2 (**Java 25** 필요)
- **Skript 플러그인이 반드시 필요합니다.** (2.16.2 기준)
