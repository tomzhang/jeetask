/*
 * Copyright 1999-2015 dangdang.com.
 * <p>
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
 * </p>
 */

package com.jeeframework.jeetask.event.rdb;

import com.jeeframework.jeetask.event.JobEventListener;
import com.jeeframework.jeetask.event.JobEventListenerConfigurationException;
import com.jeeframework.jeetask.event.JobEventProcessor;
import com.jeeframework.jeetask.event.rdb.impl.JobEventCommonStorageProcessor;
import com.jeeframework.jeetask.event.type.JobExecutionEvent;
import com.jeeframework.util.classes.ClassUtils;
import com.jeeframework.util.validate.Validate;
import lombok.RequiredArgsConstructor;

import javax.sql.DataSource;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;

/**
 * 运行痕迹事件数据库监听器.
 *
 * @author caohao
 */
@RequiredArgsConstructor
public final class JobEventRdbListener implements JobEventListener {

    private final JobEventProcessor processor;

    public JobEventRdbListener(final DataSource dataSource, final String jobEventStorageClass) throws SQLException,
            JobEventListenerConfigurationException {
        if (!Validate.isEmpty(jobEventStorageClass)) {
            try {
                Class jobEventStorageClazz = ClassUtils.forName(jobEventStorageClass);
                Constructor constructor = ClassUtils.getConstructorIfAvailable(jobEventStorageClazz, new
                        Class[]{DataSource.class});
                processor = (JobEventProcessor) constructor.newInstance(dataSource);
            } catch (ClassNotFoundException e) {
                throw new JobEventListenerConfigurationException(e);
            } catch (IllegalAccessException e) {
                throw new JobEventListenerConfigurationException(e);
            } catch (InstantiationException e) {
                throw new JobEventListenerConfigurationException(e);
            } catch (InvocationTargetException e) {
                throw new JobEventListenerConfigurationException(e);
            }

        } else {
            processor = new JobEventCommonStorageProcessor(dataSource);
        }
    }

    @Override
    public void trigger(final JobExecutionEvent executionEvent) {
        processor.addJobExecutionEvent(executionEvent);
    }


//    @Override
//    public void listen(final JobStatusTraceEvent jobStatusTraceEvent) {
//        repository.addJobStatusTraceEvent(jobStatusTraceEvent);
//    }
}
