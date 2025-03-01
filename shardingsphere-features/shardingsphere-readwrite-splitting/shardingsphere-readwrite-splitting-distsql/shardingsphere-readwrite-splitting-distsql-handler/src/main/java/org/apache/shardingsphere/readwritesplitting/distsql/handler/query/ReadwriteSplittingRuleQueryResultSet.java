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

package org.apache.shardingsphere.readwritesplitting.distsql.handler.query;

import org.apache.shardingsphere.infra.config.algorithm.ShardingSphereAlgorithmConfiguration;
import org.apache.shardingsphere.infra.distsql.constant.ExportableConstants;
import org.apache.shardingsphere.infra.distsql.query.DistSQLResultSet;
import org.apache.shardingsphere.infra.metadata.ShardingSphereDatabase;
import org.apache.shardingsphere.infra.properties.PropertiesConverter;
import org.apache.shardingsphere.infra.rule.identifier.type.ExportableRule;
import org.apache.shardingsphere.readwritesplitting.api.ReadwriteSplittingRuleConfiguration;
import org.apache.shardingsphere.readwritesplitting.api.rule.ReadwriteSplittingDataSourceRuleConfiguration;
import org.apache.shardingsphere.readwritesplitting.distsql.parser.statement.ShowReadwriteSplittingRulesStatement;
import org.apache.shardingsphere.sql.parser.sql.common.statement.SQLStatement;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;

/**
 * Result set for show readwrite splitting rule.
 */
public final class ReadwriteSplittingRuleQueryResultSet implements DistSQLResultSet {
    
    private static final String DYNAMIC = "Dynamic";
    
    private Iterator<Collection<Object>> data = Collections.emptyIterator();
    
    private Map<String, Map<String, String>> exportableAutoAwareDataSource = Collections.emptyMap();
    
    private Map<String, Map<String, String>> exportableDataSourceMap = Collections.emptyMap();
    
    @Override
    public void init(final ShardingSphereDatabase database, final SQLStatement sqlStatement) {
        Optional<ReadwriteSplittingRuleConfiguration> ruleConfig = database.getRuleMetaData().findRuleConfigurations(ReadwriteSplittingRuleConfiguration.class).stream().findAny();
        buildExportableMap(database);
        ruleConfig.ifPresent(optional -> data = buildData(optional).iterator());
    }
    
    private void buildExportableMap(final ShardingSphereDatabase database) {
        Optional<ExportableRule> exportableRule = getExportableRule(database);
        exportableRule.ifPresent(optional -> {
            Map<String, Object> exportable = exportableRule.get()
                    .export(Arrays.asList(ExportableConstants.EXPORTABLE_KEY_AUTO_AWARE_DATA_SOURCE, ExportableConstants.EXPORTABLE_KEY_ENABLED_DATA_SOURCE));
            exportableAutoAwareDataSource = (Map<String, Map<String, String>>) exportable.getOrDefault(ExportableConstants.EXPORTABLE_KEY_AUTO_AWARE_DATA_SOURCE, Collections.emptyMap());
            exportableDataSourceMap = (Map<String, Map<String, String>>) exportable.getOrDefault(ExportableConstants.EXPORTABLE_KEY_ENABLED_DATA_SOURCE, Collections.emptyMap());
        });
    }
    
    private Optional<ExportableRule> getExportableRule(final ShardingSphereDatabase database) {
        return database.getRuleMetaData().findRules(ExportableRule.class).stream()
                .filter(each -> each.containExportableKey(Arrays.asList(ExportableConstants.EXPORTABLE_KEY_AUTO_AWARE_DATA_SOURCE, ExportableConstants.EXPORTABLE_KEY_ENABLED_DATA_SOURCE))).findAny();
    }
    
    private Collection<Collection<Object>> buildData(final ReadwriteSplittingRuleConfiguration ruleConfig) {
        Collection<Collection<Object>> result = new LinkedList<>();
        ruleConfig.getDataSources().forEach(each -> {
            Collection<Object> dataItem = buildDataItem(each, getLoadBalancers(ruleConfig));
            result.add(dataItem);
        });
        return result;
    }
    
    private Collection<Object> buildDataItem(final ReadwriteSplittingDataSourceRuleConfiguration dataSourceRuleConfig, final Map<String, ShardingSphereAlgorithmConfiguration> loadBalancers) {
        String name = dataSourceRuleConfig.getName();
        Map<String, String> exportDataSources = DYNAMIC.equalsIgnoreCase(dataSourceRuleConfig.getType()) ? exportableAutoAwareDataSource.get(name) : exportableDataSourceMap.get(name);
        Optional<ShardingSphereAlgorithmConfiguration> loadBalancer = Optional.ofNullable(loadBalancers.get(dataSourceRuleConfig.getLoadBalancerName()));
        return Arrays.asList(name,
                dataSourceRuleConfig.getAutoAwareDataSourceName().orElse(""),
                getWriteDataSourceName(dataSourceRuleConfig, exportDataSources),
                getReadDataSourceNames(dataSourceRuleConfig, exportDataSources),
                loadBalancer.map(ShardingSphereAlgorithmConfiguration::getType).orElse(""),
                loadBalancer.map(each -> PropertiesConverter.convert(each.getProps())).orElse(""));
    }
    
    private Map<String, ShardingSphereAlgorithmConfiguration> getLoadBalancers(final ReadwriteSplittingRuleConfiguration ruleConfig) {
        Map<String, ShardingSphereAlgorithmConfiguration> loadBalancers = ruleConfig.getLoadBalancers();
        return null != loadBalancers ? loadBalancers : Collections.emptyMap();
    }
    
    private String getWriteDataSourceName(final ReadwriteSplittingDataSourceRuleConfiguration dataSourceRuleConfig, final Map<String, String> exportDataSources) {
        if (null != exportDataSources) {
            return exportDataSources.get(ExportableConstants.PRIMARY_DATA_SOURCE_NAME);
        }
        return dataSourceRuleConfig.getWriteDataSourceName().orElse("");
    }
    
    private String getReadDataSourceNames(final ReadwriteSplittingDataSourceRuleConfiguration dataSourceRuleConfig, final Map<String, String> exportDataSources) {
        if (null != exportDataSources) {
            return exportDataSources.get(ExportableConstants.REPLICA_DATA_SOURCE_NAMES);
        }
        return dataSourceRuleConfig.getReadDataSourceNames().orElse("");
    }
    
    @Override
    public Collection<String> getColumnNames() {
        return Arrays.asList("name", "auto_aware_data_source_name", "write_data_source_name", "read_data_source_names", "load_balancer_type", "load_balancer_props");
    }
    
    @Override
    public boolean next() {
        return data.hasNext();
    }
    
    @Override
    public Collection<Object> getRowData() {
        return data.next();
    }
    
    @Override
    public String getType() {
        return ShowReadwriteSplittingRulesStatement.class.getName();
    }
}
