package com.enroll.server.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * 学生修改报名信息请求 DTO
 *
 * 注意：id 必填（定位要改哪条记录），其余字段可部分填写（只改传了的字段）。
 */
public class ApplicationUpdateRequest {

    @NotNull(message = "报名ID不能为空")
    private Integer id;
    private String name;
    private String phone;
    private String idCard;
    private String gender;
    private String hasPhysics;
    private String hasEnglish;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getIdCard() { return idCard; }
    public void setIdCard(String idCard) { this.idCard = idCard; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getHasPhysics() { return hasPhysics; }
    public void setHasPhysics(String hasPhysics) { this.hasPhysics = hasPhysics; }

    public String getHasEnglish() { return hasEnglish; }
    public void setHasEnglish(String hasEnglish) { this.hasEnglish = hasEnglish; }
}
