package com.konkerlabs.platform.registry.business.services.api;

import java.util.List;

import com.konkerlabs.platform.registry.business.model.Application;
import com.konkerlabs.platform.registry.business.model.Device;
import com.konkerlabs.platform.registry.business.model.Location;
import com.konkerlabs.platform.registry.business.model.Tenant;

public interface LocationSearchService {

    ServiceResponse<Location> findRoot(Tenant tenant, Application application);
    ServiceResponse<Location> findDefault(Tenant tenant, Application application);
    ServiceResponse<Location> findByName(Tenant tenant, Application application, String locationName, boolean loadTree);
    ServiceResponse<Location> findByGuid(Tenant tenant, Application application, String locationName);
    ServiceResponse<List<Location>> findAll(Tenant tenant, Application application);

    ServiceResponse<List<Device>> listDevicesByLocationName(Tenant tenant, Application application, String locationName);


}