package fiap_adj8.feedback_platform.domain.model;

import java.time.LocalDateTime;

public class AlertMessageDetails {
    private String studentName;
    private String lessonName;
    private String comment;
    private int rating;
    private LocalDateTime date;

    // ✅ Constructors
    public AlertMessageDetails() {
    }

    public AlertMessageDetails(String studentName, String lessonName, String comment, int rating, LocalDateTime date) {
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

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }
}
