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

import com.palantir.conjure.spec.ConjureDefinition;
import io.swagger.v3.oas.models.OpenAPI;
import java.io.File;
import java.io.IOException;
import picocli.CommandLine;

@CommandLine.Command(
        name = "conjure-openapi",
        description = "CLI to generate OpenApi from Conjure API definitions.",
        mixinStandardHelpOptions = true,
        subcommands = OpenApiCli.GenerateCommand.class)
public final class OpenApiCli implements Runnable {
    public static void main(String[] args) {
        CommandLine.run(new OpenApiCli(), args);
    }

    @Override
    public void run() {
        CommandLine.usage(this, System.out);
    }

    @CommandLine.Command(
            name = "generate",
            description = "Generate OpenApi for a Conjure API",
            mixinStandardHelpOptions = true,
            usageHelpWidth = 120)
    public static final class GenerateCommand implements Runnable {
        @CommandLine.Parameters(paramLabel = "<input>", description = "Path to the input IR file", index = "0")
        private String input;

        @CommandLine.Parameters(
                paramLabel = "<output>",
                description = "Output directory for generated source",
                index = "1")
        private String output;

        @Override
        public void run() {
            try {
                ConjureDefinition conjureDefinition =
                        Mapper.OBJECT_MAPPER.readValue(new File(input), ConjureDefinition.class);
                OpenAPI api = OpenApiGenerator.generate(conjureDefinition);
                Mapper.OBJECT_MAPPER.writeValue(new File(output, "openapi.yaml"), api);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
