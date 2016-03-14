package se.kth.autoscalar.scaling;

import junit.framework.Assert;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import se.kth.autoscalar.common.monitoring.ResourceMonitoringEvent;
import se.kth.autoscalar.common.monitoring.RuleSupport;
import se.kth.autoscalar.common.monitoring.RuleSupport.ResourceType;
import se.kth.autoscalar.scaling.core.ElasticScalarAPI;
import se.kth.autoscalar.scaling.exceptions.ElasticScalarException;
import se.kth.autoscalar.scaling.group.Group;
import se.kth.autoscalar.scaling.rules.Rule;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Ashansa Perera
 * @version $Id$
 * @since 1.0
 */
public class ElasticScalingTest {

    private static ElasticScalarAPI elasticScalarAPI;
    private String GROUP_BASE_NAME = "my_group";
    private String RULE_BASE_NAME = "my_rule";
    private int coolingTimeOut = 60;
    private int coolingTimeIn = 300;

    double random;
    Rule rule;
    Rule rule2;
    Group group;
    String groupName;

    @BeforeClass
    public static void init() throws ElasticScalarException {
        elasticScalarAPI = new ElasticScalarAPI();
    }

    @Test
    public void testElasticScaling() throws ElasticScalarException {

        setRulesNGroup();
        //TODO should set rules in monitoring component
        MonitoringListener listener = elasticScalarAPI.startElasticScaling(group.getGroupName(), 2);
        //TODO pass the listener to monitoring component and it should send events based on rules

        //temporary mocking the monitoring events for scale out 1 machine
        ResourceMonitoringEvent resourceMonitoringEvent = new ResourceMonitoringEvent(ResourceType.CPU_PERCENTAGE,
                RuleSupport.Comparator.GREATER_THAN, (int)(random * 100) + 1);
        listener.onHighCPU(groupName, resourceMonitoringEvent);

        ArrayBlockingQueue<ScalingSuggestion>  suggestions = elasticScalarAPI.getSuggestionQueue(groupName);

        int count = 0;
        while (suggestions == null) {
            suggestions = elasticScalarAPI.getSuggestionQueue(groupName);
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                System.out.println("CPU_PERCENTAGE:testElasticScaling thread sleep while getting suggestions interrupted.............");
            }
            count++;
            if (count > 20) {
                new AssertionError("CPU_PERCENTAGE:No suggestion received during one minute");
            }
        }
        try {
            ScalingSuggestion suggestion = suggestions.take();
            switch (suggestion.getScalingDirection()) {
                //TODO validate suggestion
                case SCALE_IN:
                    System.out.println("...........CPU_PERCENTAGE: got scale in suggestion...........");
                    new AssertionError("CPU_PERCENTAGE: Events were to trigger scale out and retrieved a scale in suggestion");
                    break;
                case SCALE_OUT:
                    Assert.assertEquals(1, suggestion.getScaleOutSuggestions().size());
                    System.out.println("...........CPU_PERCENTAGE: got scale down suggestion...........");
                    break;
            }
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }


        //temporary mocking the monitoring events for scale out 2 machine
        resourceMonitoringEvent = new ResourceMonitoringEvent(ResourceType.RAM_PERCENTAGE, RuleSupport.Comparator.
                GREATER_THAN_OR_EQUAL, (int)(random * 100) + 3);
        listener.onHighRam(groupName, resourceMonitoringEvent);

        suggestions = elasticScalarAPI.getSuggestionQueue(groupName);
        count = 0;

        while (suggestions == null) {
            suggestions = elasticScalarAPI.getSuggestionQueue(groupName);
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                System.out.println("RAM_PERCENTAGE: testElasticScaling thread sleep while getting suggestions interrupted.............");
            }
            count++;
            if (count > 20) {
                new AssertionError("RAM_PERCENTAGE: No suggestion received during one minute");
            }
        }
        try {
            ScalingSuggestion suggestion = suggestions.take();
            switch (suggestion.getScalingDirection()) {
                //TODO validate suggestion
                case SCALE_IN:
                    System.out.println("...........RAM_PERCENTAGE: got scale in suggestion...........");
                    new AssertionError("RAM_PERCENTAGE: Events were to trigger scale out and retrieved a scale in suggestion");
                    break;
                case SCALE_OUT:
                    Assert.assertEquals(2, suggestion.getScaleOutSuggestions().size());
                    System.out.println("...........RAM_PERCENTAGE: got scale down suggestion...........");
                    break;
            }
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

   /* public void setMonitoringInfo() {
        HashMap<String, Number> systemInfo = new HashMap<String, Number>();
        systemInfo.put(ResourceType.CPU_PERCENTAGE.name(), 50.5);
        systemInfo.put(ResourceType.RAM_PERCENTAGE.name(), 85);
        //can add other supported resources similarly in future and elastic scalar should iterate the list and consume
        //ES ==> startES ==> getMonitoringInfo iteratively and set it somewhere (setMonitoringInfo) to use in ES logic

        //TODO should get the VM start time when adding a VM to the ES module, in order to decide when to shut down the machine
    }

    public void testRecommendation() {
        HashMap<String, Number> systemReq = new HashMap<String, Number>();
        systemReq.put("Min_CPUs", 4 );
        systemReq.put("Min_Ram", 8);
        systemReq.put("Min_Storage", 100);
    }*/

    private void setRulesNGroup() {

        try {
            random = Math.random();
            groupName = GROUP_BASE_NAME + String.valueOf((int) (random * 10));
            rule = elasticScalarAPI.createRule(RULE_BASE_NAME + String.valueOf((int) (random * 10)),
                    ResourceType.CPU_PERCENTAGE, RuleSupport.Comparator.GREATER_THAN, (float) (random * 100), 1);
            rule2 = elasticScalarAPI.createRule(RULE_BASE_NAME + String.valueOf((int)(random * 10) + 1),
                    ResourceType.RAM_PERCENTAGE, RuleSupport.Comparator.GREATER_THAN_OR_EQUAL, (float) ((random * 100) + 2) , 2);

            Map<Group.ResourceRequirement, Integer> minReq = new HashMap<Group.ResourceRequirement, Integer>();
            minReq.put(Group.ResourceRequirement.NUMBER_OF_VCPUS, 4);
            minReq.put(Group.ResourceRequirement.RAM, 8);
            minReq.put(Group.ResourceRequirement.STORAGE, 50);

            group = elasticScalarAPI.createGroup(groupName, (int)(random * 10), (int)(random * 100), coolingTimeOut,
                    coolingTimeIn, new String[]{rule.getRuleName(), rule2.getRuleName()}, minReq, 2.0f);

        } catch (ElasticScalarException e) {
            throw new IllegalStateException(e);
        }

    }

    @AfterClass
    public static void cleanup() throws ElasticScalarException {
        try {
            elasticScalarAPI.tempMethodDeleteTables();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }
}
