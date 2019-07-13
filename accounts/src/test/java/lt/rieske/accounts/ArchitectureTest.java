package lt.rieske.accounts;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchUnitRunner;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;


@RunWith(ArchUnitRunner.class)
@AnalyzeClasses(packages = "lt.rieske.accounts", importOptions = ImportOption.DoNotIncludeTests.class)
public class ArchitectureTest {

    @ArchTest
    public static final ArchRule domainShouldNotAccessClassesInEventSourcing =
            noClasses().that().resideInAPackage("lt.rieske.accounts.domain")
                    .should().accessClassesThat().resideInAPackage("lt.rieske.accounts.eventsourcing");

    @ArchTest
    public static final ArchRule domainShouldNotDependOnInfrastructure =
            noClasses().that().resideInAPackage("lt.rieske.accounts.domain")
                    .should().dependOnClassesThat().resideInAPackage("lt.rieske.accounts.infrastructure");
}