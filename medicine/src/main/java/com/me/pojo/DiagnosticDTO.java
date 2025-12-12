package com.me.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DiagnosticDTO {
    private Integer diagnosticId;
    private Integer recordId;
    private String result;
    private String orders;
    private String prescription;
} 