package org.apache.cassandra.stress.util;
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


import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/** PrintStream that multiplexes to multiple streams */
public class MultiPrintStream extends PrintStream {
    private List<PrintStream> newStreams;

    public MultiPrintStream(PrintStream baseStream) {
        super(baseStream);
        this.newStreams = new ArrayList();
    }

    public MultiPrintStream(PrintStream baseStream, List<PrintStream> newStreams) {
        super(baseStream);
        this.newStreams = newStreams;
    }

    @Override
    public void flush() {
        super.flush();
        for (PrintStream s : newStreams)
            s.flush();
    }

    @Override
    public void close() {
        super.close();
        for (PrintStream s : newStreams)
            s.close();
    }

    @Override
    public boolean checkError() {
        boolean error = super.checkError();
        for (PrintStream s : newStreams) {
            if (s.checkError())
                error = true;
        }
        return error;
    }

    @Override
    public void write(int b) {
        super.write(b);
        for (PrintStream s: newStreams)
            s.write(b);
    }

    @Override
    public void write(byte[] buf, int off, int len) {
        super.write(buf, off, len);
        for (PrintStream s: newStreams)
            s.write(buf, off, len);
    }

    public void addStream(PrintStream printStream) {
        newStreams.add(printStream);
    }
}
