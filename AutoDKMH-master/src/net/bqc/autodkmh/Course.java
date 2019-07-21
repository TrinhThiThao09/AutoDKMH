package net.bqc.autodkmh;

public class Course {
    
    private String courseCode;
    private String rowindex;
    private String credid;
    
    public Course(String rowindex, String credid) {
        super();
        this.rowindex = rowindex;
        this.credid = credid;
    }
    
    public Course(String courseCode, String rowindex, String credid) {
        super();
        this.courseCode = courseCode;
        this.rowindex = rowindex;
        this.credid = credid;
    }

    public String getCourseCode() {
        return courseCode;
    }
    
    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public String getRowindex() {
        return rowindex;
    }

    public void setRowindex(String rowindex) {
        this.rowindex = rowindex;
    }

    public String getCredid() {
        return credid;
    }

    public void setCredid(String credid) {
        this.credid = credid;
    }
    
}
