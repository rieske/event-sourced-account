package lt.rieske.accounts;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;


@AnalyzeClasses(packages = "lt.rieske.accounts", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule domainShouldNotDependOnInfrastructure =
            noClasses().that().resideInAPackage("lt.rieske.accounts.domain")
                    .should().dependOnClassesThat().resideInAPackage("lt.rieske.accounts.infrastructure");

    @ArchTest
    static final ArchRule domainShouldNotDependOnApi =
            noClasses().that().resideInAPackage("lt.rieske.accounts.domain")
                    .should().dependOnClassesThat().resideInAPackage("lt.rieske.accounts.api");

    @ArchTest
    static final ArchRule eventSourcingShouldNotDependOnInfrastructure =
            noClasses().that().resideInAPackage("lt.rieske.accounts.eventsourcing")
                    .should().dependOnClassesThat().resideInAPackage("lt.rieske.accounts.infrastructure");

    @ArchTest
    static final ArchRule eventSourcingShouldNotDependOnApi =
            noClasses().that().resideInAPackage("lt.rieske.accounts.eventsourcing")
                    .should().dependOnClassesThat().resideInAPackage("lt.rieske.accounts.api");

    @ArchTest
    static final ArchRule eventSourcingShouldNotDependOnDomain =
            noClasses().that().resideInAPackage("lt.rieske.accounts.eventsourcing")
                    .should().dependOnClassesThat().resideInAPackage("lt.rieske.accounts.domain");

    @ArchTest
    static final ArchRule infrastructureShouldNotDependOnApi =
            noClasses().that().resideInAPackage("lt.rieske.accounts.infrastructure")
                    .should().dependOnClassesThat().resideInAPackage("lt.rieske.accounts.api");
}