package lt.rieske.accounts;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.core.importer.Location;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import java.util.regex.Pattern;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;


@AnalyzeClasses(packages = "lt.rieske.accounts", importOptions = ArchitectureTest.DoNotIncludeTests.class)
class ArchitectureTest {

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

