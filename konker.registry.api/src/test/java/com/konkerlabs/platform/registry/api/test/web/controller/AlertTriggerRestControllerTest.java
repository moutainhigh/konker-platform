package com.konkerlabs.platform.registry.api.test.web.controller;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.konkerlabs.platform.registry.api.config.WebMvcConfig;
import com.konkerlabs.platform.registry.api.test.config.MongoTestConfig;
import com.konkerlabs.platform.registry.api.test.config.WebTestConfiguration;
import com.konkerlabs.platform.registry.api.web.controller.AlertTriggerRestController;
import com.konkerlabs.platform.registry.api.web.wrapper.CrudResponseAdvice;
import com.konkerlabs.platform.registry.business.model.AlertTrigger;
import com.konkerlabs.platform.registry.business.model.Application;
import com.konkerlabs.platform.registry.business.model.DeviceModel;
import com.konkerlabs.platform.registry.business.model.Location;
import com.konkerlabs.platform.registry.business.model.SilenceTrigger;
import com.konkerlabs.platform.registry.business.model.Tenant;
import com.konkerlabs.platform.registry.business.services.api.AlertTriggerService;
import com.konkerlabs.platform.registry.business.services.api.ApplicationService;
import com.konkerlabs.platform.registry.business.services.api.DeviceModelService;
import com.konkerlabs.platform.registry.business.services.api.LocationSearchService;
import com.konkerlabs.platform.registry.business.services.api.ServiceResponseBuilder;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = AlertTriggerRestController.class)
@AutoConfigureMockMvc(secure = false)
@ContextConfiguration(classes = {
        WebTestConfiguration.class,
        MongoTestConfig.class,
        WebMvcConfig.class,
        CrudResponseAdvice.class
})
public class AlertTriggerRestControllerTest extends WebLayerTestContext {

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private Tenant tenant;

    @Autowired
    private Application application;

    @Autowired
    private AlertTriggerService alertTriggerService;

    @Autowired
    private DeviceModelService deviceModelService;

    @Autowired
    private LocationSearchService locationSearchService;

    private DeviceModel deviceModel;

    private Location location;

    private SilenceTrigger silenceTrigger;

    private String BASEPATH = "triggers";

    @Before
    public void setUp() {

        deviceModel = DeviceModel.builder()
                .tenant(tenant)
                .application(application)
        		.guid(UUID.randomUUID().toString())
        		.name("air conditioner")
        		.build();

        location = Location.builder()
                .tenant(tenant)
                .application(application)
                .guid(UUID.randomUUID().toString())
                .name("13th floor")
                .build();

        silenceTrigger = new SilenceTrigger();
        silenceTrigger.setGuid("2cdc391d-6a31-4103-9679-52cb6f2e5df5");
        silenceTrigger.setTenant(tenant);
        silenceTrigger.setApplication(application);
        silenceTrigger.setDeviceModel(deviceModel);
        silenceTrigger.setLocation(location);
        silenceTrigger.setMinutes(200);

        when(applicationService.getByApplicationName(tenant, application.getName()))
            .thenReturn(ServiceResponseBuilder.<Application> ok().withResult(application).build());

        when(deviceModelService.getByTenantApplicationAndName(tenant, application, deviceModel.getName()))
            .thenReturn(ServiceResponseBuilder.<DeviceModel> ok().withResult(deviceModel).build());

        when(locationSearchService.findByName(tenant, application, location.getName(), false))
            .thenReturn(ServiceResponseBuilder.<Location> ok().withResult(location).build());

    }

    @After
    public void tearDown() {
        Mockito.reset(applicationService);
    }

    @Test
    public void shouldListSilenceTriggers() throws Exception {

        List<AlertTrigger> silenceTriggers = new ArrayList<>();
        silenceTriggers.add(silenceTrigger);
        silenceTriggers.add(silenceTrigger);

        when(alertTriggerService.listByTenantAndApplication(tenant, application))
            .thenReturn(ServiceResponseBuilder.<List<AlertTrigger>> ok()
                    .withResult(silenceTriggers).build());

        getMockMvc()
                .perform(MockMvcRequestBuilders
        		.get(MessageFormat.format("/{0}/{1}/", application.getName(), BASEPATH))
        		.contentType("application/json")
        		.accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.code", is(HttpStatus.OK.value())))
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.timestamp",greaterThan(1400000000)))
                .andExpect(jsonPath("$.result", hasSize(2)))
                .andExpect(jsonPath("$.result[0].guid", is(silenceTrigger.getGuid())))
                .andExpect(jsonPath("$.result[0].deviceModelName", is(deviceModel.getName())))
                .andExpect(jsonPath("$.result[0].locationName", is(location.getName())))
                .andExpect(jsonPath("$.result[0].type", is("silence")))
                .andExpect(jsonPath("$.result[0].minutes", is(200)))
                .andExpect(jsonPath("$.result[1].guid", is(silenceTrigger.getGuid())))
                .andExpect(jsonPath("$.result[1].deviceModelName", is(deviceModel.getName())))
                .andExpect(jsonPath("$.result[1].locationName", is(location.getName())))
                .andExpect(jsonPath("$.result[1].type", is("silence")))
                .andExpect(jsonPath("$.result[1].minutes", is(200)))
                ;
    }

    @Test
    public void shouldReturnInternalErrorWhenListSilenceTriggers() throws Exception {

        when(alertTriggerService.listByTenantAndApplication(tenant, application))
            .thenReturn(ServiceResponseBuilder.<List<AlertTrigger>>error()
                    .build());

        getMockMvc().perform(MockMvcRequestBuilders
                .get(MessageFormat.format("/{0}/{1}/", application.getName(), BASEPATH))
        		.accept(MediaType.APPLICATION_JSON)
        		.contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is5xxServerError())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.code", is(HttpStatus.INTERNAL_SERVER_ERROR.value())))
                .andExpect(jsonPath("$.status", is("error")))
                .andExpect(jsonPath("$.timestamp", greaterThan(1400000000)))
                .andExpect(jsonPath("$.messages").doesNotExist())
                .andExpect(jsonPath("$.result").doesNotExist());
    }

}
