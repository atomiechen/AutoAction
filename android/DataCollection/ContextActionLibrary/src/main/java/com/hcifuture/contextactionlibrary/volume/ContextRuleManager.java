package com.hcifuture.contextactionlibrary.volume;

import java.util.ArrayList;

public class ContextRuleManager {
    private ArrayList<ContextRule> ruleList;

    // Constructor
    public ContextRuleManager() {
        ruleList = new ArrayList<ContextRule>();
    }

    // Add a rule to the ruleList
    public void addRule(ContextRule rule) {
        ruleList.add(rule);
    }

    // Remove all rules from the ruleList
    public void clearRules() {
        ruleList.clear();
    }

    // Get all rules in the ruleList
    public ArrayList<ContextRule> getRules() {
        return ruleList;
    }

    // Find the most matching rule given a VolumeContext
    public String findMatchingRule(VolumeContext context) {
        String matchingAction = null;
        double maxScore = 0;

        // Iterate over all rules in the ruleList
        for (ContextRule rule : ruleList) {
            // Check if trigger is triggered
            if (rule.checkTrigger(context)) {
                // Check if condition is satisfied
                if (rule.checkCondition(context)) {
//                    // Calculate the score of the rule based on context matching
//                    double score = context.matchScore(rule.getContext());
//
//                    // Update the matching rule and score if the current rule is a better match
//                    if (score > maxScore) {
//                        matchingAction = rule.getAction();
//                        maxScore = score;
//                    }
                    matchingAction = rule.getAction();
                }
            }
        }

        return matchingAction;
    }
}