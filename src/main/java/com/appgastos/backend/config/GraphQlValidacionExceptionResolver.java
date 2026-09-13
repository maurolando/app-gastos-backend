package com.appgastos.backend.config;

import com.appgastos.backend.services.ValidacionException;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Component;

/**
 * Sin esto Spring GraphQL responde cualquier excepción como INTERNAL_ERROR con
 * un texto genérico, y el frontend no puede explicarle al usuario qué corregir
 * (por ejemplo, que los porcentajes de su regla no suman 100%).
 *
 * Solo se expone el mensaje de {@link ValidacionException}: el de otras
 * excepciones puede traer detalles internos.
 */
@Component
public class GraphQlValidacionExceptionResolver extends DataFetcherExceptionResolverAdapter {

    @Override
    protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
        if (ex instanceof ValidacionException) {
            return GraphqlErrorBuilder.newError(env)
                    .errorType(ErrorType.BAD_REQUEST)
                    .message(ex.getMessage())
                    .build();
        }
        return null;
    }
}
