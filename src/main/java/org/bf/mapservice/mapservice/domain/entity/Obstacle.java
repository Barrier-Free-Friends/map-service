package org.bf.mapservice.mapservice.domain.entity;

import jakarta.persistence.*;
import org.bf.global.domain.Auditable;
import org.locationtech.jts.geom.Geometry;
import java.time.OffsetDateTime;

@Entity
@Table(name = "obstacle")
public class Obstacle extends Auditable {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
       공간 정보 => 장애물이 실제로 지도 어디에 존재하는지 / POINT, LINESTRING, POLYGON 가능
       JTS Geometry, SRID 4326 (WGS84, 위경도), DB/도메인 전용, API DTO에는 그대로 못 나감
    */
    @Column(name = "geom", nullable = false, columnDefinition = "geometry(Geometry,4326)")
    private Geometry geom;

    /*
        따로 있는 이유 => Geometry 자체는 추상적 정책 / 로직에서 분기하기 위함 / 도메인 로직을 단순화하기 위한 명시적 타입
    */
    @Enumerated(EnumType.STRING)
    @Column(name = "geom_type", nullable = false, length = 20)
    private ObstacleGeometryType geomType;

    // 장애물 종류
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 40)
    private ObstacleType type;

    // 장애물의 심각도 경로 탐색, 회피 우선순위, 경고 표시 등에 사용
    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private Severity severity;

    // 장애물의 상태
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ObstacleStatus status;

    // POINT일 때 기본 버퍼 반경(미터). LINESTRING이면 null 허용
    @Column(name = "radius_meters")
    private Integer radiusMeters;

    // 기간형 장애물 starts_at, ends_at
    @Column(name = "starts_at")
    private OffsetDateTime startsAt;

    @Column(name = "ends_at")
    private OffsetDateTime endsAt;

    // 신뢰도(0~100)
    @Column(name = "confidence", nullable = false)
    private int confidence;

    //JPA 전용이라 외부에서 new 금지
    protected Obstacle() {}

    //불완전한 장애물 생성 방지 생성 시점에 반드시 필요한 값 강제
    public Obstacle(
            Geometry geom,
            ObstacleGeometryType geomType,
            ObstacleType type,
            Severity severity,
            ObstacleStatus status,
            Integer radiusMeters,
            OffsetDateTime startsAt,
            OffsetDateTime endsAt,
            int confidence
    ) {
        this.geom = geom;
        this.geomType = geomType;
        this.type = type;
        this.severity = severity;
        this.status = status;
        this.radiusMeters = radiusMeters;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.confidence = confidence;
    }

    public Long getId() { return id; }
    public Geometry getGeom() { return geom; }
    public ObstacleGeometryType getGeomType() { return geomType; }
    public ObstacleType getType() { return type; }
    public Severity getSeverity() { return severity; }
    public Integer getRadiusMeters() { return radiusMeters; }

    //상태 변경의 의미를 코드에 남김 나중에 규칙 추가 가능
    public void resolve() { this.status = ObstacleStatus.RESOLVED; }
}
