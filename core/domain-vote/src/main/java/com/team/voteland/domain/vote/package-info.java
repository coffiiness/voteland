/**
 * Vote 도메인 모듈
 *
 * 이 모듈은 투표 관련 기능을 담당합니다. 현재는 패키지 구조만 생성되어 있으며, 실제 투표 기능은 추후 구현 예정입니다.
 *
 * 패키지 구조: - domain/: 도메인 모델 및 서비스, 이벤트 - api/: REST API 컨트롤러 및 DTO - infra/: 인프라스트럭처 구현체
 *
 * 도메인 이벤트: - VoteCompletedEvent: 투표 완료 시 발행 - UserDeletedEventHandler: 회원 탈퇴 이벤트 수신
 */
package com.team.voteland.domain.vote;
