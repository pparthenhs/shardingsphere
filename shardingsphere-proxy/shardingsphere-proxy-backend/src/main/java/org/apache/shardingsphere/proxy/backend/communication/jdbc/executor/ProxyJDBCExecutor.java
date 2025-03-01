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

package org.apache.shardingsphere.proxy.backend.communication.jdbc.executor;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.shardingsphere.infra.binder.LogicSQL;
import org.apache.shardingsphere.infra.binder.statement.SQLStatementContext;
import org.apache.shardingsphere.infra.database.type.DatabaseType;
import org.apache.shardingsphere.infra.executor.kernel.model.ExecutionGroupContext;
import org.apache.shardingsphere.infra.executor.sql.execute.engine.driver.jdbc.JDBCExecutionUnit;
import org.apache.shardingsphere.infra.executor.sql.execute.engine.driver.jdbc.JDBCExecutor;
import org.apache.shardingsphere.infra.executor.sql.execute.result.ExecuteResult;
import org.apache.shardingsphere.infra.executor.sql.process.ExecuteProcessEngine;
import org.apache.shardingsphere.mode.metadata.MetaDataContexts;
import org.apache.shardingsphere.proxy.backend.communication.jdbc.JDBCDatabaseCommunicationEngine;
import org.apache.shardingsphere.proxy.backend.communication.jdbc.executor.callback.ProxyJDBCExecutorCallbackFactory;
import org.apache.shardingsphere.proxy.backend.context.ProxyContext;
import org.apache.shardingsphere.proxy.backend.session.ConnectionSession;

import java.sql.SQLException;
import java.util.List;

/**
 * Proxy JDBC executor.
 */
@RequiredArgsConstructor
public final class ProxyJDBCExecutor {
    
    private final String type;
    
    private final ConnectionSession connectionSession;
    
    private final JDBCDatabaseCommunicationEngine databaseCommunicationEngine;
    
    @Getter
    private final JDBCExecutor jdbcExecutor;
    
    /**
     * Execute.
     * 
     * @param logicSQL logic SQL
     * @param executionGroupContext execution group context
     * @param isReturnGeneratedKeys is return generated keys
     * @param isExceptionThrown is exception thrown
     * @return execute results
     * @throws SQLException SQL exception
     */
    public List<ExecuteResult> execute(final LogicSQL logicSQL, final ExecutionGroupContext<JDBCExecutionUnit> executionGroupContext,
                                       final boolean isReturnGeneratedKeys, final boolean isExceptionThrown) throws SQLException {
        try {
            MetaDataContexts metaDataContexts = ProxyContext.getInstance().getContextManager().getMetaDataContexts();
            DatabaseType databaseType = metaDataContexts.getDatabaseMetaData(connectionSession.getDatabaseName()).getResource().getDatabaseType();
            ExecuteProcessEngine.initialize(logicSQL, executionGroupContext, metaDataContexts.getProps());
            SQLStatementContext<?> context = logicSQL.getSqlStatementContext();
            List<ExecuteResult> result = jdbcExecutor.execute(executionGroupContext,
                    ProxyJDBCExecutorCallbackFactory.newInstance(type, databaseType, context.getSqlStatement(), databaseCommunicationEngine, isReturnGeneratedKeys, isExceptionThrown, true),
                    ProxyJDBCExecutorCallbackFactory.newInstance(type, databaseType, context.getSqlStatement(), databaseCommunicationEngine, isReturnGeneratedKeys, isExceptionThrown, false));
            ExecuteProcessEngine.finish(executionGroupContext.getExecutionID());
            return result;
        } finally {
            ExecuteProcessEngine.clean();
        }
    }
}
