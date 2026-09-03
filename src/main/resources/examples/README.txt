CasterAbility — 예제 스크립트
================================

이 폴더의 .sk 파일은 CasterAbility가 서버 시작 시 꺼내둔 예제입니다.
여기 있는 것만으로는 아무 일도 일어나지 않습니다.

사용법
------
1. 쓰고 싶은 .sk 파일을 plugins/Skript/scripts/ 로 복사합니다.
2. 서버에서 /sk reload all 을 실행합니다.
3. /ca start 로 게임을 시작하면 추첨 목록에 능력이 나옵니다.

이 폴더의 파일은 덮어쓰지 않습니다.
예제를 처음 상태로 되돌리려면 파일을 지우고 서버를 재시작하세요.


능력 정의 문법
--------------
능력은 on load 블록 안에서 정의합니다.

    on load:
        ability class "yandere":
            name: "얀데레"
            tier: 2

        ability "yandere_jealousy":
            class: "yandere"
            trigger: right_click
            item: blaze_rod
            cooldown: 45

ability class 항목
    name    표시 이름. 없으면 id를 그대로 씁니다.
    tier    0=Legendary 1=S 2=A 3=B 4=C. 기본 4.

ability 항목
    class               (필수) 어느 ability class에 속하는지
    trigger             아래 목록 중 하나. 기본 right_click
    item                트리거로 쓸 손에 든 아이템. 없으면 아무거나
    cooldown            초 단위. 기본 0 (쿨타임 없음)
    passive_interval    trigger가 passive일 때 tick 간격. 기본 20

trigger 종류
    right_click     손에 든 아이템으로 우클릭
    left_click      좌클릭
    passive         passive_interval 마다 자동
    on_hit          내가 남을 때렸을 때
    on_damaged      내가 맞았을 때
    on_kill         내가 죽였을 때
    on_death        내가 죽었을 때


능력 동작 작성
--------------
    on ability use "능력id":
        # 여기에 동작을 씁니다

이벤트 안에서 쓸 수 있는 값
    player          능력을 쓴 플레이어
    event-entity    대상. on_hit이면 맞은 쪽, on_damaged면 때린 쪽
    event-number    피해량 (on_hit / on_damaged 일 때)
    event-string    발동한 능력 id
    ability damage  피해량. 읽기와 쓰기 모두 됩니다

    cancel event 를 하면 능력이 취소되고 쿨타임도 걸리지 않습니다.


쓸 수 있는 구문
---------------
표현식
    ability class of %player%            → "yandere"
    ability class name of %player%       → "얀데레"
    ability class tier of %player%       → 2
    cooldown of %player% for ability %string%   → 남은 초
    ability damage                       → 피해량 (set 가능)

조건
    %player% has ability class %string%
    %player% doesn't have ability class %string%
    %player% has an ability
    %player% has no ability
    %player% is on cooldown for ability %string%
    %player% is not on cooldown for ability %string%

효과
    set ability class of %player% to %string%
    give %player% ability class %string%
    remove ability from %player%
    set cooldown of %player% for ability %string% to %number%
    reset cooldown of %player% for ability %string%


주의
----
ability class 를 먼저 정의하고 그 다음에 ability 를 정의하세요.
같은 on load 블록 안에서 위에서 아래 순서로 실행됩니다.

능력을 스크립트에서 지우고 리로드해도 이미 등록된 정의는 남아 있습니다.
완전히 지우려면 서버를 재시작하세요.
