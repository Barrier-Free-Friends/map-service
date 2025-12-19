package org.bf.mapservice.mapservice.application.query;

public final class BboxUtil {

    public record Bbox(double minLon, double minLat, double maxLon, double maxLat) {}

    private BboxUtil() {}

    public static Bbox envelopeWithMarginMeters(double lat1, double lon1, double lat2, double lon2, double marginMeters) {
        //시작점 끝점 중 크기 상관없이 사각형 범위의 기본 테두리 잡음
        double minLat = Math.min(lat1, lat2);
        double maxLat = Math.max(lat1, lat2);
        double minLon = Math.min(lon1, lon2);
        double maxLon = Math.max(lon1, lon2);

        /*  미터 위경도 degree(도)로 바꾸기
            위도 1도는 대략 111.32km → 그래서 metersPerDegLat는 상수로 둠.
            경도 1도는 위도에 따라 줄어듦(극지방 갈수록 0에 가까움)
            그래서 cos(midLat)를 곱해서 경도 방향 길이를 줄여 계산
            위도 방향: 거의 고정 / 경도 방향: midLat 기반 근사
        */
        double midLat = (lat1 + lat2) / 2.0;
        double metersPerDegLat = 111_320.0;
        double metersPerDegLon = Math.cos(Math.toRadians(midLat)) * 111_320.0;

        double dLat = marginMeters / metersPerDegLat;
        double dLon = marginMeters / metersPerDegLon;

        return new Bbox(
                minLon - dLon,
                minLat - dLat,
                maxLon + dLon,
                maxLat + dLat
        );
    }
}