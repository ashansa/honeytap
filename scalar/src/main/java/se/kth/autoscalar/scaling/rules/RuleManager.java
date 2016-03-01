package se.kth.autoscalar.scaling.rules;

import se.kth.autoscalar.scaling.exceptions.ElasticScalarException;
import se.kth.autoscalar.common.monitoring.RuleSupport.ResourceType;
import se.kth.autoscalar.common.monitoring.RuleSupport.Comparator;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Ashansa Perera
 * @version $Id$
 * @since 1.0
 */
public interface RuleManager {

    Rule createRule(String ruleName, ResourceType resourceType, Comparator comparator,
                    float thresholdPercentage, int operationAction) throws ElasticScalarException;

    Rule getRule(String ruleName) throws ElasticScalarException;

    void updateRule(String ruleName, Rule rule) throws ElasticScalarException;

    void deleteRule(String ruleName) throws ElasticScalarException;

    boolean isRuleExists(String ruleName) throws ElasticScalarException;

    boolean isRuleInUse(String ruleName);

    String[] getRuleUsage(String ruleName) throws ElasticScalarException;
}
