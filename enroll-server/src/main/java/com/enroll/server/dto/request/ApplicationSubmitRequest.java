package com.enroll.server.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * 学生报名提交请求 DTO
 *
 * 与手写校验不同，@NotBlank/@Pattern 注解由 Spring 的 @Valid 触发自动校验，
 * 校验失败直接返回 400 级错误，无需在 Controller/Service 里手写 if 判断。
 */
public class ApplicationSubmitRequest {

    @NotBlank(message = "姓名不能为空")
    @Pattern(regexp = "^[一-龥]{2,10}$", message = "姓名格式不正确（2-10个中文）")
    private String name;

    @NotBlank(message = "身份证号不能为空")
    @Pattern(regexp = "^\\d{17}[\\dXx]$", message = "身份证号格式不正确")
    private String idCard;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    private String gender;
    private String hasPhysics;
    private String hasEnglish;
    private String appliedCategory;

    @NotNull(message = "班级ID不能为空")
    private Integer classId;

    private Object noticeAgreed;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getIdCard() { return idCard; }
    public void setIdCard(String idCard) { this.idCard = idCard; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getHasPhysics() { return hasPhysics; }
    public void setHasPhysics(String hasPhysics) { this.hasPhysics = hasPhysics; }

    public String getHasEnglish() { return hasEnglish; }
    public void setHasEnglish(String hasEnglish) { this.hasEnglish = hasEnglish; }

    public String getAppliedCategory() { return appliedCategory; }
    public void setAppliedCategory(String appliedCategory) { this.appliedCategory = appliedCategory; }

    public Integer getClassId() { return classId; }
    public void setClassId(Integer classId) { this.classId = classId; }

    public Object getNoticeAgreed() { return noticeAgreed; }
    public void setNoticeAgreed(Object noticeAgreed) { this.noticeAgreed = noticeAgreed; }
}
