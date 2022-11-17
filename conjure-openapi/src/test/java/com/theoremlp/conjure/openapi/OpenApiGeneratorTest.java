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

import com.google.common.collect.ImmutableList;
import com.palantir.conjure.defs.Conjure;
import com.palantir.conjure.spec.ConjureDefinition;
import io.swagger.v3.oas.models.OpenAPI;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class OpenApiGeneratorTest {
    @TempDir
    Path tempDir;

    static List<String> getInputs() {
        try (Stream<Path> list = Files.list(Path.of("src/test/resources"))) {
            return list.filter(path -> path.getFileName().toString().endsWith("test.yml"))
                    .map(path -> com.google.common.io.Files.getNameWithoutExtension(
                            path.getFileName().toString()))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @ParameterizedTest
    @MethodSource("getInputs")
    void testConversion(String prefix) throws IOException {
        ConjureDefinition conjureDefinition =
                Conjure.parse(ImmutableList.of(new File("src/test/resources/" + prefix + ".yml")));
        OpenAPI api = OpenApiGenerator.generate(conjureDefinition);

        String content = Mapper.OBJECT_MAPPER.writeValueAsString(api);
        SnapshotTesting.validateGeneratorOutput(Path.of("src/test/resources/" + prefix + ".openapi.yaml"), content);
    }
}
