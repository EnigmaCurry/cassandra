/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.cassandra.cql3.statements;

import org.apache.cassandra.auth.Permission;
import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.config.ColumnDefinition;
import org.apache.cassandra.config.KSMetaData;
import org.apache.cassandra.config.Schema;
import org.apache.cassandra.cql3.*;
import org.apache.cassandra.exceptions.*;
import org.apache.cassandra.service.ClientState;
import org.apache.cassandra.service.MigrationManager;
import org.apache.cassandra.transport.messages.ResultMessage;

public class DropIndexStatement extends SchemaAlteringStatement
{
    public final String indexName;
    public final boolean ifExists;

    public DropIndexStatement(String indexName, boolean ifExists)
    {
        super(new CFName());
        this.indexName = indexName;
        this.ifExists = ifExists;
    }

    public void checkAccess(ClientState state) throws UnauthorizedException, InvalidRequestException
    {
        CFMetaData cfm = findIndexedCF();
        if (cfm == null)
            return;

        state.hasColumnFamilyAccess(cfm.ksName, cfm.cfName, Permission.ALTER);
    }

    public ResultMessage.SchemaChange.Change changeType()
    {
        // Dropping an index is akin to updating the CF
        return ResultMessage.SchemaChange.Change.UPDATED;
    }

    public void announceMigration() throws InvalidRequestException, ConfigurationException
    {
        CFMetaData cfm = findIndexedCF();
        if (cfm == null)
            return;

        CFMetaData updatedCfm = updateCFMetadata(cfm);
        MigrationManager.announceColumnFamilyUpdate(updatedCfm, false);
    }

    private CFMetaData updateCFMetadata(CFMetaData cfm)
    {
        ColumnDefinition column = findIndexedColumn(cfm);
        assert column != null;
        CFMetaData cloned = cfm.clone();
        ColumnDefinition toChange = cloned.getColumnDefinition(column.name);
        assert toChange.getIndexName() != null && toChange.getIndexName().equals(indexName);
        toChange.setIndexName(null);
        toChange.setIndexType(null, null);
        return cloned;
    }

    private CFMetaData findIndexedCF() throws InvalidRequestException
    {
        KSMetaData ksm = Schema.instance.getKSMetaData(keyspace());
        for (CFMetaData cfm : ksm.cfMetaData().values())
        {
            if (findIndexedColumn(cfm) != null)
                return cfm;
        }

        if (ifExists)
            return null;
        else
            throw new InvalidRequestException("Index '" + indexName + "' could not be found in any of the column families of keyspace '" + keyspace() + "'");
    }

    private ColumnDefinition findIndexedColumn(CFMetaData cfm)
    {
        for (ColumnDefinition column : cfm.allColumns())
        {
            if (column.getIndexType() != null && column.getIndexName() != null && column.getIndexName().equals(indexName))
                return column;
        }
        return null;
    }
}
