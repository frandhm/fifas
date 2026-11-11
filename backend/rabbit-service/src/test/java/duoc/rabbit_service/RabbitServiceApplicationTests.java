package duoc.rabbit_service;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
class RabbitServiceApplicationTests {

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public ConnectionFactory connectionFactory() {
            ConnectionFactory factory = mock(ConnectionFactory.class);
            Connection conn = mock(Connection.class);
            when(factory.createConnection()).thenReturn(conn);
            return factory;
        }
    }

    @Test
    void contextLoads() {
    }
}
