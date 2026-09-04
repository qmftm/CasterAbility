<img src="https://capsule-render.vercel.app/api?type=waving&height=300&color=gradient&text=CasterAbility&fontSize=70&fontAlignY=45&textBg=false&reversal=false&section=header?color=gradient&customColorList=12"></img>
### 개발: [qmftm](https://github.com/qmftm)
<br/>

**📖 소개**

Skript로 능력을 만들 수 있는 능력자 전쟁 플러그인입니다.

플러그인은 게임 진행(능력 추첨 · 월드 · 무적 시간 · 월드보더 · 쿨타임)을 맡고,
능력의 내용은 여러분이 Skript로 작성합니다.

<br/>

**📚 문서**

- [능력 작성 문법](docs/능력-작성-문법.md) — 능력 정의, 트리거, 상태이상, 액션바 등 Skript 구문 전체
- [기본 능력](docs/기본-능력.md) — 처음 설치하면 자동으로 깔리는 능력 4종
- [설정](docs/설정.md) — `config.yml` 항목 설명

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

트리거 종류, 상태이상, 액션바, 전체 구문은 [능력 작성 문법](docs/능력-작성-문법.md) 문서를 참고하세요.

> 능력을 **지우거나 이름을 바꾼** 뒤에는 `/ca reload` → `/sk reload all` 순서로 하세요.
> `/sk reload all` 만 하면 지운 능력이 목록에 그대로 남습니다.

<br/>

**🎁 기본 능력**

플러그인을 처음 설치하면 기본 능력 5종(얀데레·멘헤라·돼지·엑스칼리버·물귀신)이
`plugins/Skript/scripts/CasterAbility/classic/` 에 자동으로 깔립니다.
자세한 능력 설명은 [기본 능력](docs/기본-능력.md) 문서를 참고하세요.

처음 설치한 직후에는 `/sk reload all` 을 한 번 하거나 서버를 재시작하세요.
자동 설치가 싫으면 `config.yml` 의 `classic.install` 을 `false` 로 두세요.

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
