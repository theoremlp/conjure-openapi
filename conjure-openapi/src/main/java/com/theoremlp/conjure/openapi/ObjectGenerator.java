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

import com.google.common.base.CaseFormat;
import com.google.common.base.Converter;
import com.google.common.collect.ImmutableMap;
import com.palantir.conjure.spec.AliasDefinition;
import com.palantir.conjure.spec.EnumDefinition;
import com.palantir.conjure.spec.EnumValueDefinition;
import com.palantir.conjure.spec.FieldDefinition;
import com.palantir.conjure.spec.ObjectDefinition;
import com.palantir.conjure.spec.TypeDefinition;
import com.palantir.conjure.spec.UnionDefinition;
import com.palantir.conjure.visitor.TypeVisitor;
import com.palantir.logsafe.Safe;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.media.Discriminator;
import io.swagger.v3.oas.models.media.Schema;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

final class ObjectGenerator {
    private static final Converter<String, String> LOWER_TO_UPPER_CAMEL =
            CaseFormat.LOWER_CAMEL.converterTo(CaseFormat.UPPER_CAMEL);

    static Components generateComponents(List<TypeDefinition> typeDefinitions) {
        return new Components()
                .schemas(typeDefinitions.stream()
                        .flatMap(ObjectGenerator::convertTypeDefinition)
                        .collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue)));
    }

    private static Stream<Entry<String, Schema<?>>> convertTypeDefinition(TypeDefinition typeDefinition) {
        return typeDefinition.accept(new TypeDefinition.Visitor<>() {

            @Override
            public Stream<Entry<String, Schema<?>>> visitAlias(AliasDefinition value) {
                return Stream.of(Map.entry(
                        value.getTypeName().getName(), value.getAlias().accept(ConjureTypeVisitor.INSTANCE)));
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
                return Stream.of(convertObject(value));
            }

            @Override
            public Stream<Entry<String, Schema<?>>> visitUnion(UnionDefinition value) {
                return convertUnion(value);
            }

            @Override
            public Stream<Entry<String, Schema<?>>> visitUnknown(@Safe String unknownType) {
                throw new IllegalStateException("Unexpected type " + unknownType);
            }
        });
    }

    private static Entry<String, Schema<?>> convertObject(ObjectDefinition value) {
        return Map.entry(
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
                                        elt.getFieldName().get(), elt.getType().accept(ConjureTypeVisitor.INSTANCE)))
                                .collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue))));
    }

    private static Stream<Entry<String, Schema<?>>> convertUnion(UnionDefinition value) {
        return Stream.concat(
                value.getUnion().stream()
                        .sorted(Comparator.comparing(FieldDefinition::getFieldName))
                        .map(fieldDef -> {
                            Schema<?> typeField = new Schema<>()
                                    .type("string")
                                    ._enum(List.of(fieldDef.getFieldName().get()));
                            Map<String, Schema<?>> properties = ImmutableMap.<String, Schema<?>>builder()
                                    .put("type", typeField)
                                    .put(
                                            fieldDef.getFieldName().get(),
                                            fieldDef.getType().accept(ConjureTypeVisitor.INSTANCE))
                                    .buildOrThrow();
                            return Map.entry(
                                    toUnionWrapperName(value, fieldDef),
                                    new Schema<>()
                                            .type("object")
                                            .properties(properties)
                                            .required(List.copyOf(properties.keySet())));
                        }),
                Stream.of(Map.entry(
                        value.getTypeName().getName(),
                        new Schema<>()
                                .oneOf(value.getUnion().stream()
                                        .sorted(Comparator.comparing(FieldDefinition::getFieldName))
                                        .map(elt -> new Schema<>()
                                                .$ref("#/components/schemas/" + toUnionWrapperName(value, elt)))
                                        .toList())
                                .discriminator(new Discriminator().propertyName("type")))));
    }

    private static String toUnionWrapperName(UnionDefinition value, FieldDefinition fieldDef) {
        return value.getTypeName().getName()
                + LOWER_TO_UPPER_CAMEL.convert(fieldDef.getFieldName().get()) + "Wrapper";
    }

    private ObjectGenerator() {}
}
