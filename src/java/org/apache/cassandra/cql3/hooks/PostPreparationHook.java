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
package org.apache.cassandra.cql3.hooks;

import org.apache.cassandra.cql3.CQLStatement;
import org.apache.cassandra.exceptions.RequestValidationException;

/**
 * Run directly after a CQL Statement is prepared in
 * {@link org.apache.cassandra.cql3.QueryProcessor}.
 */
public interface PostPreparationHook
{
    /**
     * Called in QueryProcessor, once a CQL statement has been prepared.
     *
     * @param statement the statement to perform additional processing on
     * @param context preparation context containing additional info
     *                about the operation and statement
     * @throws RequestValidationException
     */
    void processStatement(CQLStatement statement, PreparationContext context) throws RequestValidationException;
}
