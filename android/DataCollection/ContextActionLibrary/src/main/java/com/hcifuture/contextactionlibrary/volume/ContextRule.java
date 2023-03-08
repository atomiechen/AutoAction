package com.hcifuture.contextactionlibrary.volume;

import java.util.ArrayList;

public class ContextRule {
    private final String utterance;
    private final ArrayList<String> triggers;
    private final Condition condition;
    private final String action;

    // Constructor
    public ContextRule(String utterance, ArrayList<String> triggers, Condition condition, String action) {
        this.utterance = utterance;
        this.triggers = triggers;
        this.condition = condition;
        this.action = action;
    }

    // Getters
    public String getUtterance() {
        return utterance;
    }

    public ArrayList<String> getTrigger() {
        return triggers;
    }

    public Condition getCondition() {
        return condition;
    }

    public String getAction() {
        return action;
    }

    // Methods
    public boolean checkTrigger(VolumeContext context) {
        for (String s : context.getEvents()) {
            if (this.triggers.contains(s)) {
                return true;
            }
        }
        return false;
    }

    public boolean checkCondition(VolumeContext context) {
        return condition.isSatisfied(context);
    }

    public void performAction() {
    }
}
