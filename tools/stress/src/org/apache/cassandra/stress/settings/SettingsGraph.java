package org.apache.cassandra.stress.settings;
/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */


import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class SettingsGraph implements Serializable
{

    public final String file;
    public final String revision;
    public final File temporaryLogFile;

    public SettingsGraph(GraphOptions options)
    {
        file = options.file.value();
        revision = options.revision.value();
        if (inGraphMode()) {
            try {
                temporaryLogFile = File.createTempFile("cassandra-stress", ".log");
            } catch (IOException e) {
                throw new RuntimeException("Cannot open temporary file");
            }
        } else {
            temporaryLogFile = null;
        }
    }

    /** Are we in graphing mode?
     */
    public boolean inGraphMode() {
        return this.file == null ? false : true;
    }

    public void cleanup() {
        temporaryLogFile.delete();
    }

    // Option Declarations

    private static final class GraphOptions extends GroupedOptions
    {
        final OptionSimple file = new OptionSimple("file=", ".*", null, "HTML file to generate", true);
        final OptionSimple revision = new OptionSimple("revision=", ".*", "unknown", "Unique name to give to a collection of results in the graph", false);
        @Override
        public List<? extends Option> options()
        {
            return Arrays.asList(file, revision);
        }
    }

    // CLI Utility Methods

    public static SettingsGraph get(Map<String, String[]> clArgs)
    {
        String[] params = clArgs.remove("-graph");
        if (params == null)
        {
            return new SettingsGraph(new GraphOptions());
        }
        GraphOptions options = GroupedOptions.select(params, new GraphOptions());
        if (options == null)
        {
            printHelp();
            System.out.println("Invalid -graph options provided, see output for valid options");
            System.exit(1);
        }
        return new SettingsGraph(options);
    }

    public static void printHelp()
    {
        GroupedOptions.printOptions(System.out, "-graph", new GraphOptions());
    }

    public static Runnable helpPrinter()
    {
        return new Runnable()
        {
            @Override
            public void run()
            {
                printHelp();
            }
        };
    }
}

