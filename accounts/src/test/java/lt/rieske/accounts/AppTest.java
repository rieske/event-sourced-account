package lt.rieske.accounts;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AppTest {
    @Test
    public void appHasAGreeting() {
        App classUnderTest = new App();

        assertThat(classUnderTest.getGreeting()).isNotNull();
    }
}
