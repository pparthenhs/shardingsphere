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

package org.apache.shardingsphere.proxy.backend.text.database;

import com.google.common.base.Strings;
import lombok.RequiredArgsConstructor;
import org.apache.shardingsphere.infra.executor.check.SQLCheckEngine;
import org.apache.shardingsphere.infra.metadata.ShardingSphereDatabase;
import org.apache.shardingsphere.infra.metadata.user.Grantee;
import org.apache.shardingsphere.infra.rule.ShardingSphereRule;
import org.apache.shardingsphere.proxy.backend.context.ProxyContext;
import org.apache.shardingsphere.proxy.backend.exception.DBDropNotExistsException;
import org.apache.shardingsphere.proxy.backend.exception.UnknownDatabaseException;
import org.apache.shardingsphere.proxy.backend.response.header.ResponseHeader;
import org.apache.shardingsphere.proxy.backend.response.header.update.UpdateResponseHeader;
import org.apache.shardingsphere.proxy.backend.session.ConnectionSession;
import org.apache.shardingsphere.proxy.backend.text.TextProtocolBackendHandler;
import org.apache.shardingsphere.sql.parser.sql.common.statement.ddl.DropDatabaseStatement;

import java.util.Collection;
import java.util.LinkedList;

/**
 * Drop database backend handler.
 */
@RequiredArgsConstructor
public final class DropDatabaseBackendHandler implements TextProtocolBackendHandler {
    
    private final DropDatabaseStatement sqlStatement;
    
    private final ConnectionSession connectionSession;
    
    @Override
    public ResponseHeader execute() {
        check(sqlStatement, connectionSession.getGrantee());
        if (isDropCurrentDatabase(sqlStatement.getDatabaseName())) {
            connectionSession.setCurrentDatabase(null);
        }
        ProxyContext.getInstance().getContextManager().deleteDatabase(sqlStatement.getDatabaseName());
        return new UpdateResponseHeader(sqlStatement);
    }
    
    private void check(final DropDatabaseStatement sqlStatement, final Grantee grantee) {
        String databaseName = sqlStatement.getDatabaseName();
        if (!SQLCheckEngine.check(databaseName, getRules(databaseName), grantee)) {
            throw new UnknownDatabaseException(databaseName);
        }
        if (!sqlStatement.isContainsExistClause() && !ProxyContext.getInstance().getAllDatabaseNames().contains(databaseName)) {
            throw new DBDropNotExistsException(databaseName);
        }
    }
    
    private boolean isDropCurrentDatabase(final String databaseName) {
        return !Strings.isNullOrEmpty(connectionSession.getDatabaseName()) && connectionSession.getDatabaseName().equals(databaseName);
    }
    
    private static Collection<ShardingSphereRule> getRules(final String schemaName) {
        Collection<ShardingSphereRule> result = new LinkedList<>();
        ShardingSphereDatabase database = ProxyContext.getInstance().getContextManager().getMetaDataContexts().getDatabaseMetaData(schemaName);
        if (null != database && null != database.getRuleMetaData()) {
            result.addAll(database.getRuleMetaData().getRules());
        }
        result.addAll(ProxyContext.getInstance().getContextManager().getMetaDataContexts().getGlobalRuleMetaData().getRules());
        return result;
    }
}
