# Map Service

Barrier Free Friends MSA의 맵/경로 서비스입니다. 장애물 정보를 등록·조회하고, GraphHopper 기반으로 장애물을 회피하는 경로를 탐색합니다.

전체 서비스 구성은 [조직 README](https://github.com/Barrier-Free-Friends)를 참고하세요.

## 기술 스택
- Java 21, Spring Boot 3.5.7 (JPA, QueryDSL)
- PostgreSQL + PostGIS (Hibernate Spatial, JTS)
- GraphHopper (경로 탐색 엔진, REST 연동)
- Kafka, OpenFeign, Resilience4j
- Eureka Client, Config Client

## 주요 구조
- `application/command`, `application/query` — 경로/장애물 유스케이스 (RouteApplicationService, ObstacleCommandService 등)
- `domain/entity` — Obstacle, MobilityType, Severity 등
- `infrastructure/graphhopper` — GraphHopper 연동
- `infrastructure/kafka` — 이벤트 연동
- `presentation/controller` — ObstaclesController, RoutesController

## 실행

```bash
./gradlew bootRun
```

기본 포트: `3334`

필요 환경변수: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `HOPPER_BASE_URL`

GraphHopper는 별도 컨테이너로 기동합니다: `infra/graphhopper/docker-compose.yml`

## Docker

```bash
docker build -t map-service .
docker run -p 3334:3334 map-service
```
