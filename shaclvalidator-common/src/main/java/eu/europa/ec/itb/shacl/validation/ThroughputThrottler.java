/*
 * Copyright (C) 2025 European Union
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

package eu.europa.ec.itb.shacl.validation;

import eu.europa.ec.itb.shacl.ApplicationConfig;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.Semaphore;
import java.util.function.Supplier;

/**
 * Singleton component used to apply a throughput rate on concurrently executing validations.
 * <p/>
 * The implementation follows a simple locking approach given that validations cannot be executed asynchronously.
 */
@Component
public class ThroughputThrottler {

    private static final Logger LOG = LoggerFactory.getLogger(ThroughputThrottler.class);

    @Autowired
    private ApplicationConfig appConfig;
    private Semaphore semaphore;

    @PostConstruct
    public void initialise() {
        Integer configuredMaximum = appConfig.getMaximumConcurrentValidations();
        if (configuredMaximum != null && configuredMaximum > 0) {
            // Create a FIFO semaphore with the configured capacity.
            semaphore = new Semaphore(configuredMaximum, true);
            LOG.info("Validation throttling allows {} validations to proceed in parallel", configuredMaximum);
        } else {
            semaphore = null;
            LOG.info("No validation throttling configured");
        }
    }

    /**
     * Proceed with the supplied task while respecting configured throttling.
     *
     * @param task The validation task to execute.
     * @return The result.
     * @param <T> The type of result.
     */
    public <T> T proceed(Supplier<T> task) {
        if (semaphore == null) {
            // No throttling.
            return task.get();
        } else {
            try {
                semaphore.acquire();
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Thread [{}] acquired semaphore permit ({} permits left - current queue {})", Thread.currentThread().getName(), semaphore.availablePermits(), semaphore.getQueueLength());
                }
                return task.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted thread while waiting for throughput throttling", e);
            } finally {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Thread [{}] releasing semaphore lock", Thread.currentThread().getName());
                }
                semaphore.release();
            }
        }
    }

}
