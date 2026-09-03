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

on ability use "yandere_jealousy":
    apply status effect "stun" to event-entity for 3 seconds

# 면역
on status effect apply "stun":
    if player has ability class "menhera":
        cancel event
```

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

**🎁 기본 능력**

플러그인을 처음 설치하면 기본 능력이 자동으로 깔립니다.

```
plugins/Skript/scripts/CasterAbility/classic/
├── yandere.sk     얀데레   (A) — 집착 대상, 거리 기반 공격력, 질투
├── menhera.sk     멘헤라   (B) — 자연 회복 차단, 피격 재생, 자신감 해소
├── pig.sk         돼지     (B) — 몸무게, 공격력·이동속도 교환, 폭주
└── excalibur.sk   엑스칼리버 (A) — 전용 검, 구속 해방 스택
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

게임 진행 로직은 [ChzzkAbility](https://github.com/qmftm/ChzzkAbility)에서 옮겨왔습니다.

<br/>

**🔗 Download**

Minecraft **26.2** 버전으로 제작되었습니다.

- 서버: PaperMC 26.2 (**Java 25** 필요)
- **Skript 플러그인이 반드시 필요합니다.** (2.16.2 기준)
