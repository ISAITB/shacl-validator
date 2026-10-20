/*
 * Copyright (C) 2026 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

package eu.europa.ec.itb.shacl.config;

import eu.europa.ec.itb.validation.commons.error.ValidatorException;
import org.apache.jena.sparql.algebra.op.OpService;
import org.apache.jena.sparql.engine.ExecutionContext;
import org.apache.jena.sparql.engine.QueryIterator;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.service.ServiceExecutorRegistry;
import org.apache.jena.sparql.service.single.ChainingServiceExecutor;
import org.apache.jena.sparql.service.single.ServiceExecutor;
import org.apache.jena.sys.JenaSystem;

/**
 * Executor for SPARQL SERVICE calls that rejects them while blocking is enabled for the current thread.
 * <p>
 * SHACL shapes may include SPARQL constraints that call out to arbitrary endpoints by means of SERVICE clauses. This
 * must not be allowed for shapes that are provided by users, as it would let them trigger requests to arbitrary locations.
 */
public class ServiceBlockingExecutor implements ChainingServiceExecutor {

    private static final InheritableThreadLocal<Boolean> BLOCKED = new InheritableThreadLocal<>();

    /**
     * Register the executor to be consulted before any other executor. Calling this multiple times is safe.
     */
    public static void install() {
        // Jena must be initialised before any of its classes (e.g. ARQ constants) are used.
        JenaSystem.init();
        var registry = ServiceExecutorRegistry.get();
        synchronized (ServiceBlockingExecutor.class) {
            if (registry.getSingleChain().stream().noneMatch(ServiceBlockingExecutor.class::isInstance)) {
                registry.addSingleLink(new ServiceBlockingExecutor());
            }
        }
    }

    /**
     * Reject SPARQL SERVICE calls made by the current thread (and threads it creates) until {@link #unblock()}.
     */
    public static void block() {
        BLOCKED.set(Boolean.TRUE);
    }

    /**
     * Stop rejecting SPARQL SERVICE calls made by the current thread.
     */
    public static void unblock() {
        BLOCKED.remove();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public QueryIterator createExecution(OpService opExecute, OpService opOriginal, Binding binding, ExecutionContext execCxt, ServiceExecutor chain) {
        if (Boolean.TRUE.equals(BLOCKED.get())) {
            throw new ValidatorException("validator.label.exception.sparqlServiceNotAllowed", String.valueOf(opOriginal.getService()));
        }
        return chain.createExecution(opExecute, opOriginal, binding, execCxt);
    }

}
