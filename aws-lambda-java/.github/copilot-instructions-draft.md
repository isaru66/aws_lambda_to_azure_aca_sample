
Migration Plan: AWS Lambda Java to Containerized Java App

Target: Azure Container Apps, Azure AKS

Should be completed in 7 steps, each with detailed instructions.
--------------------------------------------------------------------------------
PHASE 1: ASSESS & INITIALIZE SPRING BOOT PROJECT
--------------------------------------------------------------------------------
- Inventory all existing AWS Lambda functions and document:
  - Handler class
  - Trigger type (API Gateway, SQS, etc.)
  - Input/output structure
  - External dependencies (S3, DynamoDB, etc.)

- Folder structure:
  src/main/java/isaru66

- Configure application.yml for environments.

--------------------------------------------------------------------------------
PHASE 2: DEFINE DOMAIN & APPLICATION LAYERS
--------------------------------------------------------------------------------
- Move business logic from Lambda handler to use case services.

- Domain Layer:
  - Plain Java classes (e.g., User.java)
  - Interfaces for ports (e.g., UserRepository)

- Application Layer:
  - Service classes (e.g., UserService.java)
  - Implements business orchestration logic

--------------------------------------------------------------------------------
PHASE 3: IMPLEMENT ADAPTER (INFRASTRUCTURE) LAYER
--------------------------------------------------------------------------------
- Create adapter/persistence layer:
  - Use Spring Data JPA (e.g., UserJpaRepository)
  - Or manual in-memory repository for tests

- External service access (S3, etc.) goes here.
- Annotate with @Repository or @Component.

--------------------------------------------------------------------------------
PHASE 4: DEVELOP CONTROLLER LAYER
--------------------------------------------------------------------------------
- Replace Lambda triggers with @RestController endpoints.

- Use OpenAPI annotations via springdoc-openapi-ui.

- Map request/response DTOs to domain models.

- Use proper HTTP verbs, response codes, and validation.

--------------------------------------------------------------------------------
PHASE 5: INCREMENTAL MIGRATION & VERIFICATION
--------------------------------------------------------------------------------
- Migrate Lambdas function-by-function:
  - Extract logic → domain/application layer
  - Create matching controller
  - Write unit + integration tests
  - Compare behavior against Lambda (input/output)

- Add error handling:
  - Use @ControllerAdvice for global exception handler

- Add validation:
  - Use @Valid with javax.validation annotations

- Add structured logging:
  - Use SLF4J (Logback or Log4j2)

- Test in parallel with Lambda if needed.

--------------------------------------------------------------------------------
PHASE 6: CONTAINERIZATION & AZURE DEPLOYMENT
--------------------------------------------------------------------------------
- Create Dockerfile:
  FROM eclipse-temurin:17-jdk
  COPY target/app.jar app.jar
  ENTRYPOINT ["java", "-jar", "app.jar"]

- Build & push to Azure Container Registry (ACR):
  az acr build --image springboot-api:1.0.0 --registry myacr --file Dockerfile .

- Create ACA environment (if not yet):
  az containerapp env create --name aca-env --resource-group rg-springboot --location eastasia

- Deploy with YAML:

springboot-containerapp.yaml:
  name: springboot-api
  type: Microsoft.App/containerApps@2023-05-01
  location: eastasia
  properties:
    environmentId: /subscriptions/<subscription-id>/resourceGroups/rg-springboot/providers/Microsoft.App/managedEnvironments/aca-env
    configuration:
      ingress:
        external: true
        targetPort: 8080
        transport: auto
        allowInsecure: false
      registries:
        - server: myacr.azurecr.io
          identity: system
      secrets:
        - name: spring_profile
          value: "prod"
    template:
      containers:
        - name: springboot-api
          image: myacr.azurecr.io/springboot-api:1.0.0
          resources:
            cpu: 0.5
            memory: 1Gi
          env:
            - name: SPRING_PROFILES_ACTIVE
              secretRef: spring_profile
      scale:
        minReplicas: 1
        maxReplicas: 5
        rules:
          - name: http-scaling
            http:
              metadata:
                concurrentRequests: "100"

- Deploy using:
  az containerapp create --resource-group rg-springboot --file springboot-containerapp.yaml


--------------------------------------------------------------------------------
BUILD TOOLING
--------------------------------------------------------------------------------
- For consistency and enterprise alignment, **use only Maven** for building and managing the Spring Boot application.
  - If your existing AWS Lambda project used Gradle, migrate the build configuration to **Maven** during the transition.
  - Remove all Gradle-related files after migration:
    - `build.gradle` or `build.gradle.kts`
    - `settings.gradle`
    - `.gradle/` directory
    - `gradlew` and `gradlew.bat`

- Set up Maven wrapper in the root of the project:
  - Run: `mvn -N io.takari:maven:wrapper`
  - This adds:
    - `mvnw` and `mvnw.cmd`
    - `.mvn/wrapper` directory

- Recommended Maven plugins for Spring Boot:
  - spring-boot-maven-plugin
  - maven-compiler-plugin
  - build-helper-maven-plugin (optional)

Example `pom.xml` snippet:
<build>
  <plugins>
    <plugin>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-maven-plugin</artifactId>
    </plugin>
  </plugins>
</build>


--------------------------------------------------------------------------------
PHASE 7: Provide script AZURE INFRASTRUCTURE USING BICEP
--------------------------------------------------------------------------------
Use Bicep to provision infrastructure as code for consistent environments.

Components to provision:
- Azure Container Registry (ACR)
- Azure Container Apps Environment
- Log Analytics Workspace (for tracing)
- Azure PostgreSQL Flexible Server (for test/prod)
- Azure Cache for Redis (optional for dev/testing)
- App Configuration / Key Vault (optional)

Example: infrastructure/main.bicep

param location string = 'Southeast Asia'
param acrName string
param logAnalyticsName string
param postgresName string
param containerAppEnvName string

resource acr 'Microsoft.ContainerRegistry/registries@2023-01-01-preview' = {
  name: acrName
  location: location
  sku: {
    name: 'Basic'
  }
}

resource logAnalytics 'Microsoft.OperationalInsights/workspaces@2021-12-01-preview' = {
  name: logAnalyticsName
  location: location
  sku: {
    name: 'PerGB2018'
  }
  properties: {
    retentionInDays: 30
  }
}

resource postgres 'Microsoft.DBforPostgreSQL/flexibleServers@2022-12-01' = {
  name: postgresName
  location: location
  sku: {
    name: 'Standard_B1ms',
    tier: 'Burstable',
    capacity: 1,
    family: 'B'
  }
  properties: {
    administratorLogin: 'dbadmin'
    storage: {
      storageSizeGB: 32
    }
    version: '14'
    availabilityZone: '1'
    backup: {
      backupRetentionDays: 7
    }
  }
}

resource containerEnv 'Microsoft.App/managedEnvironments@2023-05-01' = {
  name: containerAppEnvName
  location: location
  properties: {
    appLogsConfiguration: {
      destination: 'log-analytics'
      logAnalyticsConfiguration: {
        customerId: logAnalytics.properties.customerId
        sharedKey: listKeys(logAnalytics.id, logAnalytics.apiVersion).primarySharedKey
      }
    }
  }
}

--------------------------------------------------------------------------------
ENVIRONMENT STRATEGY
--------------------------------------------------------------------------------
Use different configurations per environment:

DEV:
- Use in-memory H2 database
- Optional: Use Azure Cache for Redis for local caching simulation
- Container App uses 'dev' profile
- Enable OpenAPI UI and verbose logging

TEST:
- Use in-memory H2 database
- Use dev config (disable debug endpoints)
- Container App uses 'test' profile

PROD:
- Use separate PostgreSQL Flexible Server
- Enable diagnostics via Azure Monitor + Log Analytics
- Container App uses 'prod' profile
- Secrets and DB creds from Key Vault (optional)

--------------------------------------------------------------------------------
APPLICATION TRACING (REQUIRED FOR ALL ENVIRONMENTS)
--------------------------------------------------------------------------------
Enable distributed tracing and observability in Spring Boot:
- Add Spring Boot Starter for Micrometer + Azure Monitor

Add dependencies:
pom.xml
<dependency>
  <groupId>com.microsoft.azure</groupId>
  <artifactId>applicationinsights-spring-boot-starter</artifactId>
  <version>3.4.8</version>
</dependency>

Enable tracing config:
application.yml
azure:
  application-insights:
    connection-string: ${APPINSIGHTS_CONNECTION_STRING}

Log every major service, repository, and controller call with:
- SLF4J logger
- MDC correlation IDs
- Custom tracing IDs if needed

Use Log Analytics query in Azure to trace by operationId or correlationId

This ensures every function call is traceable across environments.

--------------------------------------------------------------------------------
POST-MIGRATION CLEANUP
--------------------------------------------------------------------------------
Once the migration is complete and validated:
- Remove all AWS Lambda-related folders and files:
  - Delete handler classes implementing `RequestHandler`
  - Delete `lambda` packages or folders (e.g., `src/main/java/com/example/lambda`)
  - Delete AWS-specific dependencies in `pom.xml` or `build.gradle`:
    - aws-lambda-java-core
    - aws-lambda-java-events
    - aws-lambda-java-log4j2
    - AWS SDK libraries if no longer needed
  - Remove Lambda-specific `sam.yaml` or `template.yaml`
  - Delete deployment scripts or JSON files related to AWS

- Clean up test code related to Lambda
- Delete old README sections or documentation referring to Lambda

- Confirm that:
  - All functionality has parity in the Spring Boot app
  - The container is deployed successfully to Azure (ACA/AKS/App Service)
  - Monitoring, logging, and scaling behave as expected

This cleanup helps ensure your codebase is clean, focused, and avoids future confusion or accidental AWS re-deployment.

--------------------------------------------------------------------------------
BEST PRACTICES
--------------------------------------------------------------------------------
- Keep domain layer framework-free.
- Use constructor-based dependency injection.
- Use DTOs to isolate API contracts.
- Enable OpenAPI docs for client clarity.
- Avoid mixing responsibilities across layers.
- Follow clean logging (no sensitive data).
- Write unit and integration tests per function.
- Use environment profiles for config separation.
- Containerize with slim images (multi-stage builds).