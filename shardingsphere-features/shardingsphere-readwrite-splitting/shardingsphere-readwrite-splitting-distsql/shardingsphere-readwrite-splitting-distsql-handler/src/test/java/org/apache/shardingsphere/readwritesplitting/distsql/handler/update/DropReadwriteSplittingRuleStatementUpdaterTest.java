/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.readwritesplitting.distsql.handler.update;

import org.apache.shardingsphere.infra.config.algorithm.ShardingSphereAlgorithmConfiguration;
import org.apache.shardingsphere.infra.config.function.ResourceRequiredRuleConfiguration;
import org.apache.shardingsphere.infra.distsql.exception.rule.RequiredRuleMissedException;
import org.apache.shardingsphere.infra.distsql.exception.rule.RuleDefinitionViolationException;
import org.apache.shardingsphere.infra.distsql.exception.rule.RuleInUsedException;
import org.apache.shardingsphere.infra.metadata.ShardingSphereDatabase;
import org.apache.shardingsphere.readwritesplitting.api.ReadwriteSplittingRuleConfiguration;
import org.apache.shardingsphere.readwritesplitting.api.rule.ReadwriteSplittingDataSourceRuleConfiguration;
import org.apache.shardingsphere.readwritesplitting.distsql.parser.statement.DropReadwriteSplittingRuleStatement;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public final class DropReadwriteSplittingRuleStatementUpdaterTest {
    
    private final DropReadwriteSplittingRuleStatementUpdater updater = new DropReadwriteSplittingRuleStatementUpdater();
    
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ShardingSphereDatabase database;
    
    @Test(expected = RequiredRuleMissedException.class)
    public void assertCheckSQLStatementWithoutCurrentRule() throws RuleDefinitionViolationException {
        updater.checkSQLStatement(database, createSQLStatement(), null);
    }
    
    @Test(expected = RequiredRuleMissedException.class)
    public void assertCheckSQLStatementWithoutToBeDroppedRule() throws RuleDefinitionViolationException {
        updater.checkSQLStatement(database, createSQLStatement(), new ReadwriteSplittingRuleConfiguration(Collections.emptyList(), Collections.emptyMap()));
    }
    
    @Test
    public void assertCheckSQLStatementWithIfExists() throws RuleDefinitionViolationException {
        when(database.getRuleMetaData().findRuleConfigurations(ResourceRequiredRuleConfiguration.class)).thenReturn(Collections.emptyList());
        updater.checkSQLStatement(database, new DropReadwriteSplittingRuleStatement(true, Collections.singleton("readwrite_ds")),
                new ReadwriteSplittingRuleConfiguration(Collections.emptyList(), Collections.emptyMap()));
        updater.checkSQLStatement(database, new DropReadwriteSplittingRuleStatement(true, Collections.singleton("readwrite_ds")), null);
    }
    
    @Test(expected = RuleInUsedException.class)
    public void assertCheckSQLStatementWithInUsed() throws RuleDefinitionViolationException {
        when(database.getRuleMetaData().findRuleConfigurations(any()))
                .thenReturn(Collections.singletonList((ResourceRequiredRuleConfiguration) () -> Collections.singleton("readwrite_ds")));
        updater.checkSQLStatement(database, createSQLStatement(), createCurrentRuleConfiguration());
    }
    
    @Test
    public void assertUpdateCurrentRuleConfiguration() {
        ReadwriteSplittingRuleConfiguration ruleConfig = createCurrentRuleConfiguration();
        assertTrue(updater.updateCurrentRuleConfiguration(createSQLStatement(), ruleConfig));
        assertThat(ruleConfig.getLoadBalancers().size(), is(1));
    }
    
    @Test
    public void assertUpdateCurrentRuleConfigurationWithInUsedLoadBalancer() {
        ReadwriteSplittingRuleConfiguration ruleConfig = createMultipleCurrentRuleConfigurations();
        assertFalse(updater.updateCurrentRuleConfiguration(createSQLStatement(), ruleConfig));
        assertThat(ruleConfig.getLoadBalancers().size(), is(1));
    }
    
    private DropReadwriteSplittingRuleStatement createSQLStatement() {
        return new DropReadwriteSplittingRuleStatement(Collections.singleton("readwrite_ds"));
    }
    
    private ReadwriteSplittingRuleConfiguration createCurrentRuleConfiguration() {
        ReadwriteSplittingDataSourceRuleConfiguration dataSourceRuleConfig = new ReadwriteSplittingDataSourceRuleConfiguration("readwrite_ds", "Static", new Properties(), "TEST");
        Map<String, ShardingSphereAlgorithmConfiguration> loadBalancers = Collections.singletonMap("readwrite_ds", new ShardingSphereAlgorithmConfiguration("TEST", new Properties()));
        return new ReadwriteSplittingRuleConfiguration(new LinkedList<>(Collections.singleton(dataSourceRuleConfig)), loadBalancers);
    }
    
    private ReadwriteSplittingRuleConfiguration createMultipleCurrentRuleConfigurations() {
        ReadwriteSplittingDataSourceRuleConfiguration fooDataSourceRuleConfig = new ReadwriteSplittingDataSourceRuleConfiguration("foo_ds", "Static", new Properties(), "TEST");
        ReadwriteSplittingDataSourceRuleConfiguration barDataSourceRuleConfig = new ReadwriteSplittingDataSourceRuleConfiguration("bar_ds", "Static", new Properties(), "TEST");
        Map<String, ShardingSphereAlgorithmConfiguration> loadBalancers = Collections.singletonMap("foo_ds", new ShardingSphereAlgorithmConfiguration("TEST", new Properties()));
        return new ReadwriteSplittingRuleConfiguration(new LinkedList<>(Arrays.asList(fooDataSourceRuleConfig, barDataSourceRuleConfig)), loadBalancers);
    }
}
