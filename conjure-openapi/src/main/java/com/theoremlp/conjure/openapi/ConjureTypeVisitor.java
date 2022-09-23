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

import com.palantir.conjure.spec.ExternalReference;
import com.palantir.conjure.spec.ListType;
import com.palantir.conjure.spec.MapType;
import com.palantir.conjure.spec.OptionalType;
import com.palantir.conjure.spec.PrimitiveType;
import com.palantir.conjure.spec.SetType;
import com.palantir.conjure.spec.Type;
import com.palantir.conjure.spec.TypeName;
import com.palantir.logsafe.Safe;
import io.swagger.v3.oas.models.media.Schema;

public final class ConjureTypeVisitor implements Type.Visitor<Schema<?>> {

    private static final PrimitiveType.Visitor<Schema<?>> PRIMITIVE_VISITOR = new ConjurePrimitiveTypeVisitor();

    @Override
    public Schema<?> visitPrimitive(PrimitiveType value) {
        return value.accept(PRIMITIVE_VISITOR);
    }

    @Override
    public Schema<?> visitOptional(OptionalType value) {
        return value.getItemType().accept(this);
    }

    @Override
    public Schema<?> visitList(ListType value) {
        return new Schema<>().type("array").items(value.getItemType().accept(this));
    }

    @Override
    public Schema<?> visitSet(SetType value) {
        return new Schema<>().type("array").items(value.getItemType().accept(this));
    }

    @Override
    public Schema<?> visitMap(MapType value) {
        // TODO(matthew): enforce allowing only compatible key types and throw otherwise
        return new Schema<>()
                .type("object")
                .additionalProperties(value.getValueType().accept(this));
    }

    @Override
    public Schema<?> visitReference(TypeName value) {
        return new Schema<>().$ref("#/components/schemas/" + value.getName());
    }

    @Override
    public Schema<?> visitExternal(ExternalReference _value) {
        throw new IllegalStateException();
    }

    @Override
    public Schema<?> visitUnknown(@Safe String _unknownType) {
        throw new IllegalStateException();
    }
}
