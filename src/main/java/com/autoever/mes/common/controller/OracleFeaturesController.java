package com.autoever.mes.common.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class OracleFeaturesController {
    
    @GetMapping("/oracle-features")
    public String oracleFeatures() {
        return "oracle-features";
    }
}
