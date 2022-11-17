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

import com.palantir.conjure.spec.ArgumentDefinition;
import com.palantir.conjure.spec.AuthType;
import com.palantir.conjure.spec.AuthType.Visitor;
import com.palantir.conjure.spec.BodyParameterType;
import com.palantir.conjure.spec.CookieAuthType;
import com.palantir.conjure.spec.Documentation;
import com.palantir.conjure.spec.EndpointDefinition;
import com.palantir.conjure.spec.HeaderAuthType;
import com.palantir.conjure.spec.HeaderParameterType;
import com.palantir.conjure.spec.HttpPath;
import com.palantir.conjure.spec.ParameterType;
import com.palantir.conjure.spec.PathParameterType;
import com.palantir.conjure.spec.QueryParameterType;
import com.palantir.conjure.spec.ServiceDefinition;
import com.palantir.conjure.spec.Type;
import com.palantir.conjure.spec.TypeName;
import com.palantir.conjure.visitor.ParameterTypeVisitor;
import com.palantir.conjure.visitor.TypeVisitor;
import com.palantir.logsafe.Safe;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

final class ServiceGenerator {

    public static final String BEARER_AUTH = "BearerAuth";

    static Paths generatePaths(List<ServiceDefinition> services) {
        Paths paths = new Paths();
        services.stream().map(ServiceGenerator::generatePaths).forEach(paths::putAll);
        return paths;
    }

    private static Map<String, PathItem> generatePaths(ServiceDefinition serviceDefinition) {
        Map<HttpPath, List<EndpointDefinition>> endpointsByPath = serviceDefinition.getEndpoints().stream()
                .collect(Collectors.groupingBy(EndpointDefinition::getHttpPath));
        return endpointsByPath.entrySet().stream()
                .collect(Collectors.toMap(entry -> entry.getKey().get(), entry -> {
                    PathItem pathItem = new PathItem();
                    entry.getValue().forEach(endpointDefinition -> {
                        Operation operation = generateOperation(serviceDefinition.getServiceName(), endpointDefinition);
                        switch (endpointDefinition.getHttpMethod().get()) {
                            case GET -> pathItem.get(operation);
                            case POST -> pathItem.post(operation);
                            case PUT -> pathItem.put(operation);
                            case DELETE -> pathItem.delete(operation);
                            case UNKNOWN -> throw new IllegalStateException("Unexpected http method");
                        }
                    });
                    return pathItem;
                }));
    }

    private static Operation generateOperation(TypeName serviceName, EndpointDefinition endpointDefinition) {
        Operation operation = new Operation();
        operation
                .operationId(serviceName.getName() + "#" + endpointDefinition.getEndpointName())
                .deprecated(endpointDefinition.getDeprecated().isPresent());
        endpointDefinition.getDocs().ifPresent(docs -> operation.setDescription(docs.get()));
        endpointDefinition
                .getAuth()
                .map(ServiceGenerator::toSecurityRequirement)
                .ifPresent(operation::addSecurityItem);
        endpointDefinition.getArgs().stream()
                .filter(arg -> !arg.getParamType().accept(ParameterTypeVisitor.IS_BODY))
                .map(ServiceGenerator::toParameter)
                .forEach(operation::addParametersItem);
        endpointDefinition.getArgs().stream()
                .filter(arg -> arg.getParamType().accept(ParameterTypeVisitor.IS_BODY))
                .findFirst()
                .ifPresent(arg -> operation.setRequestBody(toRequestBody(arg)));
        endpointDefinition.getReturns().map(ServiceGenerator::toResponses).ifPresent(operation::setResponses);

        return operation;
    }

    private static RequestBody toRequestBody(ArgumentDefinition arg) {
        RequestBody requestBody = new RequestBody()
                .required(true)
                .content(new Content()
                        .addMediaType(
                                "application/json",
                                new MediaType().schema(arg.getType().accept(ConjureTypeVisitor.INSTANCE))));
        arg.getDocs().map(Documentation::get).ifPresent(requestBody::setDescription);

        return requestBody;
    }

    private static Parameter toParameter(ArgumentDefinition argumentDefinition) {
        Parameter parameter =
                new Parameter().name(argumentDefinition.getArgName().get());
        argumentDefinition.getDocs().map(Documentation::get).ifPresent(parameter::setDescription);

        String location = argumentDefinition.getParamType().accept(new ParameterType.Visitor<String>() {
            @Override
            public String visitBody(BodyParameterType _value) {
                throw new IllegalStateException("Should never happen");
            }

            @Override
            public String visitHeader(HeaderParameterType _value) {
                return "header";
            }

            @Override
            public String visitPath(PathParameterType _value) {
                return "path";
            }

            @Override
            public String visitQuery(QueryParameterType _value) {
                return "query";
            }

            @Override
            public String visitUnknown(@Safe String unknownType) {
                throw new IllegalStateException("Unexpected type " + unknownType);
            }
        });
        parameter.in(location);
        parameter.setRequired(!argumentDefinition.getType().accept(TypeVisitor.IS_OPTIONAL));
        parameter.setSchema(argumentDefinition.getType().accept(ConjureTypeVisitor.INSTANCE));

        return parameter;
    }

    private static ApiResponses toResponses(Type type) {
        return new ApiResponses()
                .addApiResponse(
                        "200",
                        new ApiResponse()
                                .content(new Content()
                                        .addMediaType(
                                                "application/json",
                                                new MediaType().schema(type.accept(ConjureTypeVisitor.INSTANCE)))));
    }

    private static SecurityRequirement toSecurityRequirement(AuthType auth) {
        return auth.accept(new Visitor<>() {
            @Override
            public SecurityRequirement visitHeader(HeaderAuthType _value) {
                return new SecurityRequirement().addList(BEARER_AUTH);
            }

            @Override
            public SecurityRequirement visitCookie(CookieAuthType _value) {
                throw new IllegalStateException("Cookie auth is not supported");
            }

            @Override
            public SecurityRequirement visitUnknown(String unknownType) {
                throw new IllegalStateException("Unexpected auth type " + unknownType);
            }
        });
    }

    private ServiceGenerator() {}
}
