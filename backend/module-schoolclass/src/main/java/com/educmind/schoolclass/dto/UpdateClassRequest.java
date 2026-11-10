package com.educmind.schoolclass.dto;

import jakarta.validation.constraints.Size;

public class UpdateClassRequest {

    @Size(min = 1, max = 100, message = "Tên lớp phải từ 1 đến 100 ký tự")
    private String name;

    private Short grade;

    @Size(max = 20, message = "Năm học tối đa 20 ký tự")
    private String schoolYear;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Short getGrade() {
        return grade;
    }

    public void setGrade(Short grade) {
        this.grade = grade;
    }

    public String getSchoolYear() {
        return schoolYear;
    }

    public void setSchoolYear(String schoolYear) {
        this.schoolYear = schoolYear;
    }
}
