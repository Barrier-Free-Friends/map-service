package org.bf.mapservice.mapservice.domain.entity;

import jakarta.persistence.*;
import org.locationtech.jts.geom.Geometry;
import java.time.OffsetDateTime;

@Entity
@Table(name = "obstacle")
public class Obstacle {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "geom", nullable = false, columnDefinition = "geometry(Geometry,4326)")
    private Geometry geom;

    @Enumerated(EnumType.STRING)
    @Column(name = "geom_type", nullable = false, length = 20)
    private ObstacleGeometryType geomType;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 40)
    private ObstacleType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ObstacleStatus status;

    // POINT일 때 기본 버퍼 반경(미터). LINESTRING이면 null 허용
    @Column(name = "radius_meters")
    private Integer radiusMeters;

    // 기간형 장애물
    @Column(name = "starts_at")
    private OffsetDateTime startsAt;

    @Column(name = "ends_at")
    private OffsetDateTime endsAt;

    // 신뢰도(0~100)
    @Column(name = "confidence", nullable = false)
    private int confidence;

    // 생성/수정 시간 (Auditable 있으면 그걸로 대체)
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Obstacle() {}

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
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public Geometry getGeom() { return geom; }
    public ObstacleGeometryType getGeomType() { return geomType; }
    public ObstacleType getType() { return type; }
    public Severity getSeverity() { return severity; }
    public ObstacleStatus getStatus() { return status; }
    public Integer getRadiusMeters() { return radiusMeters; }
    public OffsetDateTime getStartsAt() { return startsAt; }
    public OffsetDateTime getEndsAt() { return endsAt; }
    public int getConfidence() { return confidence; }

    public void resolve() { this.status = ObstacleStatus.RESOLVED; }
    public void activate() { this.status = ObstacleStatus.ACTIVE; }
}
