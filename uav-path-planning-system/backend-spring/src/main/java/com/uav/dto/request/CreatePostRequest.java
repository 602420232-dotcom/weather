package com.uav.dto.request;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

@Data
public class CreatePostRequest {

    @NotBlank(message = "标题不能为空")
    @Size(max = 255, message = "标题长度不能超过255个字符")
    private String title;

    @NotBlank(message = "内容不能为空")
    private String content;

    @NotBlank(message = "板块不能为空")
    private String section;

    private List<String> tags;
}