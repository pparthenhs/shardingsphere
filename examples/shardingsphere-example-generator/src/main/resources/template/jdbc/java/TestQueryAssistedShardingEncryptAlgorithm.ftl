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
<#assign package = feature?replace('-', '')?replace(',', '.') />

package org.apache.shardingsphere.example.${package}.${framework?replace('-', '.')};

import lombok.Getter;
import lombok.Setter;
import org.apache.shardingsphere.encrypt.spi.QueryAssistedEncryptAlgorithm;
import org.apache.shardingsphere.encrypt.spi.context.EncryptContext;

import java.util.Properties;

public final class TestQueryAssistedShardingEncryptAlgorithm implements QueryAssistedEncryptAlgorithm<Object, String> {

    @Getter
    @Setter
    private Properties props;
    
    @Override
    public String encrypt(final Object plainValue, final EncryptContext encryptContext) {
        return "encryptValue";
    }
    
    @Override
    public Object decrypt(final String cipherValue, final EncryptContext encryptContext) {
        return "decryptValue";
    }
    
    @Override
    public String queryAssistedEncrypt(final Object plainValue, final EncryptContext encryptContext) {
        return "assistedEncryptValue";
    }
    
    @Override
    public String getType() {
        return "assistedTest";
    }
}
