package com.konkerlabs.platform.registry.business.services;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.konkerlabs.platform.registry.business.exceptions.BusinessException;
import com.konkerlabs.platform.registry.business.model.DataEnrichmentExtension;
import com.konkerlabs.platform.registry.business.model.Tenant;
import com.konkerlabs.platform.registry.business.repositories.DataEnrichmentExtensionRepository;
import com.konkerlabs.platform.registry.business.repositories.TenantRepository;
import com.konkerlabs.platform.registry.business.services.api.DataEnrichmentExtensionService;
import com.konkerlabs.platform.registry.business.services.api.ServiceResponse;

@Service
public class DataEnrichmentExtensionServiceImpl implements DataEnrichmentExtensionService {

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private DataEnrichmentExtensionRepository repository;

    @Override
    public ServiceResponse<DataEnrichmentExtension> register(Tenant tenant, DataEnrichmentExtension dee) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Not Implemented");
    }

    @Override
    public ServiceResponse<DataEnrichmentExtension> save(Tenant tenant, DataEnrichmentExtension dee) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Not Implemented");
    }

    @Override
    public ServiceResponse<List<DataEnrichmentExtension>> getAll(Tenant tenant) {
        // TODO Auto-generated method stub

        return ServiceResponse.<List<DataEnrichmentExtension>> builder().status(ServiceResponse.Status.OK)
                .result(Collections.emptyList()).build();
        // throw new UnsupportedOperationException("Not Implemented");
    }

    @Override
    public ServiceResponse<DataEnrichmentExtension> findByName(Tenant tenant, String name) {
        try {
            Optional.ofNullable(name).orElseThrow(() -> new BusinessException("Name cannot be null"));
            Optional.ofNullable(tenant).orElseThrow(() -> new BusinessException("Tenant cannot be null"));

            Tenant t = tenantRepository.findByName(tenant.getName());
            Optional.ofNullable(t).orElseThrow(() -> new BusinessException("Tenant does not exist"));

            DataEnrichmentExtension dee = Optional.ofNullable(repository.findByTenantIdAndName(t.getId(), name))
                    .orElseThrow(() -> new BusinessException("Data Enrichment Extension does not exist"));

            return ServiceResponse.<DataEnrichmentExtension> builder()
                    .status(ServiceResponse.Status.OK)
                    .result(dee)
                    .build();
        } catch (BusinessException be) {
            return ServiceResponse.<DataEnrichmentExtension> builder()
                    .status(ServiceResponse.Status.ERROR)
                    .responseMessage(be.getMessage())
                    .build();
        }
    }

}