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

package org.apache.shardingsphere.proxy.backend.context;

import org.apache.shardingsphere.infra.config.props.ConfigurationProperties;
import org.apache.shardingsphere.infra.database.type.dialect.H2DatabaseType;
import org.apache.shardingsphere.infra.federation.optimizer.context.OptimizerContext;
import org.apache.shardingsphere.infra.instance.InstanceContext;
import org.apache.shardingsphere.infra.metadata.ShardingSphereDatabase;
import org.apache.shardingsphere.infra.metadata.rule.ShardingSphereRuleMetaData;
import org.apache.shardingsphere.mode.manager.ContextManager;
import org.apache.shardingsphere.mode.metadata.MetaDataContexts;
import org.apache.shardingsphere.mode.metadata.persist.MetaDataPersistService;
import org.apache.shardingsphere.proxy.backend.exception.NoDatabaseSelectedException;
import org.apache.shardingsphere.proxy.backend.util.ProxyContextRestorer;
import org.apache.shardingsphere.transaction.context.TransactionContexts;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class ProxyContextTest extends ProxyContextRestorer {
    
    private static final String SCHEMA_PATTERN = "db_%s";
    
    @Test
    public void assertInit() {
        ProxyContext.init(new ContextManager(new MetaDataContexts(mock(MetaDataPersistService.class)), mock(TransactionContexts.class), mock(InstanceContext.class, RETURNS_DEEP_STUBS)));
        assertThat(ProxyContext.getInstance().getContextManager().getMetaDataContexts(), is(ProxyContext.getInstance().getContextManager().getMetaDataContexts()));
        assertThat(ProxyContext.getInstance().getContextManager().getTransactionContexts(), is(ProxyContext.getInstance().getContextManager().getTransactionContexts()));
        assertTrue(ProxyContext.getInstance().getStateContext().isPresent());
        assertThat(ProxyContext.getInstance().getStateContext(), is(ProxyContext.getInstance().getStateContext()));
    }
    
    @Test
    public void assertDatabaseExists() throws NoSuchFieldException, IllegalAccessException {
        Map<String, ShardingSphereDatabase> databaseMap = mockDatabaseMap();
        Field contextManagerField = ProxyContext.getInstance().getClass().getDeclaredField("contextManager");
        contextManagerField.setAccessible(true);
        ContextManager contextManager = mock(ContextManager.class, RETURNS_DEEP_STUBS);
        MetaDataContexts metaDataContexts = new MetaDataContexts(
                mock(MetaDataPersistService.class), databaseMap, mock(ShardingSphereRuleMetaData.class), mock(OptimizerContext.class), new ConfigurationProperties(new Properties()));
        when(contextManager.getMetaDataContexts()).thenReturn(metaDataContexts);
        contextManagerField.set(ProxyContext.getInstance(), contextManager);
        assertTrue(ProxyContext.getInstance().databaseExists("db"));
        assertFalse(ProxyContext.getInstance().databaseExists("db_1"));
    }
    
    @Test(expected = NoDatabaseSelectedException.class)
    public void assertGetDatabaseWithNull() {
        assertNull(ProxyContext.getInstance().getDatabase(null));
    }
    
    @Test(expected = NoDatabaseSelectedException.class)
    public void assertGetDatabaseWithEmptyString() {
        assertNull(ProxyContext.getInstance().getDatabase(""));
    }
    
    @Test(expected = NoDatabaseSelectedException.class)
    public void assertGetDatabaseWhenNotExisted() throws NoSuchFieldException, IllegalAccessException {
        Map<String, ShardingSphereDatabase> databaseMap = mockDatabaseMap();
        Field contextManagerField = ProxyContext.getInstance().getClass().getDeclaredField("contextManager");
        contextManagerField.setAccessible(true);
        ContextManager contextManager = mock(ContextManager.class, RETURNS_DEEP_STUBS);
        MetaDataContexts metaDataContexts = new MetaDataContexts(
                mock(MetaDataPersistService.class), databaseMap, mock(ShardingSphereRuleMetaData.class), mock(OptimizerContext.class), new ConfigurationProperties(new Properties()));
        when(contextManager.getMetaDataContexts()).thenReturn(metaDataContexts);
        contextManagerField.set(ProxyContext.getInstance(), contextManager);
        ProxyContext.getInstance().getDatabase("db1");
    }
    
    @Test
    public void assertGetDatabase() throws NoSuchFieldException, IllegalAccessException {
        Map<String, ShardingSphereDatabase> databaseMap = mockDatabaseMap();
        Field contextManagerField = ProxyContext.getInstance().getClass().getDeclaredField("contextManager");
        contextManagerField.setAccessible(true);
        ContextManager contextManager = mock(ContextManager.class, RETURNS_DEEP_STUBS);
        MetaDataContexts metaDataContexts = new MetaDataContexts(
                mock(MetaDataPersistService.class), databaseMap, mock(ShardingSphereRuleMetaData.class), mock(OptimizerContext.class), new ConfigurationProperties(new Properties()));
        when(contextManager.getMetaDataContexts()).thenReturn(metaDataContexts);
        contextManagerField.set(ProxyContext.getInstance(), contextManager);
        assertThat(databaseMap.get("db"), is(ProxyContext.getInstance().getDatabase("db")));
    }
    
    @Test
    public void assertGetAllDatabaseNames() throws NoSuchFieldException, IllegalAccessException {
        Map<String, ShardingSphereDatabase> databaseMap = createDatabaseMap();
        Field contextManagerField = ProxyContext.getInstance().getClass().getDeclaredField("contextManager");
        contextManagerField.setAccessible(true);
        ContextManager contextManager = mock(ContextManager.class, RETURNS_DEEP_STUBS);
        MetaDataContexts metaDataContexts = new MetaDataContexts(
                mock(MetaDataPersistService.class), databaseMap, mock(ShardingSphereRuleMetaData.class), mock(OptimizerContext.class), new ConfigurationProperties(new Properties()));
        when(contextManager.getMetaDataContexts()).thenReturn(metaDataContexts);
        contextManagerField.set(ProxyContext.getInstance(), contextManager);
        assertThat(new LinkedHashSet<>(ProxyContext.getInstance().getAllDatabaseNames()), is(databaseMap.keySet()));
    }
    
    private Map<String, ShardingSphereDatabase> createDatabaseMap() {
        Map<String, ShardingSphereDatabase> result = new LinkedHashMap<>(10, 1);
        for (int i = 0; i < 10; i++) {
            result.put(String.format(SCHEMA_PATTERN, i), mock(ShardingSphereDatabase.class));
        }
        return result;
    }
    
    private Map<String, ShardingSphereDatabase> mockDatabaseMap() {
        ShardingSphereDatabase result = mock(ShardingSphereDatabase.class, RETURNS_DEEP_STUBS);
        when(result.getResource().getDatabaseType()).thenReturn(new H2DatabaseType());
        return Collections.singletonMap("db", result);
    }
}
