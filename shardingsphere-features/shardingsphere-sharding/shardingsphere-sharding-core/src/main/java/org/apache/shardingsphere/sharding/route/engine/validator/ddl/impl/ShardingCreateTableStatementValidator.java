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

package org.apache.shardingsphere.sharding.route.engine.validator.ddl.impl;

import org.apache.shardingsphere.infra.binder.statement.SQLStatementContext;
import org.apache.shardingsphere.infra.config.props.ConfigurationProperties;
import org.apache.shardingsphere.infra.exception.ShardingSphereException;
import org.apache.shardingsphere.infra.metadata.ShardingSphereDatabase;
import org.apache.shardingsphere.infra.metadata.schema.ShardingSphereSchema;
import org.apache.shardingsphere.infra.route.context.RouteContext;
import org.apache.shardingsphere.sharding.route.engine.validator.ddl.ShardingDDLStatementValidator;
import org.apache.shardingsphere.sharding.rule.ShardingRule;
import org.apache.shardingsphere.sql.parser.sql.common.statement.ddl.CreateTableStatement;
import org.apache.shardingsphere.sql.parser.sql.dialect.handler.ddl.CreateTableStatementHandler;

import java.util.Collections;
import java.util.List;

/**
 * Sharding create table statement validator.
 */
public final class ShardingCreateTableStatementValidator extends ShardingDDLStatementValidator<CreateTableStatement> {
    
    @Override
    public void preValidate(final ShardingRule shardingRule,
                            final SQLStatementContext<CreateTableStatement> sqlStatementContext, final List<Object> parameters, final ShardingSphereDatabase database) {
        if (!CreateTableStatementHandler.containsNotExistClause(sqlStatementContext.getSqlStatement())) {
            String defaultSchema = sqlStatementContext.getDatabaseType().getDefaultSchema(database.getName());
            ShardingSphereSchema schema = sqlStatementContext.getTablesContext().getSchemaName()
                    .map(optional -> database.getSchemas().get(optional)).orElseGet(() -> database.getSchemas().get(defaultSchema));
            validateTableNotExist(schema, Collections.singletonList(sqlStatementContext.getSqlStatement().getTable()));
        }
    }
    
    @Override
    public void postValidate(final ShardingRule shardingRule, final SQLStatementContext<CreateTableStatement> sqlStatementContext, final List<Object> parameters,
                             final ShardingSphereDatabase database, final ConfigurationProperties props, final RouteContext routeContext) {
        String primaryTable = sqlStatementContext.getSqlStatement().getTable().getTableName().getIdentifier().getValue();
        if (isRouteUnitDataNodeDifferentSize(shardingRule, routeContext, primaryTable)) {
            throw new ShardingSphereException("CREATE TABLE ... statement can not route correctly for tables %s.", sqlStatementContext.getTablesContext().getTableNames());
        }
    }
}
