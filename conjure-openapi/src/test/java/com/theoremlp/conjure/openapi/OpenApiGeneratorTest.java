/*
 * (c) Copyright 2022 Theorem Technology, Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.theoremlp.conjure.openapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature;
import com.google.common.collect.ImmutableList;
import com.palantir.conjure.defs.Conjure;
import com.palantir.conjure.spec.ConjureDefinition;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OpenApiGeneratorTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper(new YAMLFactory()
                    .enable(Feature.INDENT_ARRAYS_WITH_INDICATOR)
                    .disable(Feature.WRITE_DOC_START_MARKER))
            .setSerializationInclusion(JsonInclude.Include.NON_ABSENT)
            .enable(SerializationFeature.INDENT_OUTPUT)
            .addMixIn(Schema.class, SchemaMixin.class);

    @TempDir
    Path tempDir;

    private void compareOutputsForPrefix(String prefix) throws IOException {
        ConjureDefinition conjureDefinition =
                Conjure.parse(ImmutableList.of(new File("src/test/resources/" + prefix + ".yml")));
        OpenAPI api = OpenApiGenerator.generate(conjureDefinition);
        OBJECT_MAPPER.writeValue(tempDir.resolve(prefix + ".openapi.yaml").toFile(), api);
        SnapshotTesting.validateGeneratorOutput(
                ImmutableList.of(Path.of("src/test/resources/" + prefix + ".openapi.yaml")), tempDir);
    }

    @Test
    void testAlias() throws IOException {
        compareOutputsForPrefix("alias-test");
    }

    @Test
    void testEnum() throws IOException {
        compareOutputsForPrefix("enum-test");
    }

    @Test
    void testObject() throws IOException {
        compareOutputsForPrefix("object-test");
    }

    @Test
    void testList() throws IOException {
        compareOutputsForPrefix("list-test");
    }

    @Test
    void testSet() throws IOException {
        compareOutputsForPrefix("set-test");
    }

    @Test
    void testMap() throws IOException {
        compareOutputsForPrefix("map-test");
    }

    @Test
    void testUnion() throws IOException {
        compareOutputsForPrefix("union-test");
    }
}
