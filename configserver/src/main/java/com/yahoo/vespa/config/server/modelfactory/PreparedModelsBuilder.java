// Copyright Verizon Media. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.vespa.config.server.modelfactory;

import com.yahoo.cloud.config.ConfigserverConfig;
import com.yahoo.component.Version;
import com.yahoo.config.application.api.ApplicationPackage;
import com.yahoo.config.application.api.DeployLogger;
import com.yahoo.config.model.api.ConfigChangeAction;
import com.yahoo.config.model.api.ConfigDefinitionRepo;
import com.yahoo.config.model.api.HostInfo;
import com.yahoo.config.model.api.HostProvisioner;
import com.yahoo.config.model.api.Model;
import com.yahoo.config.model.api.ModelContext;
import com.yahoo.config.model.api.ModelCreateResult;
import com.yahoo.config.model.api.ModelFactory;
import com.yahoo.config.model.api.Provisioned;
import com.yahoo.config.model.api.ValidationParameters;
import com.yahoo.config.model.api.ValidationParameters.IgnoreValidationErrors;
import com.yahoo.config.model.application.provider.FilesApplicationPackage;
import com.yahoo.config.model.deploy.DeployState;
import com.yahoo.config.provision.AllocatedHosts;
import com.yahoo.config.provision.ApplicationId;
import com.yahoo.config.provision.DockerImage;
import com.yahoo.vespa.config.server.application.Application;
import com.yahoo.vespa.config.server.application.ApplicationCuratorDatabase;
import com.yahoo.vespa.config.server.application.ApplicationSet;
import com.yahoo.vespa.config.server.application.PermanentApplicationPackage;
import com.yahoo.vespa.config.server.deploy.ModelContextImpl;
import com.yahoo.vespa.config.server.filedistribution.FileDistributionProvider;
import com.yahoo.vespa.config.server.host.HostValidator;
import com.yahoo.vespa.config.server.provision.HostProvisionerProvider;
import com.yahoo.vespa.config.server.session.PrepareParams;
import com.yahoo.vespa.curator.Curator;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * @author bratseth
 */
public class PreparedModelsBuilder extends ModelsBuilder<PreparedModelsBuilder.PreparedModelResult> {

    private static final Logger log = Logger.getLogger(PreparedModelsBuilder.class.getName());

    private final PermanentApplicationPackage permanentApplicationPackage;
    private final ConfigDefinitionRepo configDefinitionRepo;
    private final HostValidator<ApplicationId> hostValidator;
    private final PrepareParams params;
    private final FileDistributionProvider fileDistributionProvider;
    private final Optional<ApplicationSet> currentActiveApplicationSet;
    private final ModelContext.Properties properties;
    private final Curator curator;

    public PreparedModelsBuilder(ModelFactoryRegistry modelFactoryRegistry,
                                 PermanentApplicationPackage permanentApplicationPackage,
                                 ConfigDefinitionRepo configDefinitionRepo,
                                 FileDistributionProvider fileDistributionProvider,
                                 HostProvisionerProvider hostProvisionerProvider,
                                 Curator curator,
                                 HostValidator<ApplicationId> hostValidator,
                                 DeployLogger deployLogger,
                                 PrepareParams params,
                                 Optional<ApplicationSet> currentActiveApplicationSet,
                                 ModelContext.Properties properties,
                                 ConfigserverConfig configserverConfig) {
        super(modelFactoryRegistry, configserverConfig, properties.zone(), hostProvisionerProvider, deployLogger);
        this.permanentApplicationPackage = permanentApplicationPackage;
        this.configDefinitionRepo = configDefinitionRepo;
        this.fileDistributionProvider = fileDistributionProvider;
        this.hostValidator = hostValidator;
        this.curator = curator;
        this.params = params;
        this.currentActiveApplicationSet = currentActiveApplicationSet;
        this.properties = properties;
    }

    @Override
    protected PreparedModelResult buildModelVersion(ModelFactory modelFactory,
                                                    ApplicationPackage applicationPackage,
                                                    ApplicationId applicationId,
                                                    Optional<DockerImage> wantedDockerImageRepository,
                                                    Version wantedNodeVespaVersion,
                                                    Optional<AllocatedHosts> allocatedHosts) {
        Version modelVersion = modelFactory.version();
        log.log(Level.FINE, () -> "Building model " + modelVersion + " for " + applicationId);

        // Use empty on non-hosted systems, use already allocated hosts if available, create connection to a host provisioner otherwise
        Provisioned provisioned = new Provisioned();
        ModelContext modelContext = new ModelContextImpl(
                applicationPackage,
                modelOf(modelVersion),
                permanentApplicationPackage.applicationPackage(),
                deployLogger(),
                configDefinitionRepo,
                fileDistributionProvider.getFileRegistry(),
                new ApplicationCuratorDatabase(applicationId.tenant(), curator).readReindexingStatus(applicationId),
                createHostProvisioner(applicationPackage, provisioned),
                provisioned,
                properties,
                getAppDir(applicationPackage),
                wantedDockerImageRepository,
                modelVersion,
                wantedNodeVespaVersion);

        ModelCreateResult result = createAndValidateModel(modelFactory, applicationId, modelVersion, modelContext);
        return new PreparedModelResult(modelVersion, result.getModel(), fileDistributionProvider, result.getConfigChangeActions());
    }

    private ModelCreateResult createAndValidateModel(ModelFactory modelFactory, ApplicationId applicationId, Version modelVersion, ModelContext modelContext) {
        log.log(properties.zone().system().isCd() ? Level.INFO : Level.FINE,
                () -> "Create and validate model " + modelVersion + " for " + applicationId + ", previous model is " +
                modelOf(modelVersion).map(Model::version).map(Version::toFullString).orElse("non-existing"));
        ValidationParameters validationParameters =
                new ValidationParameters(params.ignoreValidationErrors() ? IgnoreValidationErrors.TRUE : IgnoreValidationErrors.FALSE);
        ModelCreateResult result = modelFactory.createAndValidateModel(modelContext, validationParameters);
        validateModelHosts(hostValidator, applicationId, result.getModel());
        log.log(Level.FINE, () -> "Done building model " + modelVersion + " for " + applicationId);
        params.getTimeoutBudget().assertNotTimedOut(() -> "prepare timed out after building model " + modelVersion +
                                                          " (timeout " + params.getTimeoutBudget().timeout() + "): " + applicationId);
        return result;
    }

    private Optional<Model> modelOf(Version version) {
        if (currentActiveApplicationSet.isEmpty()) return Optional.empty();
        return currentActiveApplicationSet.get().get(version).map(Application::getModel);
    }

    private HostProvisioner createHostProvisioner(ApplicationPackage applicationPackage, Provisioned provisioned) {
        HostProvisioner defaultHostProvisioner = DeployState.getDefaultModelHostProvisioner(applicationPackage);
        // Note: nodeRepositoryProvisioner will always be present when hosted is true
        Optional<HostProvisioner> nodeRepositoryProvisioner = createNodeRepositoryProvisioner(properties.applicationId(), provisioned);
        Optional<AllocatedHosts> allocatedHosts = applicationPackage.getAllocatedHosts();

        if (allocatedHosts.isEmpty()) return nodeRepositoryProvisioner.orElse(defaultHostProvisioner);

        // Nodes are already allocated by a model and we should use them unless this model requests hosts from a
        // previously unallocated cluster. This allows future models to stop allocate certain clusters.
        if (hosted) return createStaticProvisionerForHosted(allocatedHosts.get(), nodeRepositoryProvisioner.get());

        return defaultHostProvisioner;
    }

    private Optional<File> getAppDir(ApplicationPackage applicationPackage) {
        try {
            return applicationPackage instanceof FilesApplicationPackage ?
                   Optional.of(((FilesApplicationPackage) applicationPackage).getAppDir()) :
                   Optional.empty();
        } catch (IOException e) {
            throw new RuntimeException("Could not find app dir", e);
        }
    }

    private void validateModelHosts(HostValidator<ApplicationId> hostValidator, ApplicationId applicationId, Model model) {
        // Will retry here, since hosts used might not be in sync on all config servers (we wait for 2/3 servers
        // to respond to deployments and deletions).
        Instant end = Instant.now().plus(Duration.ofSeconds(1));
        IllegalArgumentException exception;
        do {
            try {
                hostValidator.verifyHosts(applicationId, model.getHosts().stream()
                                                              .map(HostInfo::getHostname)
                                                              .collect(Collectors.toList()));
                return;
            } catch (IllegalArgumentException e) {
                exception = e;
                log.log(Level.INFO, "Verifying hosts failed, will retry: " + e.getMessage());
                try {
                    Thread.sleep(100);
                } catch (InterruptedException interruptedException) {/* ignore */}
            }
        } while (Instant.now().isBefore(end));

        throw exception;
    }

    /** The result of preparing a single model version */
    public static class PreparedModelResult implements ModelResult {

        public final Version version;
        public final Model model;
        public final FileDistributionProvider fileDistributionProvider;
        public final List<ConfigChangeAction> actions;

        public PreparedModelResult(Version version,
                                   Model model,
                                   FileDistributionProvider fileDistributionProvider,
                                   List<ConfigChangeAction> actions) {
            this.version = version;
            this.model = model;
            this.fileDistributionProvider = fileDistributionProvider;
            this.actions = actions;
        }

        @Override
        public Model getModel() {
            return model;
        }

    }

}
