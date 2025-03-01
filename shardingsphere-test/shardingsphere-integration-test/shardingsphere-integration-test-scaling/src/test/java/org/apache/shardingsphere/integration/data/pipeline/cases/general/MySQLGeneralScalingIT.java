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

package org.apache.shardingsphere.integration.data.pipeline.cases.general;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.shardingsphere.infra.database.type.dialect.MySQLDatabaseType;
import org.apache.shardingsphere.integration.data.pipeline.cases.base.BaseExtraSQLITCase;
import org.apache.shardingsphere.integration.data.pipeline.cases.task.MySQLIncrementTask;
import org.apache.shardingsphere.integration.data.pipeline.env.IntegrationTestEnvironment;
import org.apache.shardingsphere.integration.data.pipeline.framework.helper.ScalingCaseHelper;
import org.apache.shardingsphere.integration.data.pipeline.framework.param.ScalingParameterized;
import org.apache.shardingsphere.sharding.algorithm.keygen.SnowflakeKeyGenerateAlgorithm;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * General scaling test case, includes multiple cases.
 */
@Slf4j
@RunWith(Parameterized.class)
public final class MySQLGeneralScalingIT extends BaseExtraSQLITCase {
    
    private static final IntegrationTestEnvironment ENV = IntegrationTestEnvironment.getInstance();
    
    private final ScalingParameterized parameterized;
    
    public MySQLGeneralScalingIT(final ScalingParameterized parameterized) {
        super(parameterized);
        this.parameterized = parameterized;
        log.info("parameterized:{}", parameterized);
    }
    
    @Parameters(name = "{0}")
    public static Collection<ScalingParameterized> getParameters() {
        Collection<ScalingParameterized> result = new LinkedList<>();
        for (String version : ENV.getMysqlVersions()) {
            result.add(new ScalingParameterized(new MySQLDatabaseType(), version, "env/scenario/general/mysql.xml"));
        }
        return result;
    }
    
    @Test
    public void assertManualScalingSuccess() throws InterruptedException {
        addSourceResource();
        initShardingAlgorithm();
        assertTrue(waitShardingAlgorithmEffect(15));
        createScalingRule();
        createAllSharingTableRule();
        createNoUseTable();
        createOrderTable();
        createOrderItemTable();
        SnowflakeKeyGenerateAlgorithm keyGenerateAlgorithm = new SnowflakeKeyGenerateAlgorithm();
        Pair<List<Object[]>, List<Object[]>> dataPair = ScalingCaseHelper.generateFullInsertData(keyGenerateAlgorithm, parameterized.getDatabaseType(), 3000);
        getJdbcTemplate().batchUpdate(getExtraSQLCommand().getFullInsertOrder(), dataPair.getLeft());
        getJdbcTemplate().batchUpdate(getExtraSQLCommand().getFullInsertOrderItem(), dataPair.getRight());
        startIncrementTask(new MySQLIncrementTask(getJdbcTemplate(), keyGenerateAlgorithm, true));
        assertOriginalSourceSuccess();
        addTargetResource();
        getJdbcTemplate().execute(getCommonSQLCommand().getAutoAlterOrderWithItemShardingTableRule());
        assertCheckMatchConsistencySuccess();
    }
}
