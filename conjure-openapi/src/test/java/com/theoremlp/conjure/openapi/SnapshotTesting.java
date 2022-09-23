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

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.io.CharStreams;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class SnapshotTesting {

    public static String readFromFile(Path file) {
        try {
            return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String readFromResource(String path) {
        try {
            return CharStreams.toString(
                    new InputStreamReader(SnapshotTesting.class.getResourceAsStream(path), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void validateGeneratorOutput(List<Path> files, Path outputDir) throws IOException {
        validateGeneratorOutput(files, outputDir, "");
    }

    public static void validateGeneratorOutput(List<Path> files, Path outputDir, String suffix) throws IOException {
        for (Path file : files) {
            Path output = outputDir.resolve(file.getFileName() + suffix);
            if (Boolean.valueOf(System.getProperty("recreate", "false"))) {
                Files.deleteIfExists(output);
                Files.copy(file, output);
            }
            assertThat(readFromFile(file)).isEqualTo(readFromFile(output));
        }
    }

    private SnapshotTesting() {}
}
