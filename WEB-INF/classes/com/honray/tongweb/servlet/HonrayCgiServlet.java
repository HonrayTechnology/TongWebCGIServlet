package com.honray.tongweb.servlet;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public final class HonrayCgiServlet
        extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private int debug = 0;
    private String cgiPathPrefix = null;
    private String cgiExecutable = "perl";
    private List<String> cgiExecutableArgs = null;
    private String parameterEncoding = System.getProperty("file.encoding", "UTF-8");
    private long stderrTimeout = 2000L;
    static Object expandFileLock = new Object();
    static Hashtable<String, String> shellEnv = new Hashtable();

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        if (this.getServletConfig().getInitParameter("debug") != null) {
            this.debug = Integer.parseInt(this.getServletConfig().getInitParameter("debug"));
        }
        this.cgiPathPrefix = this.getServletConfig().getInitParameter("cgiPathPrefix");
        boolean passShellEnvironment = Boolean.valueOf(this.getServletConfig().getInitParameter("passShellEnvironment"));
        if (passShellEnvironment) {
            shellEnv.putAll(System.getenv());
        }
        if (this.getServletConfig().getInitParameter("executable") != null) {
            this.cgiExecutable = this.getServletConfig().getInitParameter("executable");
        }
        if (this.getServletConfig().getInitParameter("executable-arg-1") != null) {
            String arg;
            ArrayList<String> args = new ArrayList<String>();
            int i = 1;
            while ((arg = this.getServletConfig().getInitParameter("executable-arg-" + i)) != null) {
                args.add(arg);
                ++i;
            }
            this.cgiExecutableArgs = args;
        }
        if (this.getServletConfig().getInitParameter("parameterEncoding") != null) {
            this.parameterEncoding = this.getServletConfig().getInitParameter("parameterEncoding");
        }
        if (this.getServletConfig().getInitParameter("stderrTimeout") != null) {
            this.stderrTimeout = Long.parseLong(this.getServletConfig().getInitParameter("stderrTimeout"));
        }
    }

    protected void printServletEnvironment(ServletOutputStream out, HttpServletRequest req, HttpServletResponse res) throws IOException {
        String value;
        String param;
        String attr;
        out.println("<h1>ServletRequest Properties</h1>");
        out.println("<ul>");
        Enumeration attrs = req.getAttributeNames();
        while (attrs.hasMoreElements()) {
            String attr2 = (String)attrs.nextElement();
            out.println("<li><b>attribute</b> " + attr2 + " = " + req.getAttribute(attr2));
        }
        out.println("<li><b>characterEncoding</b> = " + req.getCharacterEncoding());
        out.println("<li><b>contentLength</b> = " + req.getContentLength());
        out.println("<li><b>contentType</b> = " + req.getContentType());
        Enumeration locales = req.getLocales();
        while (locales.hasMoreElements()) {
            Locale locale = (Locale)locales.nextElement();
            out.println("<li><b>locale</b> = " + locale);
        }
        Enumeration params = req.getParameterNames();
        while (params.hasMoreElements()) {
            String param2 = (String)params.nextElement();
            String[] values = req.getParameterValues(param2);
            for (int i = 0; i < values.length; ++i) {
                out.println("<li><b>parameter</b> " + param2 + " = " + values[i]);
            }
        }
        out.println("<li><b>protocol</b> = " + req.getProtocol());
        out.println("<li><b>remoteAddr</b> = " + req.getRemoteAddr());
        out.println("<li><b>remoteHost</b> = " + req.getRemoteHost());
        out.println("<li><b>scheme</b> = " + req.getScheme());
        out.println("<li><b>secure</b> = " + req.isSecure());
        out.println("<li><b>serverName</b> = " + req.getServerName());
        out.println("<li><b>serverPort</b> = " + req.getServerPort());
        out.println("</ul>");
        out.println("<hr>");
        out.println("<h1>HttpServletRequest Properties</h1>");
        out.println("<ul>");
        out.println("<li><b>authType</b> = " + req.getAuthType());
        out.println("<li><b>contextPath</b> = " + req.getContextPath());
        Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            for (int i = 0; i < cookies.length; ++i) {
                out.println("<li><b>cookie</b> " + cookies[i].getName() + " = " + cookies[i].getValue());
            }
        }
        Enumeration headers = req.getHeaderNames();
        while (headers.hasMoreElements()) {
            String header = (String)headers.nextElement();
            out.println("<li><b>header</b> " + header + " = " + req.getHeader(header));
        }
        out.println("<li><b>method</b> = " + req.getMethod());
        out.println("<li><a name=\"pathInfo\"><b>pathInfo</b></a> = " + req.getPathInfo());
        out.println("<li><b>pathTranslated</b> = " + req.getPathTranslated());
        out.println("<li><b>queryString</b> = " + req.getQueryString());
        out.println("<li><b>remoteUser</b> = " + req.getRemoteUser());
        out.println("<li><b>requestedSessionId</b> = " + req.getRequestedSessionId());
        out.println("<li><b>requestedSessionIdFromCookie</b> = " + req.isRequestedSessionIdFromCookie());
        out.println("<li><b>requestedSessionIdFromURL</b> = " + req.isRequestedSessionIdFromURL());
        out.println("<li><b>requestedSessionIdValid</b> = " + req.isRequestedSessionIdValid());
        out.println("<li><b>requestURI</b> = " + req.getRequestURI());
        out.println("<li><b>servletPath</b> = " + req.getServletPath());
        out.println("<li><b>userPrincipal</b> = " + req.getUserPrincipal());
        out.println("</ul>");
        out.println("<hr>");
        out.println("<h1>ServletRequest Attributes</h1>");
        out.println("<ul>");
        attrs = req.getAttributeNames();
        while (attrs.hasMoreElements()) {
            String attr3 = (String)attrs.nextElement();
            out.println("<li><b>" + attr3 + "</b> = " + req.getAttribute(attr3));
        }
        out.println("</ul>");
        out.println("<hr>");
        HttpSession session = req.getSession(false);
        if (session != null) {
            out.println("<h1>HttpSession Properties</h1>");
            out.println("<ul>");
            out.println("<li><b>id</b> = " + session.getId());
            out.println("<li><b>creationTime</b> = " + new Date(session.getCreationTime()));
            out.println("<li><b>lastAccessedTime</b> = " + new Date(session.getLastAccessedTime()));
            out.println("<li><b>maxInactiveInterval</b> = " + session.getMaxInactiveInterval());
            out.println("</ul>");
            out.println("<hr>");
            out.println("<h1>HttpSession Attributes</h1>");
            out.println("<ul>");
            attrs = session.getAttributeNames();
            while (attrs.hasMoreElements()) {
                attr = (String)attrs.nextElement();
                out.println("<li><b>" + attr + "</b> = " + session.getAttribute(attr));
            }
            out.println("</ul>");
            out.println("<hr>");
        }
        out.println("<h1>ServletConfig Properties</h1>");
        out.println("<ul>");
        out.println("<li><b>servletName</b> = " + this.getServletConfig().getServletName());
        out.println("</ul>");
        out.println("<hr>");
        out.println("<h1>ServletConfig Initialization Parameters</h1>");
        out.println("<ul>");
        params = this.getServletConfig().getInitParameterNames();
        while (params.hasMoreElements()) {
            param = (String)params.nextElement();
            value = this.getServletConfig().getInitParameter(param);
            out.println("<li><b>" + param + "</b> = " + value);
        }
        out.println("</ul>");
        out.println("<hr>");
        out.println("<h1>ServletContext Properties</h1>");
        out.println("<ul>");
        out.println("<li><b>majorVersion</b> = " + this.getServletContext().getMajorVersion());
        out.println("<li><b>minorVersion</b> = " + this.getServletContext().getMinorVersion());
        out.println("<li><b>realPath('/')</b> = " + this.getServletContext().getRealPath("/"));
        out.println("<li><b>serverInfo</b> = " + this.getServletContext().getServerInfo());
        out.println("</ul>");
        out.println("<hr>");
        out.println("<h1>ServletContext Initialization Parameters</h1>");
        out.println("<ul>");
        params = this.getServletContext().getInitParameterNames();
        while (params.hasMoreElements()) {
            param = (String)params.nextElement();
            value = this.getServletContext().getInitParameter(param);
            out.println("<li><b>" + param + "</b> = " + value);
        }
        out.println("</ul>");
        out.println("<hr>");
        out.println("<h1>ServletContext Attributes</h1>");
        out.println("<ul>");
        attrs = this.getServletContext().getAttributeNames();
        while (attrs.hasMoreElements()) {
            attr = (String)attrs.nextElement();
            out.println("<li><b>" + attr + "</b> = " + this.getServletContext().getAttribute(attr));
        }
        out.println("</ul>");
        out.println("<hr>");
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        this.doGet(req, res);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        this.doGet(req, res);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        this.doGet(req, res);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        CGIEnvironment cgiEnv = new CGIEnvironment(req, this.getServletContext());
        if (cgiEnv.isValid()) {
            CGIRunner cgi = new CGIRunner(cgiEnv.getCommand(), cgiEnv.getEnvironment(), cgiEnv.getWorkingDirectory(), cgiEnv.getParameters());
            if ("POST".equals(req.getMethod()) || "PUT".equals(req.getMethod())) {
                cgi.setInput((InputStream)req.getInputStream());
            }
            cgi.setResponse(res);
            cgi.run();
        }
        if (!cgiEnv.isValid()) {
            res.setStatus(404);
        }
        if (this.debug >= 10) {
            ServletOutputStream out = res.getOutputStream();
            out.println("<HTML><HEAD><TITLE>$Name$</TITLE></HEAD>");
            out.println("<BODY>$Header$<p>");
            if (cgiEnv.isValid()) {
                out.println(cgiEnv.toString());
            } else {
                out.println("<H3>");
                out.println("CGI script not found or not specified.");
                out.println("</H3>");
                out.println("<H4>");
                out.println("Check the <b>HttpServletRequest ");
                out.println("<a href=\"#pathInfo\">pathInfo</a></b> ");
                out.println("property to see if it is what you meant ");
                out.println("it to be.  You must specify an existant ");
                out.println("and executable file as part of the ");
                out.println("path-info.");
                out.println("</H4>");
                out.println("<H4>");
                out.println("For a good discussion of how CGI scripts ");
                out.println("work and what their environment variables ");
                out.println("mean, please visit the <a ");
                out.println("href=\"http://cgi-spec.golux.com\">CGI ");
                out.println("Specification page</a>.");
                out.println("</H4>");
            }
            this.printServletEnvironment(out, req, res);
            out.println("</BODY></HTML>");
        }
    }

    protected static class HTTPHeaderInputStream
            extends InputStream {
        private static final int STATE_CHARACTER = 0;
        private static final int STATE_FIRST_CR = 1;
        private static final int STATE_FIRST_LF = 2;
        private static final int STATE_SECOND_CR = 3;
        private static final int STATE_HEADER_END = 4;
        private InputStream input;
        private int state;

        HTTPHeaderInputStream(InputStream theInput) {
            this.input = theInput;
            this.state = 0;
        }

        @Override
        public int read() throws IOException {
            if (this.state == 4) {
                return -1;
            }
            int i = this.input.read();
            if (i == 10) {
                switch (this.state) {
                    case 0:
                    case 1: {
                        this.state = 2;
                        break;
                    }
                    case 2:
                    case 3: {
                        this.state = 4;
                    }
                }
            } else if (i == 13) {
                switch (this.state) {
                    case 0: {
                        this.state = 1;
                        break;
                    }
                    case 1: {
                        this.state = 4;
                        break;
                    }
                    case 2: {
                        this.state = 3;
                    }
                }
            } else {
                this.state = 0;
            }
            return i;
        }
    }

    protected class CGIRunner {
        private String command = null;
        private Hashtable<String, String> env = null;
        private File wd = null;
        private ArrayList<String> params = null;
        private InputStream stdin = null;
        private HttpServletResponse response = null;
        private boolean readyToRun = false;

        protected CGIRunner(String command, Hashtable<String, String> env, File wd, ArrayList<String> params) {
            this.command = command;
            this.env = env;
            this.wd = wd;
            this.params = params;
            this.updateReadyStatus();
        }

        protected void updateReadyStatus() {
            this.readyToRun = this.command != null && this.env != null && this.wd != null && this.params != null && this.response != null;
        }

        protected boolean isReady() {
            return this.readyToRun;
        }

        protected void setResponse(HttpServletResponse response) {
            this.response = response;
            this.updateReadyStatus();
        }

        protected void setInput(InputStream stdin) {
            this.stdin = stdin;
            this.updateReadyStatus();
        }

        protected String[] hashToStringArray(Hashtable<String, ?> h) throws NullPointerException {
            Vector<String> v = new Vector<String>();
            Enumeration<String> e = h.keys();
            while (e.hasMoreElements()) {
                String k = e.nextElement();
                v.add(k + "=" + h.get(k).toString());
            }
            String[] strArr = new String[v.size()];
            v.copyInto(strArr);
            return strArr;
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        protected void run() throws IOException {
            if (!this.isReady()) {
                throw new IOException(this.getClass().getName() + ": not ready to run.");
            }
            if (HonrayCgiServlet.this.debug >= 1) {
                HonrayCgiServlet.this.log("runCGI(envp=[" + this.env + "], command=" + this.command + ")");
            }
            if (this.command.indexOf(File.separator + "." + File.separator) >= 0 || this.command.indexOf(File.separator + "..") >= 0 || this.command.indexOf(".." + File.separator) >= 0) {
                throw new IOException(this.getClass().getName() + "Illegal Character in CGI command " + "path ('.' or '..') detected.  Not " + "running CGI [" + this.command + "].");
            }
            Runtime rt = null;
            BufferedReader cgiHeaderReader = null;
            InputStream cgiOutput = null;
            BufferedReader commandsStdErr = null;
            Thread errReaderThread = null;
            BufferedOutputStream commandsStdIn = null;
            Process proc = null;
            int bufRead = -1;
            ArrayList<String> cmdAndArgs = new ArrayList<String>();
            if (HonrayCgiServlet.this.cgiExecutable.length() != 0) {
                cmdAndArgs.add(HonrayCgiServlet.this.cgiExecutable);
            }
            if (HonrayCgiServlet.this.cgiExecutableArgs != null) {
                cmdAndArgs.addAll(HonrayCgiServlet.this.cgiExecutableArgs);
            }
            cmdAndArgs.add(this.command);
            cmdAndArgs.addAll(this.params);
            try {
                rt = Runtime.getRuntime();
                proc = rt.exec(cmdAndArgs.toArray(new String[cmdAndArgs.size()]), this.hashToStringArray(this.env), this.wd);
                String sContentLength = this.env.get("CONTENT_LENGTH");
                if (!"".equals(sContentLength)) {
                    commandsStdIn = new BufferedOutputStream(proc.getOutputStream());
                    IOTools.flow(this.stdin, commandsStdIn);
                    commandsStdIn.flush();
                    commandsStdIn.close();
                }
                boolean isRunning = true;
                final BufferedReader stdErrRdr = commandsStdErr = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
                errReaderThread = new Thread(){

                    @Override
                    public void run() {
                        CGIRunner.this.sendToLog(stdErrRdr);
                    }
                };
                errReaderThread.start();
                HTTPHeaderInputStream cgiHeaderStream = new HTTPHeaderInputStream(proc.getInputStream());
                cgiHeaderReader = new BufferedReader(new InputStreamReader(cgiHeaderStream));
                while (isRunning) {
                    try {
                        String line = null;
                        while ((line = cgiHeaderReader.readLine()) != null && !"".equals(line)) {
                            if (HonrayCgiServlet.this.debug >= 2) {
                                HonrayCgiServlet.this.log("runCGI: addHeader(\"" + line + "\")");
                            }
                            if (line.startsWith("HTTP")) {
                                this.response.setStatus(this.getSCFromHttpStatusLine(line));
                                continue;
                            }
                            if (line.indexOf(":") >= 0) {
                                String header = line.substring(0, line.indexOf(":")).trim();
                                String value = line.substring(line.indexOf(":") + 1).trim();
                                if (header.equalsIgnoreCase("status")) {
                                    this.response.setStatus(this.getSCFromCGIStatusHeader(value));
                                    continue;
                                }
                                this.response.addHeader(header, value);
                                continue;
                            }
                            HonrayCgiServlet.this.log("runCGI: bad header line \"" + line + "\"");
                        }
                        byte[] bBuf = new byte[2048];
                        ServletOutputStream out = this.response.getOutputStream();
                        cgiOutput = proc.getInputStream();
                        try {
                            while ((bufRead = cgiOutput.read(bBuf)) != -1) {
                                if (HonrayCgiServlet.this.debug >= 4) {
                                    HonrayCgiServlet.this.log("runCGI: output " + bufRead + " bytes of data");
                                }
                                out.write(bBuf, 0, bufRead);
                            }
                        }
                        finally {
                            if (bufRead != -1) {
                                while ((bufRead = cgiOutput.read(bBuf)) != -1) {
                                }
                            }
                        }
                        proc.exitValue();
                        isRunning = false;
                    }
                    catch (IllegalThreadStateException e) {
                        try {
                            Thread.sleep(500L);
                        }
                        catch (InterruptedException ignored) {}
                    }
                }
            }
            catch (IOException e) {
                HonrayCgiServlet.this.log("Caught exception " + e);
                throw e;
            }
            finally {
                if (cgiHeaderReader != null) {
                    try {
                        cgiHeaderReader.close();
                    }
                    catch (IOException ioe) {
                        HonrayCgiServlet.this.log("Exception closing header reader " + ioe);
                    }
                }
                if (cgiOutput != null) {
                    try {
                        cgiOutput.close();
                    }
                    catch (IOException ioe) {
                        HonrayCgiServlet.this.log("Exception closing output stream " + ioe);
                    }
                }
                if (errReaderThread != null) {
                    try {
                        errReaderThread.join(HonrayCgiServlet.this.stderrTimeout);
                    }
                    catch (InterruptedException e) {
                        HonrayCgiServlet.this.log("Interupted waiting for stderr reader thread");
                    }
                }
                if (HonrayCgiServlet.this.debug > 4) {
                    HonrayCgiServlet.this.log("Running finally block");
                }
                if (proc != null) {
                    proc.destroy();
                    proc = null;
                }
            }
        }

        private int getSCFromHttpStatusLine(String line) {
            int statusCode;
            int statusStart = line.indexOf(32) + 1;
            if (statusStart < 1 || line.length() < statusStart + 3) {
                HonrayCgiServlet.this.log("runCGI: invalid HTTP Status-Line:" + line);
                return 500;
            }
            String status = line.substring(statusStart, statusStart + 3);
            try {
                statusCode = Integer.parseInt(status);
            }
            catch (NumberFormatException nfe) {
                HonrayCgiServlet.this.log("runCGI: invalid status code:" + status);
                return 500;
            }
            return statusCode;
        }

        private int getSCFromCGIStatusHeader(String value) {
            int statusCode;
            if (value.length() < 3) {
                HonrayCgiServlet.this.log("runCGI: invalid status value:" + value);
                return 500;
            }
            String status = value.substring(0, 3);
            try {
                statusCode = Integer.parseInt(status);
            }
            catch (NumberFormatException nfe) {
                HonrayCgiServlet.this.log("runCGI: invalid status code:" + status);
                return 500;
            }
            return statusCode;
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        private void sendToLog(BufferedReader rdr) {
            String line = null;
            int lineCount = 0;
            try {
                while ((line = rdr.readLine()) != null) {
                    HonrayCgiServlet.this.log("runCGI (stderr):" + line);
                    ++lineCount;
                }
            }
            catch (IOException e) {
                HonrayCgiServlet.this.log("sendToLog error", e);
            }
            finally {
                try {
                    rdr.close();
                }
                catch (IOException ce) {
                    HonrayCgiServlet.this.log("sendToLog error", ce);
                }
            }
            if (lineCount > 0 && HonrayCgiServlet.this.debug > 2) {
                HonrayCgiServlet.this.log("runCGI: " + lineCount + " lines received on stderr");
            }
        }
    }

    protected class CGIEnvironment {
        private ServletContext context = null;
        private String contextPath = null;
        private String servletPath = null;
        private String pathInfo = null;
        private String webAppRootDir = null;
        private File tmpDir = null;
        private Hashtable<String, String> env = null;
        private String command = null;
        private File workingDirectory = null;
        private ArrayList<String> cmdLineParameters = new ArrayList();
        private boolean valid = false;

        protected CGIEnvironment(HttpServletRequest req, ServletContext context) throws IOException {
            this.setupFromContext(context);
            this.setupFromRequest(req);
            this.valid = this.setCGIEnvironment(req);
            if (this.valid) {
                this.workingDirectory = new File(this.command.substring(0, this.command.lastIndexOf(File.separator)));
            }
        }

        protected void setupFromContext(ServletContext context) {
            this.context = context;
            this.webAppRootDir = context.getRealPath("/");
            this.tmpDir = (File)context.getAttribute("javax.servlet.context.tempdir");
        }

        protected void setupFromRequest(HttpServletRequest req) throws UnsupportedEncodingException {
            String qs;
            boolean isIncluded = false;
            if (req.getAttribute("javax.servlet.include.request_uri") != null) {
                isIncluded = true;
            }
            if (isIncluded) {
                this.contextPath = (String)req.getAttribute("javax.servlet.include.context_path");
                this.servletPath = (String)req.getAttribute("javax.servlet.include.servlet_path");
                this.pathInfo = (String)req.getAttribute("javax.servlet.include.path_info");
            } else {
                this.contextPath = req.getContextPath();
                this.servletPath = req.getServletPath();
                this.pathInfo = req.getPathInfo();
            }
            if (this.pathInfo == null) {
                this.pathInfo = this.servletPath;
            }
            if ((req.getMethod().equals("GET") || req.getMethod().equals("POST") || req.getMethod().equals("HEAD")) && (qs = isIncluded ? (String)req.getAttribute("javax.servlet.include.query_string") : req.getQueryString()) != null && qs.indexOf("=") == -1) {
                StringTokenizer qsTokens = new StringTokenizer(qs, "+");
                while (qsTokens.hasMoreTokens()) {
                    this.cmdLineParameters.add(URLDecoder.decode(qsTokens.nextToken(), HonrayCgiServlet.this.parameterEncoding));
                }
            }
        }

        protected String[] findCGI(String pathInfo, String webAppRootDir, String contextPath, String servletPath, String cgiPathPrefix) {
            String path = null;
            String name = null;
            String scriptname = null;
            String cginame = "";
            if (webAppRootDir != null && webAppRootDir.lastIndexOf(File.separator) == webAppRootDir.length() - 1) {
                webAppRootDir = webAppRootDir.substring(0, webAppRootDir.length() - 1);
            }
            if (cgiPathPrefix != null) {
                webAppRootDir = webAppRootDir + File.separator + cgiPathPrefix;
            }
            if (HonrayCgiServlet.this.debug >= 2) {
                HonrayCgiServlet.this.log("findCGI: path=" + pathInfo + ", " + webAppRootDir);
            }
            File currentLocation = new File(webAppRootDir);
            StringTokenizer dirWalker = new StringTokenizer(pathInfo, "/");
            if (HonrayCgiServlet.this.debug >= 3) {
                HonrayCgiServlet.this.log("findCGI: currentLoc=" + currentLocation);
            }
            while (!currentLocation.isFile() && dirWalker.hasMoreElements()) {
                if (HonrayCgiServlet.this.debug >= 3) {
                    HonrayCgiServlet.this.log("findCGI: currentLoc=" + currentLocation);
                }
                String nextElement = (String)dirWalker.nextElement();
                currentLocation = new File(currentLocation, nextElement);
                cginame = cginame + "/" + nextElement;
            }
            if (!currentLocation.isFile()) {
                return new String[]{null, null, null, null};
            }
            if (HonrayCgiServlet.this.debug >= 2) {
                HonrayCgiServlet.this.log("findCGI: FOUND cgi at " + currentLocation);
            }
            path = currentLocation.getAbsolutePath();
            name = currentLocation.getName();
            scriptname = ".".equals(contextPath) ? servletPath : contextPath + servletPath;
            if (!servletPath.equals(cginame)) {
                scriptname = scriptname + cginame;
            }
            if (HonrayCgiServlet.this.debug >= 1) {
                HonrayCgiServlet.this.log("findCGI calc: name=" + name + ", path=" + path + ", scriptname=" + scriptname + ", cginame=" + cginame);
            }
            return new String[]{path, scriptname, cginame, name};
        }

        protected boolean setCGIEnvironment(HttpServletRequest req) throws IOException {
            Hashtable<String, String> envp = new Hashtable<String, String>();
            envp.putAll(shellEnv);
            String sPathInfoOrig = null;
            String sPathInfoCGI = null;
            String sPathTranslatedCGI = null;
            String sCGIFullPath = null;
            String sCGIScriptName = null;
            String sCGIFullName = null;
            String sCGIName = null;
            sPathInfoOrig = this.pathInfo;
            String string = sPathInfoOrig = sPathInfoOrig == null ? "" : sPathInfoOrig;
            if (this.webAppRootDir == null) {
                this.webAppRootDir = this.tmpDir.toString();
                this.expandCGIScript();
            }
            String[] sCGINames = this.findCGI(sPathInfoOrig, this.webAppRootDir, this.contextPath, this.servletPath, HonrayCgiServlet.this.cgiPathPrefix);
            sCGIFullPath = sCGINames[0];
            sCGIScriptName = sCGINames[1];
            sCGIFullName = sCGINames[2];
            sCGIName = sCGINames[3];
            if (sCGIFullPath == null || sCGIScriptName == null || sCGIFullName == null || sCGIName == null) {
                return false;
            }
            envp.put("SERVER_SOFTWARE", "TOMCAT");
            envp.put("SERVER_NAME", this.nullsToBlanks(req.getServerName()));
            envp.put("GATEWAY_INTERFACE", "CGI/1.1");
            envp.put("SERVER_PROTOCOL", this.nullsToBlanks(req.getProtocol()));
            int port = req.getServerPort();
            Integer iPort = port == 0 ? Integer.valueOf(-1) : Integer.valueOf(port);
            envp.put("SERVER_PORT", iPort.toString());
            envp.put("REQUEST_METHOD", this.nullsToBlanks(req.getMethod()));
            envp.put("REQUEST_URI", this.nullsToBlanks(req.getRequestURI()));
            sPathInfoCGI = this.pathInfo == null || this.pathInfo.substring(sCGIFullName.length()).length() <= 0 ? "" : this.pathInfo.substring(sCGIFullName.length());
            envp.put("PATH_INFO", sPathInfoCGI);
            if (sPathInfoCGI != null && !"".equals(sPathInfoCGI)) {
                sPathTranslatedCGI = this.context.getRealPath(sPathInfoCGI);
            }
            if (sPathTranslatedCGI != null && !"".equals(sPathTranslatedCGI)) {
                envp.put("PATH_TRANSLATED", this.nullsToBlanks(sPathTranslatedCGI));
            }
            envp.put("SCRIPT_NAME", this.nullsToBlanks(sCGIScriptName));
            envp.put("QUERY_STRING", this.nullsToBlanks(req.getQueryString()));
            envp.put("REMOTE_HOST", this.nullsToBlanks(req.getRemoteHost()));
            envp.put("REMOTE_ADDR", this.nullsToBlanks(req.getRemoteAddr()));
            envp.put("AUTH_TYPE", this.nullsToBlanks(req.getAuthType()));
            envp.put("REMOTE_USER", this.nullsToBlanks(req.getRemoteUser()));
            envp.put("REMOTE_IDENT", "");
            envp.put("CONTENT_TYPE", this.nullsToBlanks(req.getContentType()));
            int contentLength = req.getContentLength();
            String sContentLength = contentLength <= 0 ? "" : Integer.valueOf(contentLength).toString();
            envp.put("CONTENT_LENGTH", sContentLength);
            Enumeration headers = req.getHeaderNames();
            String header = null;
            while (headers.hasMoreElements()) {
                header = null;
                header = ((String)headers.nextElement()).toUpperCase(Locale.ENGLISH);
                if ("AUTHORIZATION".equalsIgnoreCase(header) || "PROXY_AUTHORIZATION".equalsIgnoreCase(header)) continue;
                envp.put("HTTP_" + header.replace('-', '_'), req.getHeader(header));
            }
            File fCGIFullPath = new File(sCGIFullPath);
            this.command = fCGIFullPath.getCanonicalPath();
            envp.put("X_TOMCAT_SCRIPT_PATH", this.command);
            envp.put("SCRIPT_FILENAME", this.command);
            this.env = envp;
            return true;
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        protected void expandCGIScript() {
            block16: {
                StringBuilder srcPath = new StringBuilder();
                StringBuilder destPath = new StringBuilder();
                InputStream is = null;
                if (HonrayCgiServlet.this.cgiPathPrefix == null) {
                    srcPath.append(this.pathInfo);
                    is = this.context.getResourceAsStream(srcPath.toString());
                    destPath.append(this.tmpDir);
                    destPath.append(this.pathInfo);
                } else {
                    srcPath.append(HonrayCgiServlet.this.cgiPathPrefix);
                    StringTokenizer pathWalker = new StringTokenizer(this.pathInfo, "/");
                    while (pathWalker.hasMoreElements() && is == null) {
                        srcPath.append("/");
                        srcPath.append(pathWalker.nextElement());
                        is = this.context.getResourceAsStream(srcPath.toString());
                    }
                    destPath.append(this.tmpDir);
                    destPath.append("/");
                    destPath.append((CharSequence)srcPath);
                }
                if (is == null) {
                    if (HonrayCgiServlet.this.debug >= 2) {
                        HonrayCgiServlet.this.log("expandCGIScript: source '" + srcPath + "' not found");
                    }
                    return;
                }
                File f = new File(destPath.toString());
                if (f.exists()) {
                    return;
                }
                String dirPath = destPath.toString().substring(0, destPath.toString().lastIndexOf("/"));
                File dir = new File(dirPath);
                if (!dir.mkdirs() && !dir.isDirectory()) {
                    if (HonrayCgiServlet.this.debug >= 2) {
                        HonrayCgiServlet.this.log("expandCGIScript: failed to create directories for '" + dir.getAbsolutePath() + "'");
                    }
                    return;
                }
                try {
                    Object object = expandFileLock;
                    synchronized (object) {
                        if (f.exists()) {
                            return;
                        }
                        if (!f.createNewFile()) {
                            return;
                        }
                        FileOutputStream fos = new FileOutputStream(f);
                        IOTools.flow(is, fos);
                        is.close();
                        fos.close();
                        if (HonrayCgiServlet.this.debug >= 2) {
                            HonrayCgiServlet.this.log("expandCGIScript: expanded '" + srcPath + "' to '" + destPath + "'");
                        }
                    }
                }
                catch (IOException ioe) {
                    if (!f.exists() || f.delete() || HonrayCgiServlet.this.debug < 2) break block16;
                    HonrayCgiServlet.this.log("expandCGIScript: failed to delete '" + f.getAbsolutePath() + "'");
                }
            }
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("<TABLE border=2>");
            sb.append("<tr><th colspan=2 bgcolor=grey>");
            sb.append("CGIEnvironment Info</th></tr>");
            sb.append("<tr><td>Debug Level</td><td>");
            sb.append(HonrayCgiServlet.this.debug);
            sb.append("</td></tr>");
            sb.append("<tr><td>Validity:</td><td>");
            sb.append(this.isValid());
            sb.append("</td></tr>");
            if (this.isValid()) {
                Enumeration<String> envk = this.env.keys();
                while (envk.hasMoreElements()) {
                    String s = envk.nextElement();
                    sb.append("<tr><td>");
                    sb.append(s);
                    sb.append("</td><td>");
                    sb.append(this.blanksToString(this.env.get(s), "[will be set to blank]"));
                    sb.append("</td></tr>");
                }
            }
            sb.append("<tr><td colspan=2><HR></td></tr>");
            sb.append("<tr><td>Derived Command</td><td>");
            sb.append(this.nullsToBlanks(this.command));
            sb.append("</td></tr>");
            sb.append("<tr><td>Working Directory</td><td>");
            if (this.workingDirectory != null) {
                sb.append(this.workingDirectory.toString());
            }
            sb.append("</td></tr>");
            sb.append("<tr><td>Command Line Params</td><td>");
            for (int i = 0; i < this.cmdLineParameters.size(); ++i) {
                String param = this.cmdLineParameters.get(i);
                sb.append("<p>");
                sb.append(param);
                sb.append("</p>");
            }
            sb.append("</td></tr>");
            sb.append("</TABLE><p>end.");
            return sb.toString();
        }

        protected String getCommand() {
            return this.command;
        }

        protected File getWorkingDirectory() {
            return this.workingDirectory;
        }

        protected Hashtable<String, String> getEnvironment() {
            return this.env;
        }

        protected ArrayList<String> getParameters() {
            return this.cmdLineParameters;
        }

        protected boolean isValid() {
            return this.valid;
        }

        protected String nullsToBlanks(String s) {
            return this.nullsToString(s, "");
        }

        protected String nullsToString(String couldBeNull, String subForNulls) {
            return couldBeNull == null ? subForNulls : couldBeNull;
        }

        protected String blanksToString(String couldBeBlank, String subForBlanks) {
            return "".equals(couldBeBlank) || couldBeBlank == null ? subForBlanks : couldBeBlank;
        }
    }
}

