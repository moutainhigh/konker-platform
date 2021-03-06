package com.konkerlabs.platform.registry.business.repositories;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.konkerlabs.platform.registry.business.model.SilenceTrigger;

public interface SilenceTriggerRepository  extends MongoRepository<SilenceTrigger, String> {

    @Query("{ 'tenant.id' : ?0, 'application.name' : ?1 }")
    List<SilenceTrigger> listByTenantIdAndApplicationName(String tenantId, String applicationName);

    @Query("{ 'tenant.id' : ?0, 'application.name' : ?1, 'deviceModel.id' : ?2, 'location.id' : ?3 }")
    SilenceTrigger findByTenantAndApplicationAndDeviceModelAndLocation(String tenantId, String applicationName, String deviceModelId, String locationId);

    @Query("{ 'tenant.id' : ?0, 'application.name' : ?1, 'guid' : ?2 }")
    SilenceTrigger findByTenantIdApplicationIdAndGuid(String tenantId, String applicationName, String guid);

}
