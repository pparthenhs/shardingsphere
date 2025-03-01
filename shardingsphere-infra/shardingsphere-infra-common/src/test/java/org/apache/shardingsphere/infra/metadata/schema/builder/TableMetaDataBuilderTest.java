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

package org.apache.shardingsphere.infra.metadata.schema.builder;

import org.apache.shardingsphere.infra.config.props.ConfigurationProperties;
import org.apache.shardingsphere.infra.database.type.DatabaseType;
import org.apache.shardingsphere.infra.metadata.schema.fixture.rule.CommonFixtureRule;
import org.apache.shardingsphere.infra.metadata.schema.fixture.rule.DataNodeContainedFixtureRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public final class TableMetaDataBuilderTest {
    
    @Mock
    private DatabaseType databaseType;
    
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private DataSource dataSource;
    
    @Test
    public void assertLoadWithExistedTableName() throws SQLException {
        assertFalse(TableMetaDataBuilder.load(Collections.singletonList("data_node_routed_table1"), new SchemaBuilderMaterials(
                databaseType, databaseType, Collections.singletonMap("logic_db", dataSource), Arrays.asList(new CommonFixtureRule(), new DataNodeContainedFixtureRule()),
                new ConfigurationProperties(new Properties()), "sharding_db")).isEmpty());
    }
    
    @Test
    public void assertLoadWithNotExistedTableName() throws SQLException {
        assertTrue(TableMetaDataBuilder.load(Collections.singletonList("invalid_table"), new SchemaBuilderMaterials(
                databaseType, databaseType, Collections.singletonMap("logic_db", dataSource), Arrays.asList(new CommonFixtureRule(), new DataNodeContainedFixtureRule()),
                new ConfigurationProperties(new Properties()), "sharding_db")).isEmpty());
    }
    
}
