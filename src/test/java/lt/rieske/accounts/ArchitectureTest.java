package lt.rieske.accounts;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;


@AnalyzeClasses(packages = "lt.rieske.accounts", importOptions = ImportOption.DoNotIncludeTests.class)
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
}

