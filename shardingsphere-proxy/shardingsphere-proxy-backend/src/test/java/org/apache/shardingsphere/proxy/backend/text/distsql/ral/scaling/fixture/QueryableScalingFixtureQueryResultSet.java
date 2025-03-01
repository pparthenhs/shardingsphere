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

package org.apache.shardingsphere.proxy.backend.text.distsql.ral.scaling.fixture;

import org.apache.shardingsphere.infra.distsql.query.DistSQLResultSet;
import org.apache.shardingsphere.infra.metadata.ShardingSphereDatabase;
import org.apache.shardingsphere.sql.parser.sql.common.statement.SQLStatement;

import java.util.Collection;

public final class QueryableScalingFixtureQueryResultSet implements DistSQLResultSet {
    
    @Override
    public void init(final ShardingSphereDatabase database, final SQLStatement sqlStatement) {
    }
    
    @Override
    public Collection<String> getColumnNames() {
        return null;
    }
    
    @Override
    public boolean next() {
        return false;
    }
    
    @Override
    public Collection<Object> getRowData() {
        return null;
    }
    
    @Override
    public String getType() {
        return QueryableScalingFixtureRALStatement.class.getName();
    }
    
    @Override
    public Collection<String> getTypeAliases() {
        return DistSQLResultSet.super.getTypeAliases();
    }
}
