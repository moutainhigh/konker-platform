package com.konkerlabs.platform.registry.test.web.controllers;


import com.konkerlabs.platform.registry.business.model.DataEnrichmentExtension;
import com.konkerlabs.platform.registry.business.model.Device;
import com.konkerlabs.platform.registry.business.model.Tenant;
import com.konkerlabs.platform.registry.business.model.behaviors.DeviceURIDealer;
import com.konkerlabs.platform.registry.business.services.api.DataEnrichmentExtensionService;
import com.konkerlabs.platform.registry.business.services.api.DeviceRegisterService;
import com.konkerlabs.platform.registry.business.services.api.ServiceResponse;
import com.konkerlabs.platform.registry.config.WebMvcConfig;
import com.konkerlabs.platform.registry.test.base.SecurityTestConfiguration;
import com.konkerlabs.platform.registry.test.base.WebLayerTestContext;
import com.konkerlabs.platform.registry.web.forms.EnrichmentForm;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = {
        WebMvcConfig.class,
        SecurityTestConfiguration.class,
        EnrichmentControllerTest.EnrichmentTestContextConfig.class
})
public class EnrichmentControllerTest extends WebLayerTestContext {

    @Autowired
    private DeviceRegisterService deviceRegisterService;
    @Autowired
    private DataEnrichmentExtensionService dataEnrichmentExtensionService;
    @Autowired
    private Tenant tenant;

    private Device incomingDevice;
    private DeviceURIDealer deviceUriDealer;
    private DataEnrichmentExtension dataEnrichmentExtension;
    private ServiceResponse<List<DataEnrichmentExtension>> listServiceResponse;
    private ServiceResponse<DataEnrichmentExtension> serviceResponse;
    private EnrichmentForm enrichmentForm;
    private MultiValueMap<String, String> enrichmentData;

    private String enrichmentId = "a09b3f34-db24-11e5-8a31-7b3889d9b0eb";

    @Before
    public void setUp() {
//        tenant = Tenant.builder().name("tenantName").domainName("tenantDomain").build();
        incomingDevice = Device.builder().tenant(tenant).deviceId("1").build();
        deviceUriDealer = new DeviceURIDealer() {};

        enrichmentForm = new EnrichmentForm();
        enrichmentForm.setName("EnrichmentTest1");
        enrichmentForm.setDescription("Testing the enrichment form.");
        enrichmentForm.setType("REST");
        enrichmentForm.setIncomingAuthority("device://tenantDomain/1");
        enrichmentForm.setParameters(new HashMap<String, String>() {{
            put("URL", "http://my.enriching.service.com");
            put("User", "admin");
            put("Password", "secret");
        }});
        enrichmentForm.setContainerKey("fieldTest");
        enrichmentForm.setActive(true);

        enrichmentForm.setAdditionalSupplier(() -> tenant.getDomainName());

        dataEnrichmentExtension = DataEnrichmentExtension.builder()
                .name(enrichmentForm.getName())
                .description(enrichmentForm.getDescription())
                .incoming(deviceUriDealer.toDeviceRuleURI(tenant.getDomainName(), "1"))
                .type(DataEnrichmentExtension.EnrichmentType.REST)
                .parameters(enrichmentForm.getParameters())
                .containerKey(enrichmentForm.getContainerKey())
                .active(true)
                .build();

        enrichmentData = new LinkedMultiValueMap<>();
        enrichmentData.add("name", enrichmentForm.getName());
        enrichmentData.add("description", enrichmentForm.getDescription());
        enrichmentData.add("type", enrichmentForm.getType());
        enrichmentData.add("incomingAuthority", enrichmentForm.getIncomingAuthority());
        enrichmentData.add("parameters", "");

        serviceResponse = ServiceResponse.<DataEnrichmentExtension>builder()
                .status(ServiceResponse.Status.OK)
                .result(dataEnrichmentExtension)
                .<DataEnrichmentExtension>build();

        listServiceResponse = ServiceResponse.<List<DataEnrichmentExtension>>builder()
                .status(ServiceResponse.Status.OK)
                .result(Arrays.asList(new DataEnrichmentExtension[]{dataEnrichmentExtension}))
                .<List<DataEnrichmentExtension>>build();
    }

    @After
    public void tearDown() {
        Mockito.reset(dataEnrichmentExtensionService);
        Mockito.reset(deviceRegisterService);
    }

    @Test
    public void shouldListAllSavedEnrichmentRegisters() throws Exception {
        when(dataEnrichmentExtensionService.getAll(tenant)).thenReturn(listServiceResponse);

        getMockMvc().perform(get("/enrichment")).andExpect(model().attribute("dataEnrichmentExtensions", equalTo(listServiceResponse.getResult())))
                .andExpect(view().name("enrichment/index"));
    }

    @Test
    public void shouldShowCreationForm() throws Exception {
        getMockMvc().perform(get("/enrichment/new"))
                .andExpect(view().name("enrichment/form"))
                .andExpect(model().attribute("dataEnrichmentExtension", new EnrichmentForm()))
                .andExpect(model().attribute("action", "/enrichment/save"));
    }

    @Test
    public void shouldBindErrorMessagesWhenRegistrationFailsAndGoBackToCreationForm() throws Exception {

    }

    @Test
    public void shouldRedirectToShowAfterSuccessfulEnrichmentCreation() throws Exception {

    }

    @Test
    public void shouldShowEditForm() throws Exception {
        when(dataEnrichmentExtensionService.getByName(tenant, dataEnrichmentExtension.getName())).thenReturn(serviceResponse);

        getMockMvc().perform(get(MessageFormat.format("/enrichment/{0}/edit", dataEnrichmentExtension.getName())))
                .andExpect(view().name("enrichment/form"))
                .andExpect(model().attribute("dataEnrichmentExtension", new EnrichmentForm().fillFrom(dataEnrichmentExtension)))
                .andExpect(model().attribute("action", MessageFormat.format("/enrichment/{0}", dataEnrichmentExtension.getName())));
    }

    @Test
    public void shouldRedirectToShowAfterSuccessfulEnrichmentEdit() throws Exception {
//        String newName = "NewEnrichmentName";
//        dataEnrichmentExtension.setId(enrichmentId);
//        dataEnrichmentExtension.setName(newName);
//        serviceResponse = spy(ServiceResponse.<DataEnrichmentExtension>builder()
//                .status(ServiceResponse.Status.OK)
//                .result(dataEnrichmentExtension)
//                .<DataEnrichmentExtension>build());
//
//        when(dataEnrichmentExtensionService.update(eq(tenant), eq(dataEnrichmentExtension))).thenReturn(serviceResponse);
//
//        getMockMvc().perform(post(MessageFormat.format("/enrichment/{0}", dataEnrichmentExtension.getName())).params(enrichmentData))
//                .andExpect(redirectedUrl(MessageFormat.format("/enrichment/{0}", dataEnrichmentExtension.getName())))
//                .andExpect(flash().attribute("message", "Enrichment registered successfully"));
//
//        verify(dataEnrichmentExtensionService).update(eq(tenant),eq(dataEnrichmentExtension));
    }

    @Test
    public void shouldShowRuleDetails() throws Exception {
        when(dataEnrichmentExtensionService.getByName(tenant, dataEnrichmentExtension.getName())).thenReturn(serviceResponse);

        getMockMvc().perform(get(MessageFormat.format("/enrichment/{0}", dataEnrichmentExtension.getName())))
                .andExpect(view().name("enrichment/show"))
                .andExpect(model().attribute("dataEnrichmentExtension", new EnrichmentForm().fillFrom(dataEnrichmentExtension)));
    }

    @Configuration
    static class EnrichmentTestContextConfig {
        @Bean
        public DataEnrichmentExtensionService dataEnrichmentExtensionService() {
            return mock(DataEnrichmentExtensionService.class);
        }

        @Bean
        public DeviceRegisterService deviceRegisterService() {
            return mock(DeviceRegisterService.class);
        }
    }
}