package fiap_adj8.feedback_platform.domain.model;

import java.time.LocalDateTime;

public class AlertMessageDetails {
    private String studentName;
    private String lessonName;
    private String comment;
    private String rating;
    private LocalDateTime date;

    // ✅ Constructors
    public AlertMessageDetails() {
    }

    public AlertMessageDetails(String studentName, String lessonName, String comment, String rating, LocalDateTime date) {
        this.studentName = studentName;
        this.lessonName = lessonName;
        this.comment = comment;
        this.rating = rating;
        this.date = date;
    }

    // ✅ Getters and Setters
    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getLessonName() {
        return lessonName;
    }

    public void setLessonName(String lessonName) {
        this.lessonName = lessonName;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getRating() {
        return rating;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    @Override
    public String toString() {
        return "AlertMessageDetails {" +
               "studentName='" + studentName + '\'' +
               ", lessonName='" + lessonName + '\'' +
               ", comment='" + comment + '\'' +
               ", rating='" + rating + '\'' +
               ", date=" + date +
               '}';
    }

}
