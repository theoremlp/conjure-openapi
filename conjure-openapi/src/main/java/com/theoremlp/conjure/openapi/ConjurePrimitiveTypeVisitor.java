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

import com.palantir.conjure.spec.PrimitiveType.Visitor;
import io.swagger.v3.oas.models.media.Schema;

public final class ConjurePrimitiveTypeVisitor implements Visitor<Schema<?>> {

    @Override
    public Schema<?> visitString() {
        return new Schema<>().type("string");
    }

    @Override
    public Schema<?> visitDatetime() {
        return new Schema<>().type("string").format("datetime");
    }

    @Override
    public Schema<?> visitInteger() {
        return new Schema<>().type("integer").format("int32");
    }

    @Override
    public Schema<?> visitDouble() {
        return new Schema<>().type("number").format("float");
    }

    @Override
    public Schema<?> visitSafelong() {
        return new Schema<>().type("integer").format("int64");
    }

    @Override
    public Schema<?> visitBinary() {
        throw new IllegalStateException();
    }

    @Override
    public Schema<?> visitAny() {
        throw new IllegalStateException();
    }

    @Override
    public Schema<?> visitBoolean() {
        return new Schema<>().type("boolean");
    }

    @Override
    public Schema<?> visitUuid() {
        return new Schema<>().type("string").format("uuid");
    }

    @Override
    public Schema<?> visitRid() {
        return new Schema<>().type("string");
    }

    @Override
    public Schema<?> visitBearertoken() {
        throw new IllegalStateException();
    }

    @Override
    public Schema<?> visitUnknown(String _unknownValue) {
        throw new IllegalStateException();
    }
}
