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

package org.apache.shardingsphere.infra.binder.segment.table;

import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.ToString;
import org.apache.shardingsphere.infra.binder.segment.select.projection.impl.ColumnProjection;
import org.apache.shardingsphere.infra.binder.segment.select.subquery.SubqueryTableContext;
import org.apache.shardingsphere.infra.binder.segment.select.subquery.engine.SubqueryTableContextEngine;
import org.apache.shardingsphere.infra.binder.statement.dml.SelectStatementContext;
import org.apache.shardingsphere.infra.metadata.schema.ShardingSphereSchema;
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.column.ColumnSegment;
import org.apache.shardingsphere.sql.parser.sql.common.segment.generic.OwnerSegment;
import org.apache.shardingsphere.sql.parser.sql.common.segment.generic.table.SimpleTableSegment;
import org.apache.shardingsphere.sql.parser.sql.common.segment.generic.table.SubqueryTableSegment;
import org.apache.shardingsphere.sql.parser.sql.common.segment.generic.table.TableSegment;
import org.apache.shardingsphere.sql.parser.sql.common.value.identifier.IdentifierValue;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Tables context.
 */
@Getter
@ToString
public final class TablesContext {
    
    private final Collection<SimpleTableSegment> tables = new LinkedList<>();
    
    private final Collection<String> tableNames = new HashSet<>();
    
    private final Collection<String> schemaNames = new HashSet<>();
    
    private final Map<String, Collection<SubqueryTableContext>> subqueryTables = new HashMap<>();
    
    public TablesContext(final SimpleTableSegment tableSegment) {
        this(Collections.singletonList(tableSegment));
    }
    
    public TablesContext(final Collection<SimpleTableSegment> tableSegments) {
        this(tableSegments, Collections.emptyMap());
    }
    
    public TablesContext(final Collection<? extends TableSegment> tableSegments, final Map<Integer, SelectStatementContext> subqueryContexts) {
        if (tableSegments.isEmpty()) {
            return;
        }
        for (TableSegment each : tableSegments) {
            if (each instanceof SimpleTableSegment) {
                SimpleTableSegment simpleTableSegment = (SimpleTableSegment) each;
                tables.add(simpleTableSegment);
                tableNames.add(simpleTableSegment.getTableName().getIdentifier().getValue());
                simpleTableSegment.getOwner().ifPresent(owner -> schemaNames.add(owner.getIdentifier().getValue()));    
            }
            if (each instanceof SubqueryTableSegment) {
                subqueryTables.putAll(createSubqueryTables(subqueryContexts, (SubqueryTableSegment) each));
            }
        }
    }
    
    private Map<String, Collection<SubqueryTableContext>> createSubqueryTables(final Map<Integer, SelectStatementContext> subqueryContexts, final SubqueryTableSegment subqueryTable) {
        SelectStatementContext subqueryContext = subqueryContexts.get(subqueryTable.getSubquery().getStartIndex());
        Collection<SubqueryTableContext> subqueryTableContexts = new SubqueryTableContextEngine().createSubqueryTableContexts(subqueryContext, subqueryTable.getAlias().orElse(null));
        Map<String, Collection<SubqueryTableContext>> result = new HashMap<>();
        for (SubqueryTableContext subQuery : subqueryTableContexts) {
            if (null != subQuery.getAlias()) {
                result.computeIfAbsent(subQuery.getAlias(), unused -> new LinkedList<>()).add(subQuery);
            }
        }
        return result;
    }
    
    /**
     * Get table names.
     * 
     * @return table names
     */
    public Collection<String> getTableNames() {
        return tableNames;
    }
    
    /**
     * Find expression table name map by column segment.
     *
     * @param columns column segment collection
     * @param schema schema meta data
     * @return expression table name map
     */
    public Map<String, String> findTableNamesByColumnSegment(final Collection<ColumnSegment> columns, final ShardingSphereSchema schema) {
        if (1 == tables.size()) {
            return findTableNameFromSingleTable(columns);
        }
        Map<String, String> result = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        result.putAll(findTableNameFromSQL(columns));
        result.putAll(findTableNameFromMetaData(columns, schema));
        result.putAll(findTableNameFromSubquery(columns, result));
        return result;
    }
    
    /**
     * Find expression table name map by column projection.
     *
     * @param columns column segment collection
     * @param schema schema meta data
     * @return expression table name map
     */
    public Map<String, String> findTableNamesByColumnProjection(final Collection<ColumnProjection> columns, final ShardingSphereSchema schema) {
        Collection<ColumnSegment> result = new LinkedList<>();
        for (ColumnProjection each : columns) {
            ColumnSegment columnSegment = new ColumnSegment(0, 0, new IdentifierValue(each.getName()));
            if (null != each.getOwner()) {
                columnSegment.setOwner(new OwnerSegment(0, 0, new IdentifierValue(each.getOwner())));
            }
            result.add(columnSegment);
        }
        return findTableNamesByColumnSegment(result, schema);
    }
    
    private ColumnSegment createColumnSegment(final ColumnProjection projection) {
        ColumnSegment result = new ColumnSegment(0, 0, new IdentifierValue(projection.getName()));
        if (null != projection.getOwner()) {
            result.setOwner(new OwnerSegment(0, 0, new IdentifierValue(projection.getOwner())));
        }
        return result;
    }
    
    private Map<String, String> findTableNameFromSubquery(final Collection<ColumnSegment> columns, final Map<String, String> ownerTableNames) {
        if (ownerTableNames.size() == columns.size() || subqueryTables.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> result = new LinkedHashMap<>(columns.size(), 1);
        for (ColumnSegment each : columns) {
            if (ownerTableNames.containsKey(each.getExpression())) {
                continue;
            }
            String owner = each.getOwner().map(optional -> optional.getIdentifier().getValue()).orElse("");
            Collection<SubqueryTableContext> subqueryTableContexts = subqueryTables.getOrDefault(owner, Collections.emptyList());
            for (SubqueryTableContext subqueryTableContext : subqueryTableContexts) {
                if (subqueryTableContext.getColumnNames().contains(each.getIdentifier().getValue())) {
                    result.put(each.getExpression(), subqueryTableContext.getTableName());
                }
            }
        }
        return result;
    }
    
    private Map<String, String> findTableNameFromSingleTable(final Collection<ColumnSegment> columns) {
        String tableName = tables.iterator().next().getTableName().getIdentifier().getValue();
        Map<String, String> result = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (ColumnSegment each : columns) {
            result.putIfAbsent(each.getExpression(), tableName);
        }
        return result;
    }
    
    private Map<String, Collection<String>> getOwnerColumnNames(final Collection<ColumnSegment> columns) {
        Map<String, Collection<String>> result = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (ColumnSegment each : columns) {
            if (!each.getOwner().isPresent()) {
                continue;
            }
            result.computeIfAbsent(each.getOwner().get().getIdentifier().getValue(), unused -> new LinkedList<>()).add(each.getExpression());
        }
        return result;
    }
    
    private Map<String, String> findTableNameFromSQL(final Collection<ColumnSegment> columns) {
        Map<String, Collection<String>> ownerColumnNames = getOwnerColumnNames(columns);
        if (ownerColumnNames.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> result = new LinkedHashMap<>(columns.size(), 1);
        for (SimpleTableSegment each : tables) {
            String tableName = each.getTableName().getIdentifier().getValue();
            if (ownerColumnNames.containsKey(tableName)) {
                ownerColumnNames.get(tableName).forEach(column -> result.put(column, tableName));
            }
            Optional<String> alias = each.getAlias();
            if (alias.isPresent() && ownerColumnNames.containsKey(alias.get())) {
                ownerColumnNames.get(alias.get()).forEach(column -> result.put(column, tableName));
            }
        }
        return result;
    }
    
    private Map<String, String> findTableNameFromMetaData(final Collection<ColumnSegment> columns, final ShardingSphereSchema schema) {
        Collection<String> noOwnerColumnNames = getNoOwnerColumnNames(columns);
        if (noOwnerColumnNames.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> result = new LinkedHashMap<>(noOwnerColumnNames.size(), 1);
        for (SimpleTableSegment each : tables) {
            String tableName = each.getTableName().getIdentifier().getValue();
            for (String columnName : schema.getAllColumnNames(tableName)) {
                if (noOwnerColumnNames.contains(columnName)) {
                    result.put(columnName, tableName);
                }
            }
        }
        return result;
    }
    
    private Collection<String> getNoOwnerColumnNames(final Collection<ColumnSegment> columns) {
        Collection<String> result = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (ColumnSegment each : columns) {
            if (!each.getOwner().isPresent()) {
                result.add(each.getIdentifier().getValue());
            }
        }
        return result;
    }
    
    /**
     * Get schema name.
     *
     * @return schema name
     */
    public Optional<String> getSchemaName() {
        Preconditions.checkState(schemaNames.size() <= 1, "Can not support multiple different schema.");
        for (String each : schemaNames) {
            return Optional.of(each);
        }
        return Optional.empty();
    }
}
