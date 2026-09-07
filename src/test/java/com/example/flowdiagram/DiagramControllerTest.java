package com.example.flowdiagram;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DiagramControllerTest {

    private static final String VALID = """
            {
              "title": "テストフロー",
              "states": [
                { "label": "未開始", "kind": "status",
                  "actions": [ { "type": "button", "label": "開始する", "next": "開始" } ] },
                { "label": "開始", "kind": "procedure" }
              ]
            }
            """;

    @Autowired
    private MockMvc mvc;

    private String body(MvcResult r) throws Exception {
        return r.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    void diagramReturnsFullHtml() throws Exception {
        MvcResult r = mvc.perform(post("/api/diagram")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isOk())
                .andReturn();
        String html = body(r);
        assertTrue(html.startsWith("<!DOCTYPE html>"));
        assertTrue(html.contains("fd-canvas"));
        assertTrue(html.contains("テストフロー"));
        assertFalse(html.contains("<svg"));
    }

    @Test
    void cloneHighlightIsCssOnlyAndWorksInBothPageAndFragment() throws Exception {
        String backEdgeJson = """
                {
                  "states": [
                    { "label": "A", "actions": [ { "type": "button", "label": "go", "next": "B" } ] },
                    { "label": "B", "actions": [ { "type": "button", "label": "back", "next": "A" } ] }
                  ]
                }
                """;
        MvcResult page = mvc.perform(post("/api/diagram")
                        .contentType(MediaType.APPLICATION_JSON).content(backEdgeJson))
                .andExpect(status().isOk())
                .andReturn();
        assertTrue(body(page).contains("node-clone-ref"));
        assertTrue(body(page).contains(":has(~ .node-clone-ref.lbl-"), "CSSの:has()による強調ルールが出ること");
        assertFalse(body(page).contains("scrollIntoView"), "旧クリックジャンプ用JSは廃止済みであること");

        MvcResult fragment = mvc.perform(post("/api/diagram/fragment")
                        .contentType(MediaType.APPLICATION_JSON).content(backEdgeJson))
                .andExpect(status().isOk())
                .andReturn();
        assertTrue(body(fragment).contains(":has(~ .node-clone-ref.lbl-"), "fragment でも同じCSSが出ること（JS不要のため）");
    }

    @Test
    void fragmentHasNoDoctype() throws Exception {
        MvcResult r = mvc.perform(post("/api/diagram/fragment")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isOk())
                .andReturn();
        String html = body(r);
        assertFalse(html.contains("<!DOCTYPE"));
        assertTrue(html.contains("<style>"));
    }

    @Test
    void unknownNextReturns400WithMessage() throws Exception {
        String json = """
                { "states": [ { "label": "A", "actions": [ { "label": "go", "next": "zzz" } ] } ] }
                """;
        mvc.perform(post("/api/diagram")
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.messages[0]",
                        org.hamcrest.Matchers.containsString("zzz")));
    }

    @Test
    void emptyStatesReturns400() throws Exception {
        mvc.perform(post("/api/diagram")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"states\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void brokenJsonReturns400() throws Exception {
        mvc.perform(post("/api/diagram")
                        .contentType(MediaType.APPLICATION_JSON).content("{ not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void sampleIsRenderable() throws Exception {
        MvcResult sample = mvc.perform(get("/api/sample"))
                .andExpect(status().isOk())
                .andReturn();
        String json = body(sample);
        mvc.perform(post("/api/diagram")
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk());
    }

    @Test
    void previewPageIsServed() throws Exception {
        // 静的リソースは MockMvc 上では forward として解決される
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .forwardedUrl("index.html"));
    }

    @Test
    void themeLayoutOverrideChangesOutput() throws Exception {
        String json = """
                {
                  "states": [ { "label": "A" } ],
                  "theme": { "background": "#ff00ff" }
                }
                """;
        MvcResult r = mvc.perform(post("/api/diagram")
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andReturn();
        assertTrue(body(r).contains("#ff00ff"));
    }
}
