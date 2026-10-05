package com.Saikiran.CodeReviewer.dto;

import jakarta.validation.constraints.NotBlank;

public class PasteCodeRequest {

    @NotBlank(message = "Code cannot be blank")
    private String code;

    private String language;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
}
