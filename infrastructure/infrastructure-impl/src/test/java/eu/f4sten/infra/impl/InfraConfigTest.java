/*
 * Copyright 2022 Delft University of Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.f4sten.infra.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.stefanbirkner.systemlambda.SystemLambda;

import dev.c0ps.diapper.AssertArgsError;
import dev.c0ps.diapper.RunnerArgs;
import dev.c0ps.franz.Lane;

public class InfraConfigTest {

    private InfraArgs args;
    private InfraConfig sut;

    @BeforeEach
    public void setup() {
        args = new InfraArgs();
        sut = new InfraConfig(args);
    }

    @Test
    public void kafkaConnector_basic() {
        args.kafkaUrl = "u";
        assertKafkaConnectorValues("p", //
                "u", "p", null);
    }

    @Test
    public void kafkaConnector_hasGrp() {
        args.kafkaUrl = "u";
        args.kafkaGroupId = "g";
        assertKafkaConnectorValues("p", //
                "u", "g", null);
    }

    @Test
    public void kafkaConnector_hasInst() {
        args.kafkaUrl = "u";
        args.instanceId = "i";
        assertKafkaConnectorValues("p", //
                "u", "p", "i");
    }

    @Test
    public void kafkaConnector_hasGrpAndInst() {
        args.kafkaUrl = "u";
        args.kafkaGroupId = "g";
        args.instanceId = "i";
        assertKafkaConnectorValues("p", //
                "u", "g", "i");
    }

    @Test
    public void kafkaConnector_fastenReplacedInPlugin() {
        args.kafkaUrl = "u";
        assertKafkaConnectorValues("eu.f4sten.X", //
                "u", "X", null);
    }

    @Test
    public void kafkaConnector_mainReplacedAtPluginEnd() {
        args.kafkaUrl = "u";
        assertKafkaConnectorValues("p.Main", //
                "u", "p", null);
    }

    @Test
    public void kafkaConnector_mainNotReplacedInPluginMiddle() {
        args.kafkaUrl = "u";
        assertKafkaConnectorValues("a.Main.b", //
                "u", "a.Main.b", null);
    }

    @Test
    public void kafkaConnector_mainNotReplacedAtGrp() {
        args.kafkaUrl = "u";
        args.kafkaGroupId = "g.Main";
        assertKafkaConnectorValues("p", //
                "u", "g.Main", null);
    }

    @Test
    public void kafkaConnector_failNullUrl() {
        args.kafkaUrl = null;
        assertThrows(AssertArgsError.class, () -> {
            bindKafkaConnector();
        });
    }

    @Test
    public void kafkaConnector_failEmptyInstanceId() {
        args.kafkaUrl = "u";
        args.instanceId = "";
        assertThrows(AssertArgsError.class, () -> {
            bindKafkaConnector();
        });
    }

    @Test
    public void kafkaConnector_failEmptyGroupId() {
        args.kafkaUrl = "u";
        args.kafkaGroupId = "";
        assertThrows(AssertArgsError.class, () -> {
            bindKafkaConnector();
        });
    }

    private void bindKafkaConnector() throws Exception {
        var args = new RunnerArgs();
        args.run = "someplugin";
        SystemLambda.tapSystemOut(() -> {
            sut.bindKafkaConnector(args);
        });
    }

    private void assertKafkaConnectorValues(String plugin, String expectedUrl, String expectedGrpId, String expectedInstanceId) {
        var args = new RunnerArgs();
        args.run = plugin;
        var prop = sut.bindKafkaConnector(args).getConsumerProperties(Lane.NORMAL);

        var actualUrl = prop.getProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG);
        assertEquals(expectedUrl, actualUrl);

        var actualGrpId = prop.getProperty(ConsumerConfig.GROUP_ID_CONFIG);
        if (actualGrpId != null) {
            actualGrpId = actualGrpId.substring(0, actualGrpId.indexOf('-'));
        }
        assertEquals(expectedGrpId, actualGrpId);

        var actualInstanceId = prop.getProperty(ConsumerConfig.CLIENT_ID_CONFIG);
        if (actualInstanceId != null) {
            actualInstanceId = actualInstanceId.substring(actualInstanceId.indexOf('-') + 1);
            actualInstanceId = actualInstanceId.substring(0, actualInstanceId.indexOf('-'));
        }
        assertEquals(expectedInstanceId, actualInstanceId);
    }
}