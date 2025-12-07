package com.autoever.mes.common.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class OracleTestWebController {
    
    @GetMapping("/oracle-test")
    public String oracleTest() {
        return "oracle-test";
    }
}
