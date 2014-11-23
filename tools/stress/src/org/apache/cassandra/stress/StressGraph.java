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
import java.io.PrintWriter;
import java.util.Arrays;

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
    private enum readingMode {NONE, METRICS, AGGREGATES};

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
            stats = this.createJSONStats(null);
        }
        System.out.println(stats);
        try
        {
            PrintWriter out = new PrintWriter(htmlFile);
            String statsBlock = "/* stats start */\nstats = " + stats.toJSONString() + ";\n/* stats end */\n";
            String html = getGraphHTML().replaceFirst("/\\* stats start \\*/\n\n/\\* stats end \\*/\n", statsBlock);
            out.write(html);
            out.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
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

    private JSONObject parseLogStats(InputStream log) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(log));
        JSONObject json = new JSONObject();
        JSONArray intervals = new JSONArray();
        json.put("metrics", Arrays.asList(StressMetrics.HEADMETRICS));
        json.put("test", stressSettings.command.type.name());
        json.put("revision", stressSettings.graph.revision);
        
        readingMode mode = readingMode.NONE;
        try
        {
            String line;
            while (true)
            {
                //Read the next line, break if null:
                line = reader.readLine();
                if (line == null)
                {
                    break;
                }

                //Detect mode changes:
                if (line.equals(StressMetrics.HEAD))
                {
                    mode = readingMode.METRICS;
                    continue;
                }
                else if (line.equals("Results:"))
                {
                    mode = readingMode.AGGREGATES;
                    continue;
                }
                else if (line == "END")
                {
                    mode = readingMode.NONE;
                    break;
                }

                //Process lines:
                if (mode == readingMode.METRICS)
                {
                    JSONArray metrics = new JSONArray();
                    String[] parts = line.split(",");
                    if (parts.length != StressMetrics.HEADMETRICS.length){
                        continue;
                    }
                    for (String m : parts)
                    {
                        metrics.add(m.trim());
                    }
                    intervals.add(metrics);
                }
                else if (mode == readingMode.AGGREGATES)
                {

                }
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        json.put("intervals", intervals);
        return json;
    }

    private JSONObject createJSONStats(JSONObject json)
    {
        JSONArray stats;
        JSONObject parsedMetrics;
        if(json == null){
            json = new JSONObject();
            stats = new JSONArray();
        } else {
            stats = (JSONArray) json.get("stats");
        }

        try
        {
            stats.add(parseLogStats(new FileInputStream(stressSettings.graph.temporaryLogFile)));
        }
        catch (FileNotFoundException e)
        {
            throw new RuntimeException(e);
        }

        json.put("title", stressSettings.graph.title);
        json.put("stats", stats);
        return json;
    }
}
