package org.namewta.profile.person.controller.admin;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotLoginException;
import org.namewta.common.satoken.handler.SaTokenExceptionHandler;
import org.namewta.common.web.handler.GlobalExceptionHandler;
import org.namewta.profile.person.controller.self.PersonMaterialSelfController;
import org.namewta.profile.person.domain.exception.ProfileMaterialException;
import org.namewta.profile.person.dao.ProfileMaterialDao;
import org.namewta.profile.person.domain.model.read.MaterialRequirementRow;
import org.namewta.profile.person.port.security.ProfileMaterialAccessPolicy;
import org.namewta.profile.person.service.ProfileMaterialService;
import org.namewta.profile.person.usecase.impl.ProfileMaterialUseCaseImpl;
import org.namewta.profile.person.usecase.ProfileMaterialUseCase;
import org.springframework.http.MediaType;
import org.namewta.system.api.OssService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 验证页面材料查询复用数据库规则的选择条件；鉴权与真实 SQL 另由集成场景覆盖。 */
@Tag("dev")
class MaterialRequirementsHttpContractTest {
    private final ProfileMaterialDao dao = mock(ProfileMaterialDao.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new MaterialTagController(
        new ProfileMaterialUseCaseImpl(new ProfileMaterialService(dao, mock(OssService.class),
            mock(ProfileMaterialAccessPolicy.class), List.of())))).build();

    @Test
    void permissionFailureRetains403WhenGlobalAdviceIsRegisteredFirst() throws Exception {
        var useCase = mock(ProfileMaterialUseCase.class);
        when(useCase.attach(any())).thenThrow(new NotPermissionException("profile:person:material:self"));
        var endpoint = MockMvcBuilders.standaloneSetup(new PersonMaterialSelfController(useCase))
            .setControllerAdvice(new GlobalExceptionHandler(), new SaTokenExceptionHandler()).build();
        endpoint.perform(post("/profile/person/materials/WORKING/123")
                .contentType(MediaType.APPLICATION_JSON).content("{\"ossId\":456,\"materialNodeId\":789}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void expiredSessionRetains401WhenGlobalAdviceIsRegisteredFirst() throws Exception {
        var useCase = mock(ProfileMaterialUseCase.class);
        when(useCase.attach(any())).thenThrow(new NotLoginException("login", NotLoginException.TOKEN_TIMEOUT, "expired fixture"));
        var endpoint = MockMvcBuilders.standaloneSetup(new PersonMaterialSelfController(useCase))
            .setControllerAdvice(new GlobalExceptionHandler(), new SaTokenExceptionHandler()).build();
        endpoint.perform(post("/profile/person/materials/WORKING/123")
                .contentType(MediaType.APPLICATION_JSON).content("{\"ossId\":456,\"materialNodeId\":789}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void materialFailureRetainsItsBusinessCodeWhenGlobalAdviceIsRegisteredFirst() throws Exception {
        ProfileMaterialUseCase useCase = mock(ProfileMaterialUseCase.class);
        when(useCase.attach(any())).thenThrow(new ProfileMaterialException("MISSING_REQUIRED_MATERIAL:PERSON_ID_CARD_PORTRAIT"));
        MockMvc withGlobalAdvice = MockMvcBuilders.standaloneSetup(new PersonMaterialSelfController(useCase))
            .setControllerAdvice(new GlobalExceptionHandler(), new ProfileMaterialExceptionHandler()).build();
        withGlobalAdvice.perform(post("/profile/person/materials/WORKING/123")
                .contentType(MediaType.APPLICATION_JSON).content("{\"ossId\":456,\"materialNodeId\":789}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.msg").value("MISSING_REQUIRED_MATERIAL:PERSON_ID_CARD_PORTRAIT"));
    }

    @Test
    void personUsesSelectedDocumentRules() throws Exception {
        when(dao.selectRequirements("PERSON", "CN_RESIDENT_ID", Set.of("ALWAYS"))).thenReturn(List.of(
            new MaterialRequirementRow("PERSON_ID_CARD_PORTRAIT", 1),
            new MaterialRequirementRow("PERSON_ID_CARD_EMBLEM", 1)));
        mvc.perform(get("/profile/material-tags/requirements").param("profileType", "PERSON")
                .param("documentTypeCode", "CN_RESIDENT_ID"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[1].materialTagCode").value("PERSON_ID_CARD_EMBLEM"))
            .andExpect(jsonPath("$.data[1].minimumCount").value(1));
    }

    @Test
    void enterpriseSelectsAuthorizationLetterOnlyForASeparateHandler() throws Exception {
        var ordinary = List.of(new MaterialRequirementRow("ENTERPRISE_BUSINESS_LICENSE", 1),
            new MaterialRequirementRow("ENTERPRISE_LEGAL_REPRESENTATIVE_DOCUMENT", 1));
        when(dao.selectRequirements("ENTERPRISE", "*", Set.of("ALWAYS"))).thenReturn(ordinary);
        when(dao.selectRequirements("ENTERPRISE", "*", Set.of("ALWAYS", "HANDLER_NOT_LEGAL_REPRESENTATIVE")))
            .thenReturn(List.of(ordinary.getFirst(), ordinary.getLast(),
                new MaterialRequirementRow("ENTERPRISE_AUTHORIZATION_LETTER", 1)));
        mvc.perform(get("/profile/material-tags/requirements").param("profileType", "ENTERPRISE")
                .param("documentTypeCode", "CN_PASSPORT").param("handlerIsLegalRepresentative", "true"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(2));
        mvc.perform(get("/profile/material-tags/requirements").param("profileType", "ENTERPRISE")
                .param("documentTypeCode", "CN_PASSPORT").param("handlerIsLegalRepresentative", "false"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(3))
            .andExpect(jsonPath("$.data[2].materialTagCode").value("ENTERPRISE_AUTHORIZATION_LETTER"));
    }

    @Test
    void malformedOrMissingSelectorsDoNotQueryRules() throws Exception {
        mvc.perform(get("/profile/material-tags/requirements").param("profileType", "PERSON"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/profile/material-tags/requirements").param("profileType", "OTHER")
                .param("documentTypeCode", "CN_RESIDENT_ID"))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(dao);
    }
}
