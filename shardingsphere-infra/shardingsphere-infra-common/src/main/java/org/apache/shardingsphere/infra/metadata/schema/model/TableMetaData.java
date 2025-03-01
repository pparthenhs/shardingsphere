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

package org.apache.shardingsphere.infra.metadata.schema.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Table meta data.
 */
@Getter
@EqualsAndHashCode
@ToString
public final class TableMetaData {
    
    private final String name;
    
    private final Map<String, ColumnMetaData> columns;
    
    private final Map<String, IndexMetaData> indexes;
    
    private final Map<String, ConstraintMetaData> constrains;
    
    private final List<String> columnNames = new ArrayList<>();
    
    private final List<String> primaryKeyColumns = new ArrayList<>();
    
    public TableMetaData() {
        this("", Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
    }
    
    public TableMetaData(final String name, final Collection<ColumnMetaData> columnMetaDataList,
                         final Collection<IndexMetaData> indexMetaDataList, final Collection<ConstraintMetaData> constraintMetaDataList) {
        this.name = name;
        columns = getColumns(columnMetaDataList);
        indexes = getIndexes(indexMetaDataList);
        constrains = getConstrains(constraintMetaDataList);
    }
    
    private Map<String, ColumnMetaData> getColumns(final Collection<ColumnMetaData> columnMetaDataList) {
        Map<String, ColumnMetaData> result = new LinkedHashMap<>(columnMetaDataList.size(), 1);
        for (ColumnMetaData each : columnMetaDataList) {
            String lowerColumnName = each.getName().toLowerCase();
            result.put(lowerColumnName, each);
            columnNames.add(each.getName());
            if (each.isPrimaryKey()) {
                primaryKeyColumns.add(lowerColumnName);
            }
        }
        return result;
    }
    
    private Map<String, IndexMetaData> getIndexes(final Collection<IndexMetaData> indexMetaDataList) {
        Map<String, IndexMetaData> result = new LinkedHashMap<>(indexMetaDataList.size(), 1);
        for (IndexMetaData each : indexMetaDataList) {
            result.put(each.getName().toLowerCase(), each);
        }
        return result;
    }
    
    private Map<String, ConstraintMetaData> getConstrains(final Collection<ConstraintMetaData> constraintMetaDataList) {
        Map<String, ConstraintMetaData> result = new LinkedHashMap<>(constraintMetaDataList.size(), 1);
        for (ConstraintMetaData each : constraintMetaDataList) {
            result.put(each.getName().toLowerCase(), each);
        }
        return result;
    }
}
