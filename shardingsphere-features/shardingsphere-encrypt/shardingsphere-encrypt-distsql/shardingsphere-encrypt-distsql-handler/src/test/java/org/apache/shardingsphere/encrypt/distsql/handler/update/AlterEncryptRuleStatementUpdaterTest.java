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

package org.apache.shardingsphere.encrypt.distsql.handler.update;

import org.apache.shardingsphere.distsql.parser.segment.AlgorithmSegment;
import org.apache.shardingsphere.encrypt.api.config.EncryptRuleConfiguration;
import org.apache.shardingsphere.encrypt.api.config.rule.EncryptTableRuleConfiguration;
import org.apache.shardingsphere.encrypt.distsql.parser.segment.EncryptColumnSegment;
import org.apache.shardingsphere.encrypt.distsql.parser.segment.EncryptRuleSegment;
import org.apache.shardingsphere.encrypt.distsql.parser.statement.AlterEncryptRuleStatement;
import org.apache.shardingsphere.infra.distsql.exception.DistSQLException;
import org.apache.shardingsphere.infra.distsql.exception.rule.InvalidAlgorithmConfigurationException;
import org.apache.shardingsphere.infra.distsql.exception.rule.InvalidRuleConfigurationException;
import org.apache.shardingsphere.infra.distsql.exception.rule.RequiredRuleMissedException;
import org.apache.shardingsphere.infra.metadata.ShardingSphereDatabase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.LinkedList;
import java.util.Properties;

@RunWith(MockitoJUnitRunner.class)
public final class AlterEncryptRuleStatementUpdaterTest {
    
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ShardingSphereDatabase database;
    
    private final AlterEncryptRuleStatementUpdater updater = new AlterEncryptRuleStatementUpdater();
    
    @Test(expected = RequiredRuleMissedException.class)
    public void assertCheckSQLStatementWithoutCurrentRule() throws DistSQLException {
        updater.checkSQLStatement(database, createSQLStatement("MD5"), null);
    }
    
    @Test(expected = RequiredRuleMissedException.class)
    public void assertCheckSQLStatementWithoutToBeAlteredRules() throws DistSQLException {
        updater.checkSQLStatement(database, createSQLStatement("MD5"), new EncryptRuleConfiguration(Collections.emptyList(), Collections.emptyMap()));
    }
    
    @Test(expected = InvalidAlgorithmConfigurationException.class)
    public void assertCheckSQLStatementWithoutToBeAlteredEncryptors() throws DistSQLException {
        updater.checkSQLStatement(database, createSQLStatement("INVALID_TYPE"), createCurrentRuleConfiguration());
    }
    
    @Test(expected = InvalidRuleConfigurationException.class)
    public void assertCheckSQLStatementWithIncompleteDataType() throws DistSQLException {
        EncryptColumnSegment columnSegment = new EncryptColumnSegment("user_id", "user_cipher", "user_plain", "assisted_column",
                "int varchar(10)", null, null, null, new AlgorithmSegment("test", new Properties()));
        EncryptRuleSegment ruleSegment = new EncryptRuleSegment("t_encrypt", Collections.singleton(columnSegment), null);
        AlterEncryptRuleStatement statement = new AlterEncryptRuleStatement(Collections.singleton(ruleSegment));
        updater.checkSQLStatement(database, statement, createCurrentRuleConfiguration());
    }
    
    private AlterEncryptRuleStatement createSQLStatement(final String encryptorName) {
        EncryptColumnSegment columnSegment = new EncryptColumnSegment("user_id", "user_cipher", "user_plain", "assisted_column", new AlgorithmSegment(encryptorName, new Properties()));
        EncryptRuleSegment ruleSegment = new EncryptRuleSegment("t_encrypt", Collections.singleton(columnSegment), null);
        return new AlterEncryptRuleStatement(Collections.singleton(ruleSegment));
    }
    
    private EncryptRuleConfiguration createCurrentRuleConfiguration() {
        EncryptTableRuleConfiguration tableRuleConfig = new EncryptTableRuleConfiguration("t_encrypt", Collections.emptyList(), null);
        return new EncryptRuleConfiguration(new LinkedList<>(Collections.singleton(tableRuleConfig)), Collections.emptyMap());
    }
}
