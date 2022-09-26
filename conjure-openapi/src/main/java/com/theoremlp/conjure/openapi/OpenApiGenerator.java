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

import com.palantir.conjure.spec.AliasDefinition;
import com.palantir.conjure.spec.ConjureDefinition;
import com.palantir.conjure.spec.EnumDefinition;
import com.palantir.conjure.spec.EnumValueDefinition;
import com.palantir.conjure.spec.FieldDefinition;
import com.palantir.conjure.spec.ObjectDefinition;
import com.palantir.conjure.spec.Type;
import com.palantir.conjure.spec.TypeDefinition.Visitor;
import com.palantir.conjure.spec.UnionDefinition;
import com.palantir.conjure.visitor.TypeVisitor;
import com.palantir.logsafe.Safe;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Discriminator;
import io.swagger.v3.oas.models.media.Schema;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class OpenApiGenerator {

    private static final Type.Visitor<Schema<?>> TYPE_VISITOR = new ConjureTypeVisitor();

    private static Stream<Entry<String, Schema<?>>> convertObject(ObjectDefinition value) {
        return Stream.of(Map.entry(
                value.getTypeName().getName(),
                new Schema<>()
                        .type("object")
                        .required(value.getFields().stream()
                                .filter(elt -> !elt.getType().accept(TypeVisitor.IS_OPTIONAL))
                                .map(elt -> elt.getFieldName().get())
                                .sorted()
                                .toList())
                        .properties(value.getFields().stream()
                                .map(elt -> Map.entry(
                                        elt.getFieldName().get(), elt.getType().accept(TYPE_VISITOR)))
                                .collect(Collectors.toMap(
                                        Entry::getKey,
                                        Entry::getValue,
                                        (_a, _b) -> {
                                            throw new IllegalStateException();
                                        },
                                        LinkedHashMap::new)))));
    }

    private static Stream<Entry<String, Schema<?>>> convertUnion(UnionDefinition value) {
        return Stream.concat(
                value.getUnion().stream()
                        .sorted(Comparator.comparing(FieldDefinition::getFieldName))
                        .map(elt -> Map.entry(
                                elt.getFieldName().get() + "Wrapper",
                                new Schema<>()
                                        .type("object")
                                        .properties(Stream.of(
                                                        Map.entry("type", new Schema<>().type("string")),
                                                        Map.entry(
                                                                elt.getFieldName()
                                                                        .get(),
                                                                elt.getType().accept(TYPE_VISITOR)))
                                                .collect(Collectors.toMap(
                                                        Entry::getKey,
                                                        Entry::getValue,
                                                        (_a, _b) -> {
                                                            throw new IllegalStateException();
                                                        },
                                                        LinkedHashMap::new)))
                                        .required(List.of(
                                                "type", elt.getFieldName().get())))),
                Stream.of(Map.entry(
                        value.getTypeName().getName(),
                        new Schema<>()
                                .oneOf(value.getUnion().stream()
                                        .sorted(Comparator.comparing(FieldDefinition::getFieldName))
                                        .map(elt -> new Schema<>()
                                                .$ref("#/components/schemas/"
                                                        + elt.getFieldName().get()
                                                        + "Wrapper"))
                                        .toList())
                                .discriminator(new Discriminator().propertyName("type")))));
    }

    static OpenAPI generate(ConjureDefinition conjureDefinition) {
        return new OpenAPI()
                .components(new Components()
                        .schemas(conjureDefinition.getTypes().stream()
                                .flatMap(typeDefinition -> {
                                    return typeDefinition.accept(new Visitor<Stream<Entry<String, Schema<?>>>>() {

                                        @Override
                                        public Stream<Entry<String, Schema<?>>> visitAlias(AliasDefinition value) {
                                            return Stream.of(Map.entry(
                                                    value.getTypeName().getName(),
                                                    value.getAlias().accept(TYPE_VISITOR)));
                                        }

                                        @Override
                                        public Stream<Entry<String, Schema<?>>> visitEnum(EnumDefinition value) {
                                            return Stream.of(Map.entry(
                                                    value.getTypeName().getName(),
                                                    new Schema<String>()
                                                            .type("string")
                                                            ._enum(value.getValues().stream()
                                                                    .map(EnumValueDefinition::getValue)
                                                                    .toList())));
                                        }

                                        @Override
                                        public Stream<Entry<String, Schema<?>>> visitObject(ObjectDefinition value) {
                                            return convertObject(value);
                                        }

                                        @Override
                                        public Stream<Entry<String, Schema<?>>> visitUnion(UnionDefinition value) {
                                            return convertUnion(value);
                                        }

                                        @Override
                                        public Stream<Entry<String, Schema<?>>> visitUnknown(
                                                @Safe String _unknownType) {
                                            throw new IllegalStateException();
                                        }
                                    });
                                })
                                .collect(Collectors.toMap(
                                        Entry::getKey,
                                        Entry::getValue,
                                        (_a, _b) -> {
                                            throw new IllegalStateException();
                                        },
                                        LinkedHashMap::new))));
    }

    private OpenApiGenerator() {}
}
