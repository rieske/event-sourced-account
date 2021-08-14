package lt.rieske.accounts;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.core.importer.Location;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures;
import lt.rieske.accounts.api.ApiConfiguration;

import java.util.regex.Pattern;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;


@AnalyzeClasses(packages = "lt.rieske.accounts", importOptions = ArchitectureTest.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule onionArchitecture = Architectures.onionArchitecture()
            .domainModels("lt.rieske.accounts.domain", "lt.rieske.accounts.eventsourcing")
            .adapter("eventstore", "lt.rieske.accounts.eventstore")
            .adapter("api", "lt.rieske.accounts.api")
            .applicationServices("lt.rieske.accounts", "lt.rieske.accounts.infrastructure")
            .ignoreDependency(App.class, lt.rieske.accounts.eventstore.Configuration.class)
            .ignoreDependency(App.class, lt.rieske.accounts.eventstore.BlobEventStore.class)
            .ignoreDependency(App.class, lt.rieske.accounts.api.ApiConfiguration.class)
            .ignoreDependency(App.class, lt.rieske.accounts.api.Server.class)
            .ignoreDependency(lt.rieske.accounts.api.ApiConfiguration.class, lt.rieske.accounts.eventstore.Configuration.class)
            .ignoreDependency(lt.rieske.accounts.api.ApiConfiguration.EventStoreSupplier.class, lt.rieske.accounts.eventstore.BlobEventStore.class)
            .withOptionalLayers(true);

    @ArchTest
    static final ArchRule domainShouldNotDependOnEventstore =
            noClasses().that().resideInAPackage("lt.rieske.accounts.domain")
                    .should().dependOnClassesThat().resideInAPackage("lt.rieske.accounts.eventstore");

    @ArchTest
    static final ArchRule domainShouldNotDependOnApi =
            noClasses().that().resideInAPackage("lt.rieske.accounts.domain")
                    .should().dependOnClassesThat().resideInAPackage("lt.rieske.accounts.api");

    @ArchTest
    static final ArchRule eventSourcingShouldNotDependOnEventstore =
            noClasses().that().resideInAPackage("lt.rieske.accounts.eventsourcing")
                    .should().dependOnClassesThat().resideInAPackage("lt.rieske.accounts.eventstore");

    @ArchTest
    static final ArchRule eventSourcingShouldNotDependOnApi =
            noClasses().that().resideInAPackage("lt.rieske.accounts.eventsourcing")
                    .should().dependOnClassesThat().resideInAPackage("lt.rieske.accounts.api");

    @ArchTest
    static final ArchRule eventSourcingShouldNotDependOnDomain =
            noClasses().that().resideInAPackage("lt.rieske.accounts.eventsourcing")
                    .should().dependOnClassesThat().resideInAPackage("lt.rieske.accounts.domain");

    @ArchTest
    static final ArchRule eventstoreShouldNotDependOnApi =
            noClasses().that().resideInAPackage("lt.rieske.accounts.eventstore")
                    .should().dependOnClassesThat().resideInAPackage("lt.rieske.accounts.api");

    static class DoNotIncludeTests implements ImportOption {
        private static final Pattern TESTS_PATTERN = Pattern.compile(".*/build/classes/([^/]+/)?test/.*");
        private static final Pattern TEST_FIXTURES_PATTERN = Pattern.compile(".*/build/libs/.*test-fixtures.jar!/.*");

        @Override
        public boolean includes(Location location) {
            if (location.matches(TESTS_PATTERN)) {
                return false;
            }
            return !location.matches(TEST_FIXTURES_PATTERN);
        }
    }
}

