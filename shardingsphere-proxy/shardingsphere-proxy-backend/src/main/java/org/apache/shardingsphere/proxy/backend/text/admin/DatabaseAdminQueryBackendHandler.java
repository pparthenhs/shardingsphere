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

package org.apache.shardingsphere.proxy.backend.text.admin;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.concurrent.LazyInitializer;
import org.apache.shardingsphere.infra.database.type.DatabaseType;
import org.apache.shardingsphere.infra.executor.sql.execute.result.query.QueryResultMetaData;
import org.apache.shardingsphere.infra.merge.result.MergedResult;
import org.apache.shardingsphere.infra.metadata.ShardingSphereDatabase;
import org.apache.shardingsphere.infra.rule.identifier.type.DataNodeContainedRule;
import org.apache.shardingsphere.proxy.backend.context.ProxyContext;
import org.apache.shardingsphere.proxy.backend.response.header.ResponseHeader;
import org.apache.shardingsphere.proxy.backend.response.header.query.QueryHeaderBuilderEngine;
import org.apache.shardingsphere.proxy.backend.response.header.query.QueryResponseHeader;
import org.apache.shardingsphere.proxy.backend.response.header.query.QueryHeader;
import org.apache.shardingsphere.proxy.backend.session.ConnectionSession;
import org.apache.shardingsphere.proxy.backend.text.TextProtocolBackendHandler;
import org.apache.shardingsphere.proxy.backend.text.admin.executor.DatabaseAdminQueryExecutor;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Database admin query backend handler.
 */
@RequiredArgsConstructor
public final class DatabaseAdminQueryBackendHandler implements TextProtocolBackendHandler {
    
    private final ConnectionSession connectionSession;
    
    private final DatabaseAdminQueryExecutor executor;
    
    private QueryResultMetaData queryResultMetaData;
    
    private MergedResult mergedResult;
    
    @Override
    public ResponseHeader execute() throws SQLException {
        executor.execute(connectionSession);
        queryResultMetaData = executor.getQueryResultMetaData();
        mergedResult = executor.getMergedResult();
        return new QueryResponseHeader(createResponseHeader());
    }
    
    private List<QueryHeader> createResponseHeader() throws SQLException {
        List<QueryHeader> result = new ArrayList<>(queryResultMetaData.getColumnCount());
        ShardingSphereDatabase database = null == connectionSession.getDatabaseName() ? null : ProxyContext.getInstance().getDatabase(connectionSession.getDatabaseName());
        DatabaseType databaseType = null == database ? connectionSession.getDatabaseType()
                : ProxyContext.getInstance().getContextManager().getMetaDataContexts().getDatabaseMetaData(database.getName()).getResource().getDatabaseType();
        QueryHeaderBuilderEngine queryHeaderBuilderEngine = new QueryHeaderBuilderEngine(databaseType);
        LazyInitializer<DataNodeContainedRule> dataNodeContainedRule = getDataNodeContainedRuleLazyInitializer(database);
        for (int columnIndex = 1; columnIndex <= queryResultMetaData.getColumnCount(); columnIndex++) {
            result.add(queryHeaderBuilderEngine.build(queryResultMetaData, database, columnIndex, dataNodeContainedRule));
        }
        return result;
    }
    
    private LazyInitializer<DataNodeContainedRule> getDataNodeContainedRuleLazyInitializer(final ShardingSphereDatabase database) {
        return new LazyInitializer<DataNodeContainedRule>() {
            
            @Override
            protected DataNodeContainedRule initialize() {
                return null == database ? null : database.getRuleMetaData().findSingleRule(DataNodeContainedRule.class).orElse(null);
            }
        };
    }
    
    @Override
    public boolean next() throws SQLException {
        return mergedResult.next();
    }
    
    @Override
    public Collection<Object> getRowData() throws SQLException {
        Collection<Object> result = new LinkedList<>();
        for (int columnIndex = 1; columnIndex <= queryResultMetaData.getColumnCount(); columnIndex++) {
            result.add(mergedResult.getValue(columnIndex, Object.class));
        }
        return result;
    }
}
