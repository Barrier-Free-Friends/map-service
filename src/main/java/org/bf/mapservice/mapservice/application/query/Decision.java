package org.bf.mapservice.mapservice.application.query;

public record Decision(boolean blocked, double priorityMultiply) {
    public static Decision ofBlock() { return new Decision(true, 0.0); }
    public static Decision ignore() { return new Decision(false, 1.0); }
    public static Decision penalty(double multiply) { return new Decision(false, multiply); }
}