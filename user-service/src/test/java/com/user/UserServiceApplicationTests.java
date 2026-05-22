package com.user;

import com.u.common.constant.GatewayAuthConstant;
import com.u.common.constant.JwtClaimsConstant;
import com.u.common.context.BaseContext;
import com.u.common.properties.GatewayAuthProperties;
import com.u.common.security.JwtAuthInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserServiceApplicationTests {

    @Test
    void shouldRestoreBaseContextFromGatewayHeaders() throws Exception {
        GatewayAuthProperties gatewayAuthProperties = new GatewayAuthProperties();
        gatewayAuthProperties.setAuthSecret("gateway-secret");

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .addInterceptors(new JwtAuthInterceptor(gatewayAuthProperties))
                .build();

        MvcResult result = mockMvc.perform(get("/user/test-context")
                        .header(GatewayAuthConstant.GATEWAY_AUTH_HEADER, "gateway-secret")
                        .header(JwtClaimsConstant.CURRENT_ID, "42")
                        .header(JwtClaimsConstant.ROLE, "ADMIN")
                        .header(JwtClaimsConstant.SOURCE_TYPE, "USER")
                        .accept(MediaType.TEXT_PLAIN))
                .andExpect(status().isOk())
                .andReturn();

        org.junit.jupiter.api.Assertions.assertEquals("42|ADMIN|USER", result.getResponse().getContentAsString());
        assertNull(BaseContext.getCurrentId());
        assertNull(BaseContext.getCurrentRole());
        assertNull(BaseContext.getCurrentSourceType());
    }

    @Controller
    static class TestController {

        @GetMapping("/user/test-context")
        @ResponseBody
        public String userContext() {
            return BaseContext.getCurrentId() + "|" + BaseContext.getCurrentRole() + "|" + BaseContext.getCurrentSourceType();
        }
    }
}
