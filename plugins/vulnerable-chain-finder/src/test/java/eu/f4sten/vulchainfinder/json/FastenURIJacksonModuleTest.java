package eu.f4sten.vulchainfinder.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.fasten.core.data.FastenJavaURI;
import eu.fasten.core.data.FastenURI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FastenURIJacksonModuleTest {

    ObjectMapper om;

    @BeforeEach
    void setUp() {
       om = new ObjectMapper().registerModule(new FastenURIJacksonModule());
    }

    @Test
    void testJavaUriRoundTrip() throws JsonProcessingException {
        final var uriStr =
            "fasten://mvn!eu.fasten-project.tests.syntheticjars:lib$0.0.1/lib/VehicleWash.wash(MotorVehicle)%2Fjava.lang%2FVoidType";
        test(uriStr, FastenJavaURI.class);
    }

    @Test
    void testUriRoundTrip() throws JsonProcessingException {
        final var uriStr =
            "/lib/VehicleWash.wash(MotorVehicle)%2Fjava.lang%2FVoidType";
        test(uriStr, FastenURI.class);
    }

    private void test(final String uriStr, Class<?> type) throws JsonProcessingException {
        var first = FastenURI.create(uriStr);
        final var middle = om.writeValueAsString(first);
        final var end = om.readValue(middle, FastenURI.class);
        assertInstanceOf(type, end);
        assertEquals(end, first);
    }

}