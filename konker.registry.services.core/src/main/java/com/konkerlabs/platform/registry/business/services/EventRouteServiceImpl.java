package com.konkerlabs.platform.registry.business.services;

import java.net.URI;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.konkerlabs.platform.registry.business.services.api.ApplicationService;
import com.konkerlabs.platform.registry.business.services.api.DeviceModelService;
import com.konkerlabs.platform.registry.business.services.api.DeviceRegisterService;
import com.konkerlabs.platform.registry.business.services.api.EventRouteService;
import com.konkerlabs.platform.registry.business.services.api.LocationService;
import com.konkerlabs.platform.registry.business.services.api.RestDestinationService;
import com.konkerlabs.platform.registry.business.services.api.ServiceResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.konkerlabs.platform.registry.business.model.Application;
import com.konkerlabs.platform.registry.business.model.Device;
import com.konkerlabs.platform.registry.business.model.DeviceModel;
import com.konkerlabs.platform.registry.business.model.DeviceModelLocation;
import com.konkerlabs.platform.registry.business.model.EventRoute;
import com.konkerlabs.platform.registry.business.model.Location;
import com.konkerlabs.platform.registry.business.model.EventRoute.RouteActor;
import com.konkerlabs.platform.registry.business.model.RestDestination;
import com.konkerlabs.platform.registry.business.model.Tenant;
import com.konkerlabs.platform.registry.business.model.behaviors.DeviceURIDealer;
import com.konkerlabs.platform.registry.business.model.behaviors.RESTDestinationURIDealer;
import com.konkerlabs.platform.registry.business.model.validation.CommonValidations;
import com.konkerlabs.platform.registry.business.repositories.ApplicationRepository;
import com.konkerlabs.platform.registry.business.repositories.DeviceModelRepository;
import com.konkerlabs.platform.registry.business.repositories.DeviceRepository;
import com.konkerlabs.platform.registry.business.repositories.EventRouteRepository;
import com.konkerlabs.platform.registry.business.repositories.LocationRepository;
import com.konkerlabs.platform.registry.business.repositories.RestDestinationRepository;
import com.konkerlabs.platform.registry.business.repositories.TenantRepository;
import com.konkerlabs.platform.registry.business.services.api.ServiceResponseBuilder;

@Service
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class EventRouteServiceImpl implements EventRouteService {

    private Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private TenantRepository tenantRepository;
    @Autowired
    private ApplicationRepository applicationRepository;
    @Autowired
    private EventRouteRepository eventRouteRepository;
    @Autowired
    private DeviceRepository deviceRepository;
    @Autowired
    private DeviceModelRepository deviceModelRepository;
    @Autowired
    private LocationRepository locationRepository;
    @Autowired
    private RestDestinationRepository restRepository;

    @Override
    public ServiceResponse<EventRoute> save(Tenant tenant, Application application, EventRoute route) {
        if (!Optional.ofNullable(tenant).isPresent())
            return ServiceResponseBuilder.<EventRoute>error()
                    .withMessage(CommonValidations.TENANT_NULL.getCode()).build();

        if (!Optional.ofNullable(route).isPresent())
            return ServiceResponseBuilder.<EventRoute>error()
                    .withMessage(CommonValidations.RECORD_NULL.getCode()).build();

        Tenant existingTenant = tenantRepository.findByDomainName(tenant.getDomainName());

        if (!Optional.ofNullable(existingTenant).isPresent())
            return ServiceResponseBuilder.<EventRoute>error()
                    .withMessage(CommonValidations.TENANT_DOES_NOT_EXIST.getCode()).build();

        if (!Optional.ofNullable(application).isPresent())
            return ServiceResponseBuilder.<EventRoute>error()
                    .withMessage(ApplicationService.Validations.APPLICATION_NULL.getCode()).build();

        Application existingApplication = applicationRepository.findByTenantAndName(tenant.getId(), application.getName());

        if (!Optional.ofNullable(existingApplication).isPresent())
            return ServiceResponseBuilder.<EventRoute>error()
                    .withMessage(ApplicationService.Validations.APPLICATION_NOT_FOUND.getCode()).build();

        route.setId(null);
        route.setTenant(existingTenant);
        route.setApplication(existingApplication);
        route.setGuid(UUID.randomUUID().toString());

        Optional<Map<String,Object[]>> validations = route.applyValidations();

        if (validations.isPresent()) {
            return ServiceResponseBuilder.<EventRoute>error()
                .withMessages(validations.get()).build();
        }

        if (Optional.ofNullable(eventRouteRepository.findByRouteName(tenant.getId(),
                                                                     application.getName(),
                                                                     route.getName())).isPresent()) {
            return ServiceResponseBuilder.<EventRoute>error()
                    .withMessage(Validations.NAME_IN_USE.getCode()).build();
        }

        Map<String,Object[]> routeActorValidations = applyRouteActorValidations(application, route);

        if (!routeActorValidations.isEmpty()) {
            return ServiceResponseBuilder.<EventRoute>error()
                                         .withMessages(routeActorValidations)
                                         .build();
        }

        EventRoute saved = eventRouteRepository.save(route);

        LOGGER.info("Route created. Name: {}", route.getName(), tenant.toURI(), tenant.getLogLevel());

        return ServiceResponseBuilder.<EventRoute>ok().withResult(saved).build();
    }

    @Override
    public ServiceResponse<EventRoute> update(Tenant tenant, Application application, String guid, EventRoute eventRoute) {
        if (!Optional.ofNullable(tenant).isPresent())
            return ServiceResponseBuilder.<EventRoute>error()
                    .withMessage(CommonValidations.TENANT_NULL.getCode()).build();

        if (!Optional.ofNullable(eventRoute).isPresent())
            return ServiceResponseBuilder.<EventRoute>error()
                    .withMessage(CommonValidations.RECORD_NULL.getCode()).build();

        Tenant existingTenant = tenantRepository.findByDomainName(tenant.getDomainName());

        if (!Optional.ofNullable(existingTenant).isPresent())
            return ServiceResponseBuilder.<EventRoute>error()
                    .withMessage(CommonValidations.TENANT_DOES_NOT_EXIST.getCode()).build();

        if (!Optional.ofNullable(application).isPresent())
            return ServiceResponseBuilder.<EventRoute>error()
                    .withMessage(ApplicationService.Validations.APPLICATION_NULL.getCode()).build();

        Application existingApplication = applicationRepository.findByTenantAndName(tenant.getId(), application.getName());

        if (!Optional.ofNullable(existingApplication).isPresent())
            return ServiceResponseBuilder.<EventRoute>error()
                    .withMessage(ApplicationService.Validations.APPLICATION_NOT_FOUND.getCode()).build();

        if (!Optional.ofNullable(guid).filter(s -> !s.isEmpty()).isPresent()) {
            return ServiceResponseBuilder.<EventRoute>error()
                    .withMessage(Validations.GUID_NULL.getCode())
                    .build();
        }

        EventRoute current = eventRouteRepository.findByGuid(
            tenant.getId(),
            application.getName(),
            guid
        );

        if (!Optional.ofNullable(current).isPresent())
            return ServiceResponseBuilder.<EventRoute>error()
                    .withMessage(Validations.EVENT_ROUTE_NOT_FOUND.getCode())
                    .build();

        current.setActive(eventRoute.isActive());
        current.setDescription(eventRoute.getDescription());
        current.setFilteringExpression(eventRoute.getFilteringExpression());
        current.setIncoming(eventRoute.getIncoming());
        current.setName(eventRoute.getName());
        current.setOutgoing(eventRoute.getOutgoing());
        current.setTransformation(eventRoute.getTransformation());

        Optional<Map<String,Object[]>> validations = current.applyValidations();

        if (validations.isPresent()) {
            return ServiceResponseBuilder.<EventRoute>error()
                    .withMessages(validations.get())
                    .build();
        }

        if (Optional.ofNullable(eventRouteRepository.findByRouteName(tenant.getId(),
                                                                     application.getName(),
                                                                     current.getName()))
                .filter(eventRoute1 -> !eventRoute1.getGuid().equals(current.getGuid()))
                .isPresent()) {
            return ServiceResponseBuilder.<EventRoute>error()
                    .withMessage(Validations.NAME_IN_USE.getCode())
                    .build();
        }

        Map<String,Object[]> routeActorValidations = applyRouteActorValidations(application, current);

        if (!routeActorValidations.isEmpty()) {
            return ServiceResponseBuilder.<EventRoute>error()
                                         .withMessages(routeActorValidations)
                                         .build();
        }

        EventRoute saved = eventRouteRepository.save(current);

        LOGGER.info("Route updated. Name: {}", saved.getName(), tenant.toURI(), tenant.getLogLevel());

        return ServiceResponseBuilder.<EventRoute>ok().withResult(saved).build();
    }

    private Map<String, Object[]> applyRouteActorValidations(Application application, EventRoute route) {

        Map<String,Object[]> validations = new HashMap<>();

        validations.putAll(applyRouteActorValidations(application, route.getIncoming()));
        validations.putAll(applyRouteActorValidations(application, route.getOutgoing()));

        return validations;

    }


    private Map<String,Object[]> applyRouteActorValidations(Application application, RouteActor actor) {

        Map<String,Object[]> validations = new HashMap<>();

        String tenantId = application.getTenant().getId();

        switch (actor.getUri().getScheme()) {
            case DeviceURIDealer.DEVICE_URI_SCHEME:

                Device device = deviceRepository.findByTenantAndGuid(tenantId,
                        actor.getUri().getPath().replace("/", ""));

                if (!Optional.ofNullable(device).isPresent()) {
                    validations.put(DeviceRegisterService.Validations.DEVICE_GUID_DOES_NOT_EXIST.getCode(), null);
                    return validations;
                }
                if (!application.equals(device.getApplication())) {
                    validations.put(Validations.CROSS_APPLICATION.getCode(), null);
                    return validations;
                }

                actor.setDisplayName(device.getDeviceId());
                break;

            case RESTDestinationURIDealer.REST_DESTINATION_URI_SCHEME:

                RestDestination rest = restRepository.getByTenantAndGUID(tenantId,
                        application.getName(),
                        actor.getUri().getPath().replace("/", ""));

                if (!Optional.ofNullable(rest).isPresent()) {
                    validations.put(RestDestinationService.Validations.DESTINATION_NOT_FOUND.getCode(), null);
                    return validations;
                }

                actor.setDisplayName(rest.getName());
                break;

            case DeviceModelLocation.URI_SCHEME:

                String uriPath = actor.getUri().getPath();
                if (uriPath.startsWith("/")) {
                    uriPath = uriPath.substring(1);
                }

                String guids[] = uriPath.split("/");
                if (guids.length < 2) {
                    validations.put(Validations.GUID_NULL.getCode(), null);
                    return validations;
                }

                DeviceModel deviceModel = deviceModelRepository.findByTenantIdApplicationNameAndGuid(
                        tenantId, application.getName(), guids[0]);

                if (!Optional.ofNullable(deviceModel).isPresent()) {
                    validations.put(DeviceModelService.Validations.DEVICE_MODEL_NOT_FOUND.getCode(), null);
                    return validations;
                }

                Location location = locationRepository.findByTenantAndApplicationAndGuid(
                        tenantId, application.getName(), guids[1]);

                if (!Optional.ofNullable(location).isPresent()) {
                    validations.put(LocationService.Validations.LOCATION_GUID_DOES_NOT_EXIST.getCode(), null);
                    return validations;
                }

                actor.setDisplayName(MessageFormat.format("{0} @ {1}", deviceModel.getName(), location.getName()));
                break;

            default:
                LOGGER.warn("{} not supported", actor.getUri().getScheme());

        }

        return validations;

    }

    @Override
    public ServiceResponse<List<EventRoute>> getAll(Tenant tenant, Application application) {
        if (!Optional.ofNullable(tenant).isPresent())
            return ServiceResponseBuilder.<List<EventRoute>>error()
                    .withMessage(CommonValidations.TENANT_NULL.getCode()).build();

        Tenant existingTenant = tenantRepository.findByDomainName(tenant.getDomainName());

        if (!Optional.ofNullable(existingTenant).isPresent())
            return ServiceResponseBuilder.<List<EventRoute>>error()
                    .withMessage(CommonValidations.TENANT_DOES_NOT_EXIST.getCode()).build();

        if (!Optional.ofNullable(application).isPresent())
            return ServiceResponseBuilder.<List<EventRoute>>error()
                    .withMessage(ApplicationService.Validations.APPLICATION_NULL.getCode()).build();

        Application existingApplication = applicationRepository.findByTenantAndName(tenant.getId(), application.getName());

        if (!Optional.ofNullable(existingApplication).isPresent())
            return ServiceResponseBuilder.<List<EventRoute>>error()
                    .withMessage(ApplicationService.Validations.APPLICATION_NOT_FOUND.getCode()).build();

        return ServiceResponseBuilder.<List<EventRoute>>ok()
            .withResult(eventRouteRepository.findAll(existingTenant.getId(), existingApplication.getName()))
            .build();
    }

    @Override
    public ServiceResponse<EventRoute> getByGUID(Tenant tenant, Application application, String guid) {
        if (!Optional.ofNullable(tenant).isPresent())
            return ServiceResponseBuilder.<EventRoute>error()
                    .withMessage(CommonValidations.TENANT_NULL.getCode()).build();

        Tenant existingTenant = tenantRepository.findByDomainName(tenant.getDomainName());

        if (!Optional.ofNullable(existingTenant).isPresent())
            return ServiceResponseBuilder.<EventRoute>error()
                    .withMessage(CommonValidations.TENANT_DOES_NOT_EXIST.getCode()).build();

        if (!Optional.ofNullable(application).isPresent())
            return ServiceResponseBuilder.<EventRoute>error()
                    .withMessage(ApplicationService.Validations.APPLICATION_NULL.getCode()).build();

        Application existingApplication = applicationRepository.findByTenantAndName(tenant.getId(), application.getName());

        if (!Optional.ofNullable(existingApplication).isPresent())
            return ServiceResponseBuilder.<EventRoute>error()
                    .withMessage(ApplicationService.Validations.APPLICATION_NOT_FOUND.getCode()).build();

        if (!Optional.ofNullable(guid).isPresent())
            return ServiceResponseBuilder.<EventRoute>error()
                    .withMessage(Validations.GUID_NULL.getCode())
                    .build();

        EventRoute route = eventRouteRepository.findByGuid(tenant.getId(), application.getName(), guid);

        if (!Optional.ofNullable(route).isPresent())
            return ServiceResponseBuilder.<EventRoute>error()
                    .withMessage(Validations.EVENT_ROUTE_NOT_FOUND.getCode())
                    .build();

        return ServiceResponseBuilder.<EventRoute>ok().withResult(route)
                .build();
    }

    @Override
    public ServiceResponse<List<EventRoute>> findByIncomingUri(URI uri) {
        if (!Optional.ofNullable(uri).isPresent())
            return ServiceResponseBuilder.<List<EventRoute>>error()
                    .withMessage(Validations.EVENT_ROUTE_URI_NULL.getCode())
                    .build();

        List<EventRoute> eventRoutes = eventRouteRepository.findByIncomingUri(uri);

        return ServiceResponseBuilder.<List<EventRoute>>ok()
                .withResult(eventRoutes)
                .build();
    }

    @Override
    public ServiceResponse<EventRoute> remove(Tenant tenant, Application application, String guid) {
        if (!Optional.ofNullable(tenant).isPresent())
            return ServiceResponseBuilder.<EventRoute>error()
                    .withMessage(CommonValidations.TENANT_NULL.getCode()).build();

        Tenant existingTenant = tenantRepository.findByDomainName(tenant.getDomainName());

        if (!Optional.ofNullable(existingTenant).isPresent())
            return ServiceResponseBuilder.<EventRoute>error()
                    .withMessage(CommonValidations.TENANT_DOES_NOT_EXIST.getCode()).build();

        if (!Optional.ofNullable(guid).filter(s -> !s.isEmpty()).isPresent())
            return ServiceResponseBuilder.<EventRoute>error()
                    .withMessage(Validations.GUID_NULL.getCode())
                    .build();

        if (!Optional.ofNullable(application).isPresent())
            return ServiceResponseBuilder.<EventRoute>error()
                    .withMessage(ApplicationService.Validations.APPLICATION_NULL.getCode()).build();

        Application existingApplication = applicationRepository.findByTenantAndName(tenant.getId(), application.getName());

        if (!Optional.ofNullable(existingApplication).isPresent())
            return ServiceResponseBuilder.<EventRoute>error()
                    .withMessage(ApplicationService.Validations.APPLICATION_NOT_FOUND.getCode()).build();

        EventRoute route = eventRouteRepository.findByGuid(existingTenant.getId(), application.getName(), guid);

        if (!Optional.ofNullable(route).isPresent())
            return ServiceResponseBuilder.<EventRoute>error()
                    .withMessage(Validations.EVENT_ROUTE_NOT_FOUND.getCode())
                    .build();

        eventRouteRepository.delete(route);

        LOGGER.info("Route removed. Name: {}", route.getName(), tenant.toURI(), tenant.getLogLevel());

        return ServiceResponseBuilder.<EventRoute>ok()
                .withResult(route)
                .build();
    }
}
