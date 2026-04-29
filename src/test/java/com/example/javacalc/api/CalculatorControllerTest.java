package com.example.javacalc.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class CalculatorControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void simplifyReturnsStructuredResponse() throws Exception {
        mockMvc.perform(post("/api/v1/simplify")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "normalFunctions": ["f(x,y)=x+y"],
                      "recursiveFunctions": [],
                      "expression": "f(x,2)"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result").value("x+2"));
    }

    @Test
    void missingExpressionReturnsStructuredError() throws Exception {
        mockMvc.perform(post("/api/v1/simplify")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "normalFunctions": [],
                      "recursiveFunctions": []
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
            .andExpect(jsonPath("$.message").value("请求参数错误"));
    }

    @Test
    void wrongRecursiveLineCountReturnsStructuredError() throws Exception {
        mockMvc.perform(post("/api/v1/simplify")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "normalFunctions": [],
                      "recursiveFunctions": [["f{0}(x)=x", "f{1}(x)=x^2"]],
                      "expression": "f{2}(x)"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void healthReturnsUp() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"));
    }
}
