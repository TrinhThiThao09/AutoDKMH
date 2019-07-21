package net.bqc.autodkmh;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Tool for automatically registering courses of VNU.
 *     public final static String HOST = "http://dangkyhoc.vnu.edu.vn";
 * @Created by cuong on  2/12/2015
 * @Updated by cuong on 17/08/2017
 */
public class AutoDKMH {

	public final static String HOST = "http://dangkyhoc.vnu.edu.vn";

    public final static String LOGIN_URL = HOST + "/dang-nhap";
    public final static String LOGOUT_URL = HOST + "/Account/Logout";

    // only available courses for your major
    public final static String AVAILABLE_COURSES_DATA_URL_MAJOR = HOST + "/danh-sach-mon-hoc/1/1";

    // all available courses
    public final static String AVAILABLE_COURSES_DATA_URL_ALL = HOST + "/danh-sach-mon-hoc/1/2";

    public final static String REGISTERED_COURSES_DATA_URL = HOST + "/danh-sach-mon-hoc-da-dang-ky/1";

    // %s for data-crdid
    public final static String CHECK_PREREQUISITE_COURSES_URL = HOST + "/kiem-tra-tien-quyet/%s/1";

    // %s for data-rowindex
    public final static String CHOOSE_COURSE_URL = HOST + "/chon-mon-hoc/%s/1/1";

    public final static String SUBMIT_URL = HOST + "/xac-nhan-dang-ky/1";

    public final static String USER_AGENT = "Mozilla/5.0";
    public final static String ACCEPT = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8";
    public final static String ACCEPT_LANGUAGE = "en-US,en;q=0.5";

    private HttpURLConnection con;

    // user's information
    private String user;
    private String password;
    private List<String> courseCodes;

    private List<Course> courses;

    // sleep time
    private long sleepTime;

    public AutoDKMH() {
        courseCodes = new ArrayList<>();
        courses = new ArrayList<>();
        loadInitialParameters("config.properties");
    }

    public static void main(String args[]) throws IOException, InterruptedException {
        AutoDKMH tool = new AutoDKMH();

        // tool.sendGet(HOST);

        logn("/******************************************/");
        logn("//! Username = " + tool.user);
        // not support for password under 2 characters :P
        logn("//! Password = " + "********");
        logn("//! Course Codes = " + tool.courseCodes);
        logn("/******************************************/");

        tool.run();
    }

    /**
     * The entrance gate to dark world...
     * 
     * @throws IOException
     * @throws InterruptedException
     */
    private void run() throws IOException, InterruptedException {
        // turn on cookie
        CookieManager cookieManager = new CookieManager();
        CookieHandler.setDefault(cookieManager);
        Calendar cal = Calendar.getInstance();

        while (true) {
            logn("\n/******************************************/");
            logn("Try on: " + cal.getTime().toString());

            try {
                doLogin();
            } catch (Exception e) {
                System.err.println("\nEncountered exception " + e.getMessage());
                logn("Try again...");
                continue;
            }

            // get registered courses, filter desired courses
            // it is necessary to do it before submitting courses
            log("Filtering desired courses...");
            String registeredCoursesData = sendPost(REGISTERED_COURSES_DATA_URL, "");
            courseCodes = courseCodes.stream()
                    .filter(code -> !registeredCoursesData.contains(code))
                    .collect(Collectors.toList());
            logn("[Done]");
            logn("Filtered courses: " + courseCodes);

            if (courseCodes.isEmpty()) {
                logn("\nCourses have been already registered!\n[Exit]");
                System.exit(1);
            }

            // must get this shit before submitting a new course >.<
            sendPost(AVAILABLE_COURSES_DATA_URL_MAJOR, "");

            // get list of courses and the course details by given course code
            log("Get raw courses data...");
            String coursesData = sendPost(AVAILABLE_COURSES_DATA_URL_ALL, "");
            logn("[Done]");

            for (Iterator<String> it = courseCodes.iterator(); it.hasNext();) {
                String courseCode = it.next();
                log("\nGetting course information for [" + courseCode + "]...");
                String courseDetails[] = getCourseDetailsFromCoursesData(coursesData, courseCode);
                logn("[Done]");

                /* register courses and submit them */
                if (courseDetails != null) {
                    // check prerequisite courses
                    log("Checking prerequisite courses...");
                    String res = sendPost(String.format(CHECK_PREREQUISITE_COURSES_URL, courseDetails[0]), "");
                    logn("[Done]");
                    logn("Response: " + res);
                    // choose course
                    log("Choose [" + courseCodes + "] for queue...");
                    res = sendPost(String.format(CHOOSE_COURSE_URL, courseDetails[1]), "");
                    logn("[Done]");
                    logn("Response: " + res);
                    // remove after being registered
                    if (res.contains("thành công"))
                        it.remove();
                }
            }

            // submit registered courses
            log("Submitting...");
            String res = sendPost(String.format(SUBMIT_URL, ""), "");
            logn("[Done]");
            logn("Response: " + res);

            // logout
            log("Logging out...");
            sendGet(LOGOUT_URL);
            logn("[Success]");

            if (courseCodes.isEmpty()) {
                logn("\nRegistered all!\n[Exit]");
                System.exit(1);
            }

            logn("/******************************************/");
            Thread.sleep(sleepTime);
        }
    }

    /**
     * Load login site to get cookie and login parameters then login using post
     * method
     * 
     * @throws IOException
     */
    private void doLogin() throws IOException {
        log("Getting cookies, token...");
        String loginSiteHtml = sendGet(LOGIN_URL);
        logn("[Done]");

        log("Logging in...");
        String loginParams = getFormParams(loginSiteHtml, user, password);
        String res = sendPost(LOGIN_URL, loginParams);
        if (!res.contains("<title>Trang ch\u1EE7")) {
            logn("[Fail]");
            System.exit(1);
        }
        logn("[Success]");
    }

    /**
     * Load configured parameters
     * 
     * @param filePath
     */
    private void loadInitialParameters(String filePath) {
        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            InputStream is = loader.getResourceAsStream(filePath);
            Properties p = new Properties();
            p.load(is);

            this.user = p.getProperty("usr");
            this.password = p.getProperty("passwd");

            String rawCourseCodes = p.getProperty("course_codes");
            String[] courseCodesArr = rawCourseCodes.split("\\.");
            courseCodes.addAll(Arrays.asList(courseCodesArr));

            String sleepTimeStr = p.getProperty("sleep_time");
            this.sleepTime = Long.parseLong(sleepTimeStr);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Get data-crdid and data-rowindex of a course
     * 
     * @param coursesDataHtml
     * @param courseCode
     *            the given code of course
     * @return the first element is data-crdid and the second is data-rowindex
     *         if the course is available. Otherwise, return null
     */
    private String[] getCourseDetailsFromCoursesData(String coursesDataHtml, String courseCode) {
        coursesDataHtml = "<table id=\"coursesData\">" + coursesDataHtml + "</table>";
        Document doc = Jsoup.parse(coursesDataHtml);
        Elements elements = doc.select("#coursesData").select("tr");

        /* find course on courses list which owns the given course code */
        for (Element e : elements) {
            if (e.toString().contains(courseCode)) {
                /*
                 * data-cridid and data-rowindex always are at the first input
                 * tag if the course is available
                 */
                Element inputElement = e.getElementsByTag("input").get(0);

                if (inputElement.hasAttr("data-rowindex")) { // the course is
                                                             // available for
                                                             // registering
                    String crdid = inputElement.attr("data-crdid");
                    String rowindex = inputElement.attr("data-rowindex");
                    return new String[] { crdid, rowindex };
                }
            }
        }

        return null; // the course is not available
    }

    /**
     * Get parameters for login action
     * 
     * @param html
     *            parse to get cookie and parameters from this
     * @param user
     *            user value parameter
     * @param passwd
     *            password value parameter
     * @return all parameters in a string
     */
    private String  getFormParams(String html, String user, String passwd) {
        Document doc = Jsoup.parse(html);
        List<String> params = new ArrayList<>();

        // login form
        Elements elements = doc.getAllElements();
        Element loginForm = elements.first();
        Elements inputElements = loginForm.getElementsByTag("input");

        // generate parameters
        for (Element inputElement : inputElements) {
            String key = inputElement.attr("name");
            String value = inputElement.attr("value");

            if (key.equals("LoginName")) {
                value = user;
            } else if (key.equals("Password")) {
                value = passwd;
            }

            params.add(key + "=" + value);
        }

        StringBuilder builder = new StringBuilder();
        for (String param : params) {
            if (builder.length() == 0) {
                builder.append(param);

            } else
                builder.append("&").append(param);
        }

        return builder.toString();
    }

    /**
     * Send post method
     * 
     * @param urlStr
     *            url for post
     * @param postParams
     *            parameters
     * @return response from server
     * @throws IOException
     */
    private String sendPost(String urlStr, String postParams) throws IOException {
        URL url = new URL(urlStr);
        con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setUseCaches(false);
        con.setDoOutput(true);

        // set properties
        con.setRequestProperty("User-Agent", USER_AGENT);
        con.setRequestProperty("Accept", ACCEPT);
        con.setRequestProperty("Accept-Language", ACCEPT_LANGUAGE);
        con.setRequestProperty("Connection", "keep-alive");
        con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        con.setRequestProperty("Content-Length", Integer.toString(postParams.length()));

        // Send post request
        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
        wr.writeBytes(postParams);
        wr.flush();
        wr.close();

        // check result code
        // int responseCode = con.getResponseCode();
        // logln("\nSending 'POST' request to URL : " + url);
        // logln("Post parameters : " + postParams);
        // logln("Response Code : " + responseCode);

        // get content
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"));
        String inputLine;
        StringBuffer response = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        return response.toString();
    }

    /**
     * Send get method
     * 
     * @param urlStr
     *            url for get
     * @return response from server
     * @throws IOException
     */
    private String sendGet(String urlStr) throws IOException {
        URL url = new URL(urlStr);
        con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setUseCaches(false);

        // set properties
        con.setRequestProperty("User-Agent", USER_AGENT);
        con.setRequestProperty("Accept", ACCEPT);
        con.setRequestProperty("Accept-Language", ACCEPT_LANGUAGE);

        // check result code
        // int responseCode = con.getResponseCode();
        // logln("\nSending 'GET' request to URL : " + url);
        // logln("Response Code : " + responseCode);
        // get result content

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"));

        String inputLine;
        StringBuffer response = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        return response.toString();
    }
    
    private static void log(String message) {
        System.out.print(message);
    }
    
    private static void logn(String message) {
        log(message + "\n");
    }
}