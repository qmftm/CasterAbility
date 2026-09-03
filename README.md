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

서버를 처음 켜면 `plugins/CasterAbility/examples/` 에 예제가 생성됩니다.

| 파일 | 내용 |
|---|---|
| `README.txt` | 문법과 사용 가능한 구문 전체 |
| `yandere.sk` | 얀데레 — 집착 대상, 거리 기반 피해 증감, 질투 |
| `menhera.sk` | 멘헤라 — 자연 회복 차단, 재생, 자해 강화 |

쓰고 싶은 파일을 `plugins/Skript/scripts/` 로 복사한 뒤 `/sk reload all` 하세요.

> 능력을 **지우거나 이름을 바꾼** 뒤에는 `/ca reload` → `/sk reload all` 순서로 하세요.
> `/sk reload all` 만 하면 지운 능력이 목록에 그대로 남습니다.

<br/>

**📋 License**

본 프로젝트는 [AbilityWar 라이선스](https://github.com/DayBreak365/AbilityWar/blob/master/LICENSE.md)를 따릅니다.

- [원본 프로젝트](https://github.com/Daybreak365/AbilityWar)
- [원본 라이선스](https://github.com/Daybreak365/AbilityWar/blob/master/LICENSE.md)

게임 진행 로직은 [ChzzkAbility](https://github.com/qmftm/ChzzkAbility)에서 옮겨왔습니다.

<br/>

**🔗 Download**

1.21.8 버전으로 제작되었으며, 1.21+버전의 사용을 권장드립니다.

**Skript 플러그인이 반드시 필요합니다.** (2.14.3 기준)
