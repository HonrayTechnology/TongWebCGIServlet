package com.honray.tongweb.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

public class IOTools {
    protected static final int DEFAULT_BUFFER_SIZE = 4096;

    private IOTools() {
    }

    /**
     * 将reader的输入写入到writer中
     * @param reader input from
     * @param writer input write to
     * @param buf 字符数组buffer
     * @throws IOException
     */
    public static void flow(Reader reader, Writer writer, char[] buf) throws IOException {
        int numRead;
        while ((numRead = reader.read(buf)) >= 0) {
            writer.write(buf, 0, numRead);
        }
    }

    /**
     * 将reader的输入写入到writer中
     * @param reader
     * @param writer
     * @throws IOException
     */
    public static void flow(Reader reader, Writer writer) throws IOException {
        char[] buf = new char[DEFAULT_BUFFER_SIZE];
        com.honray.tongweb.servlet.IOTools.flow(reader, writer, buf);
    }

    /**
     * 将输入写入到输出
     * @param is 输入
     * @param os 输出
     * @param buf 字符数组buffer
     * @throws IOException
     */
    public static void flow(InputStream is, OutputStream os, byte[] buf) throws IOException {
        int numRead;
        while ((numRead = is.read(buf)) >= 0) {
            os.write(buf, 0, numRead);
        }
    }

    /**
     * 将输入写入到输出
     * @param is
     * @param os
     * @throws IOException
     */
    public static void flow(InputStream is, OutputStream os) throws IOException {
        byte[] buf = new byte[DEFAULT_BUFFER_SIZE];
        com.honray.tongweb.servlet.IOTools.flow(is, os, buf);
    }
}
