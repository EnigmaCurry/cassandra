package org.apache.cassandra.stress;

import com.google.common.io.ByteStreams;

import org.apache.cassandra.stress.settings.StressSettings;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public class StressGraph
{

    private StressSettings stressSettings;

    public StressGraph(StressSettings stressSetttings)
    {
        this.stressSettings = stressSetttings;
    }

    public void generateGraph()
    {
        File htmlFile = new File(stressSettings.graph.file);
        JSONObject stats;
        if (htmlFile.isFile())
        {
            //TODO: Load existing statistics to merge into new ones
            stats = new JSONObject();
        }
        else
        {
            stats = this.createJSONStats();
        }
        System.out.print(stats.toJSONString());
    }

    private String getGraphHTML()
    {
        InputStream graphHTMLRes = StressGraph.class.getClassLoader().getResourceAsStream("org/apache/cassandra/stress/graph/graph.html");
        String graphHTML;
        try
        {
            graphHTML = new String(ByteStreams.toByteArray(graphHTMLRes));
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        return graphHTML;
    }

    public JSONObject parseLogIntoIntervals(InputStream log) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(log));
        JSONObject json = new JSONObject();
        json.put("metrics", Arrays.asList(StressMetrics.HEADMETRICS));
        boolean readingMetrics = false;
        try
        {
            String line = reader.readLine();
            while (line != null) {
                if (line == StressMetrics.HEAD) {
                    readingMetrics = true;
                }
                line = reader.readLine();
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return json;
    }

    private JSONObject createJSONStats()
    {
        JSONObject json = new JSONObject();
        json.put("title", stressSettings.graph.title);
        JSONArray stats = new JSONArray();
        json.put("stats", stats);
        JSONObject intervals;

        try
        {
            intervals = parseLogIntoIntervals(new FileInputStream(stressSettings.graph.temporaryLogFile));
            json.put("metrics", intervals.get("metrics"));
        }
        catch (FileNotFoundException e)
        {
            throw new RuntimeException(e);
        }

        return json;
    }
}
