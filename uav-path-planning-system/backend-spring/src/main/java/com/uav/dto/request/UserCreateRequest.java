package com.uav.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户创建请求DTO
 */
@Data
public class UserCreateRequest {

    /**
     * 用户名
     * 长度3-50字符，只能包含字母、数字、下划线
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在3-50字符之间")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
    private String username;

    /**
     * 密码
     * 长度8-100字符，必须包含大小写字母和数字
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 100, message = "密码长度必须在8-100字符之间")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$", 
             message = "密码必须包含大小写字母和数字")
    private String password;

    /**
     * 邮箱
     */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    /**
     * 姓名
     */
    @NotBlank(message = "姓名不能为空")
    @Size(max = 100, message = "姓名长度不能超过100字符")
    private String fullName;
}