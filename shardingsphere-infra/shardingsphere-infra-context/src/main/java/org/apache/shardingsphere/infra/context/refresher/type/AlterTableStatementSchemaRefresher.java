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

package org.apache.shardingsphere.infra.context.refresher.type;

import org.apache.shardingsphere.infra.config.props.ConfigurationProperties;
import org.apache.shardingsphere.infra.context.refresher.MetaDataRefresher;
import org.apache.shardingsphere.infra.eventbus.ShardingSphereEventBus;
import org.apache.shardingsphere.infra.federation.optimizer.context.planner.OptimizerPlannerContext;
import org.apache.shardingsphere.infra.federation.optimizer.context.planner.OptimizerPlannerContextFactory;
import org.apache.shardingsphere.infra.federation.optimizer.metadata.FederationDatabaseMetaData;
import org.apache.shardingsphere.infra.metadata.ShardingSphereDatabase;
import org.apache.shardingsphere.infra.metadata.schema.builder.SchemaBuilderMaterials;
import org.apache.shardingsphere.infra.metadata.schema.builder.TableMetaDataBuilder;
import org.apache.shardingsphere.infra.metadata.schema.event.SchemaAlteredEvent;
import org.apache.shardingsphere.infra.metadata.schema.model.SchemaMetaData;
import org.apache.shardingsphere.infra.metadata.schema.model.TableMetaData;
import org.apache.shardingsphere.infra.rule.identifier.type.DataNodeContainedRule;
import org.apache.shardingsphere.infra.rule.identifier.type.MutableDataNodeRule;
import org.apache.shardingsphere.sql.parser.sql.common.statement.ddl.AlterTableStatement;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * Schema refresher for alter table statement.
 */
public final class AlterTableStatementSchemaRefresher implements MetaDataRefresher<AlterTableStatement> {
    
    private static final String TYPE = AlterTableStatement.class.getName();
    
    @Override
    public void refresh(final ShardingSphereDatabase database, final FederationDatabaseMetaData federationDatabaseMetaData, final Map<String, OptimizerPlannerContext> optimizerPlanners,
                        final Collection<String> logicDataSourceNames, final String schemaName, final AlterTableStatement sqlStatement, final ConfigurationProperties props) throws SQLException {
        String tableName = sqlStatement.getTable().getTableName().getIdentifier().getValue();
        SchemaAlteredEvent event = new SchemaAlteredEvent(database.getName(), schemaName);
        if (sqlStatement.getRenameTable().isPresent()) {
            String renameTable = sqlStatement.getRenameTable().get().getTableName().getIdentifier().getValue();
            putTableMetaData(database, federationDatabaseMetaData, optimizerPlanners, logicDataSourceNames, schemaName, renameTable, props);
            removeTableMetaData(database, federationDatabaseMetaData, optimizerPlanners, schemaName, tableName);
            event.getAlteredTables().add(database.getSchemas().get(schemaName).get(renameTable));
            event.getDroppedTables().add(tableName);
        } else {
            putTableMetaData(database, federationDatabaseMetaData, optimizerPlanners, logicDataSourceNames, schemaName, tableName, props);
            event.getAlteredTables().add(database.getSchemas().get(schemaName).get(tableName));
        }
        ShardingSphereEventBus.getInstance().post(event);
    }
    
    private void removeTableMetaData(final ShardingSphereDatabase database, final FederationDatabaseMetaData federationDatabaseMetaData,
                                     final Map<String, OptimizerPlannerContext> optimizerPlanners, final String schemaName, final String tableName) {
        database.getSchemas().get(schemaName).remove(tableName);
        database.getRuleMetaData().findRules(MutableDataNodeRule.class).forEach(each -> each.remove(schemaName, tableName));
        federationDatabaseMetaData.removeTableMetadata(schemaName, tableName);
        optimizerPlanners.put(federationDatabaseMetaData.getName(), OptimizerPlannerContextFactory.create(federationDatabaseMetaData));
    }
    
    private void putTableMetaData(final ShardingSphereDatabase database, final FederationDatabaseMetaData federationDatabaseMetaData, final Map<String, OptimizerPlannerContext> optimizerPlanners,
                                  final Collection<String> logicDataSourceNames, final String schemaName, final String tableName, final ConfigurationProperties props) throws SQLException {
        if (!containsInImmutableDataNodeContainedRule(tableName, database)) {
            database.getRuleMetaData().findRules(MutableDataNodeRule.class).forEach(each -> each.put(logicDataSourceNames.iterator().next(), schemaName, tableName));
        }
        SchemaBuilderMaterials materials = new SchemaBuilderMaterials(database.getProtocolType(),
                database.getResource().getDatabaseType(), database.getResource().getDataSources(), database.getRuleMetaData().getRules(), props, schemaName);
        Map<String, SchemaMetaData> metaDataMap = TableMetaDataBuilder.load(Collections.singletonList(tableName), materials);
        Optional<TableMetaData> actualTableMetaData = Optional.ofNullable(metaDataMap.get(schemaName)).map(optional -> optional.getTables().get(tableName));
        actualTableMetaData.ifPresent(optional -> {
            database.getSchemas().get(schemaName).put(tableName, optional);
            federationDatabaseMetaData.putTableMetadata(schemaName, optional);
            optimizerPlanners.put(federationDatabaseMetaData.getName(), OptimizerPlannerContextFactory.create(federationDatabaseMetaData));
        });
    }
    
    private boolean containsInImmutableDataNodeContainedRule(final String tableName, final ShardingSphereDatabase database) {
        return database.getRuleMetaData().findRules(DataNodeContainedRule.class).stream()
                .filter(each -> !(each instanceof MutableDataNodeRule)).anyMatch(each -> each.getAllTables().contains(tableName));
    }
    
    @Override
    public String getType() {
        return TYPE;
    }
}
