package org.bf.mapservice.mapservice.application.query;

/*
    경로 계산(라우팅) 시 장애물이 해당 이동수단에 미치는 판단 결과
    1.0 → 영향 없음 (사실상 ignore)
    0.0 < x < 1.0 → 우회 유도 (작을수록 강함)
    0.0 → 사실상 차단과 동일한 효과
    enum을 안한 이유는 penalty 강도를 담으려면 record로 해야함
 */
public record Decision(boolean blocked, double priorityMultiply) {
    public static Decision ofBlock() { return new Decision(true, 0.0); }
    public static Decision ignore() { return new Decision(false, 1.0); }
    public static Decision penalty(double multiply) { return new Decision(false, multiply); }
}