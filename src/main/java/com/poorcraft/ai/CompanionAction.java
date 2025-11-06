package com.poorcraft.ai;

/**
 * Represents an action the AI companion should perform.
 */
public record CompanionAction(ActionType type, String target, int quantity) {

    public static CompanionAction gather(String resource, int quantity) {
        return new CompanionAction(ActionType.GATHER, resource, quantity);
    }

    public static CompanionAction follow() {
        return new CompanionAction(ActionType.FOLLOW, null, 0);
    }

    public static CompanionAction stop() {
        return new CompanionAction(ActionType.STOP, null, 0);
    }

    public enum ActionType {
        GATHER,
        MOVE_TO,
        FOLLOW,
        STOP,
        BREAK_BLOCK,
        SAY
    }
}
