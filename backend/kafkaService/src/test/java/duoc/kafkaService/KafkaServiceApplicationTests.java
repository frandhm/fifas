package duoc.kafkaService;

import org.apache.kafka.streams.StreamsBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;

@SpringBootTest(properties = {
    "spring.kafka.listener.auto-startup=false",
    "spring.kafka.streams.auto-startup=false"
})
class KafkaServiceApplicationTests {

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public StreamsBuilder streamsBuilder() {
            return new StreamsBuilder();
        }
    }

    @MockBean
    private KafkaAdmin kafkaAdmin;

    @MockBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @Test
    void contextLoads() {
    }
}
