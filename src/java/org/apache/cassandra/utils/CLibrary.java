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
package org.apache.cassandra.utils;

import java.io.FileDescriptor;
import java.lang.reflect.Field;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.LastErrorException;
import com.sun.jna.Native;

public final class CLibrary
{
    private static final Logger logger = LoggerFactory.getLogger(CLibrary.class);

    private static final int MCL_CURRENT = 1;
    private static final int MCL_FUTURE = 2;

    private static final int ENOMEM = 12;

    private static final int F_GETFL   = 3;  /* get file status flags */
    private static final int F_SETFL   = 4;  /* set file status flags */
    private static final int F_NOCACHE = 48; /* Mac OS X specific flag, turns cache on/off */
    private static final int O_DIRECT  = 040000; /* fcntl.h */
    private static final int O_RDONLY  = 00000000; /* fcntl.h */

    /* fadvise.h */
    public static enum FileAdvice
    {
        NORMAL(0), RANDOM(1), SEQUENTIAL(2), WILL_NEED(3), DONT_NEED(4), NO_REUSE(5);

        private final int advice;

        FileAdvice(int advice)
        {
            this.advice = advice;
        }
    }

    static boolean jnaAvailable = true;
    static boolean jnaLockable = false;

    static
    {
        try
        {
            Native.register("c");
        }
        catch (NoClassDefFoundError e)
        {
            logger.warn("JNA not found. Native methods will be disabled.");
            jnaAvailable = false;
        }
        catch (UnsatisfiedLinkError e)
        {
            logger.warn("JNA link failure, one or more native method will be unavailable.");
            logger.debug("JNA link failure details: {}", e.getMessage());
        }
        catch (NoSuchMethodError e)
        {
            logger.warn("Obsolete version of JNA present; unable to register C library. Upgrade to JNA 3.2.7 or later");
            jnaAvailable = false;
        }
    }

    private static native int mlockall(int flags) throws LastErrorException;
    private static native int munlockall() throws LastErrorException;
    private static native int fcntl(int fd, int command, long flags) throws LastErrorException;
    private static native int posix_fadvise(int fd, long offset, int len, int flag) throws LastErrorException;
    private static native int open(String path, int flags) throws LastErrorException;
    private static native int fsync(int fd) throws LastErrorException;
    private static native int close(int fd) throws LastErrorException;

    private static int errno(RuntimeException e)
    {
        assert e instanceof LastErrorException;
        try
        {
            return ((LastErrorException) e).getErrorCode();
        }
        catch (NoSuchMethodError x)
        {
            logger.warn("Obsolete version of JNA present; unable to read errno. Upgrade to JNA 3.2.7 or later");
            return 0;
        }
    }

    private CLibrary() {}

    public static boolean jnaAvailable()
    {
        return jnaAvailable;
    }

    public static boolean jnaMemoryLockable()
    {
        return jnaLockable;
    }

    public static void tryMlockall()
    {
        try
        {
            mlockall(MCL_CURRENT);
            jnaLockable = true;
            logger.info("JNA mlockall successful");
        }
        catch (UnsatisfiedLinkError e)
        {
            // this will have already been logged by CLibrary, no need to repeat it
        }
        catch (RuntimeException e)
        {
            if (!(e instanceof LastErrorException))
                throw e;

            if (errno(e) == ENOMEM && System.getProperty("os.name").toLowerCase().contains("linux"))
            {
                logger.warn("Unable to lock JVM memory (ENOMEM)."
                        + " This can result in part of the JVM being swapped out, especially with mmapped I/O enabled."
                        + " Increase RLIMIT_MEMLOCK or run Cassandra as root.");
            }
            else if (!System.getProperty("os.name").toLowerCase().contains("mac"))
            {
                // OS X allows mlockall to be called, but always returns an error
                logger.warn("Unknown mlockall error {}", errno(e));
            }
        }
    }

    public static void trySkipCache(int fd, long offset, int len)
    {
        if (fd < 0)
            return;

        if (System.getProperty("os.name").toLowerCase().contains("linux"))
            fadvice(fd, offset, len, FileAdvice.DONT_NEED);
    }

    public static int tryFcntl(int fd, int command, int flags)
    {
        // fcntl return value may or may not be useful, depending on the command
        int result = -1;

        try
        {
            result = fcntl(fd, command, flags);
        }
        catch (UnsatisfiedLinkError e)
        {
            // if JNA is unavailable just skipping
        }
        catch (RuntimeException e)
        {
            if (!(e instanceof LastErrorException))
                throw e;

            logger.warn(String.format("fcntl(%d, %d, %d) failed, errno (%d).", fd, command, flags, errno(e)));
        }

        return result;
    }

    public static int tryOpenDirectory(String path)
    {
        int fd = -1;

        try
        {
            return open(path, O_RDONLY);
        }
        catch (UnsatisfiedLinkError e)
        {
            // JNA is unavailable just skipping Direct I/O
        }
        catch (RuntimeException e)
        {
            if (!(e instanceof LastErrorException))
                throw e;

            logger.warn(String.format("open(%s, O_RDONLY) failed, errno (%d).", path, errno(e)));
        }

        return fd;
    }

    public static void trySync(int fd)
    {
        if (fd == -1)
            return;

        try
        {
            fsync(fd);
        }
        catch (UnsatisfiedLinkError e)
        {
            // JNA is unavailable just skipping Direct I/O
        }
        catch (RuntimeException e)
        {
            if (!(e instanceof LastErrorException))
                throw e;

            logger.warn(String.format("fsync(%d) failed, errno (%d).", fd, errno(e)));
        }
    }

    public static void tryCloseFD(int fd)
    {
        if (fd == -1)
            return;

        try
        {
            close(fd);
        }
        catch (UnsatisfiedLinkError e)
        {
            // JNA is unavailable just skipping Direct I/O
        }
        catch (RuntimeException e)
        {
            if (!(e instanceof LastErrorException))
                throw e;

            logger.warn(String.format("close(%d) failed, errno (%d).", fd, errno(e)));
        }
    }

    /**
     * Get system file descriptor from FileDescriptor object.
     * @param descriptor - FileDescriptor objec to get fd from
     * @return file descriptor, -1 or error
     */
    public static int getfd(FileDescriptor descriptor)
    {
        Field field = FBUtilities.getProtectedField(descriptor.getClass(), "fd");

        if (field == null)
            return -1;

        try
        {
            return field.getInt(descriptor);
        }
        catch (Exception e)
        {
            logger.warn("unable to read fd field from FileDescriptor");
        }

        return -1;
    }

    public static int fadvice(int fd, long pos, int len, FileAdvice advice)
    {
        if (fd < 0)
            return -1;

        try
        {
            int res = posix_fadvise(fd, pos, len, advice.advice);
            if (res < 0)
                logger.warn(String.format("posix_fadvise(%d, %d, %d, %s) failed, return (%d).", fd, pos, len, advice, res));

            return res;
        }
        catch (UnsatisfiedLinkError e)
        {
            // JNA is unavailable just skipping
        }
        catch (RuntimeException e)
        {
            if (!(e instanceof LastErrorException))
                throw e;

            logger.warn(String.format("posix_fadvise(%d, %d, %d, %s) failed, errno (%d).", fd, pos, len, advice, errno(e)));
        }

        return -1;
    }

    /**
     * Suggest kernel to preheat one page for the given file.
     *
     * @param fd The file descriptor of file to preheat.
     * @param position The offset of the block.
     *
     * @return On success, zero is returned. On error, an error number is returned.
     */
    public static int preheatPage(int fd, long position)
    {
        return fadvice(fd, position, 4096, FileAdvice.WILL_NEED);
    }
}
