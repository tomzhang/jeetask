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

package com.jeeframework.jeetask.executor;


import com.dangdang.ddframe.job.exception.JobSystemException;
import com.jeeframework.jeetask.config.simple.JobConfiguration;
import com.jeeframework.jeetask.executor.handler.ExecutorServiceHandler;
import com.jeeframework.jeetask.executor.handler.ExecutorServiceHandlerRegistry;
import com.jeeframework.jeetask.executor.handler.JobExceptionHandler;
import com.jeeframework.jeetask.executor.handler.JobProperties;
import com.jeeframework.jeetask.task.context.ShardingContext;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * 弹性化分布式作业执行器.
 *
 * @author zhangliang
 */
@Slf4j
public abstract class AbstractElasticJobExecutor {

    @Getter(AccessLevel.PROTECTED)
    private final JobFacade jobFacade;

    @Getter(AccessLevel.PROTECTED)
    private final JobConfiguration jobRootConfig;

    private final String jobName;

    private final ExecutorService executorService;

    private final JobExceptionHandler jobExceptionHandler;

    private final Map<Integer, String> itemErrorMessages;

    protected AbstractElasticJobExecutor(final JobFacade jobFacade) {
        this.jobFacade = jobFacade;
        jobRootConfig = jobFacade.loadJobConfiguration(true);
        jobName = jobRootConfig.getJobDefinition().getJobName();
        executorService = ExecutorServiceHandlerRegistry.getExecutorServiceHandler(jobName, (ExecutorServiceHandler)
                getHandler(JobProperties.JobPropertiesEnum.EXECUTOR_SERVICE_HANDLER));
        jobExceptionHandler = (JobExceptionHandler) getHandler(JobProperties.JobPropertiesEnum.JOB_EXCEPTION_HANDLER);
//        itemErrorMessages = new ConcurrentHashMap<>(jobRootConfig.getTypeConfig().getCoreConfig()
// .getShardingTotalCount(), 1);
        itemErrorMessages = new ConcurrentHashMap<>(1, 1);
    }

    private Object getHandler(final JobProperties.JobPropertiesEnum jobPropertiesEnum) {
        String handlerClassName = jobRootConfig.getJobProperties().get(jobPropertiesEnum);
        try {
            Class<?> handlerClass = Class.forName(handlerClassName);
            if (jobPropertiesEnum.getClassType().isAssignableFrom(handlerClass)) {
                return handlerClass.newInstance();
            }
            return getDefaultHandler(jobPropertiesEnum, handlerClassName);
        } catch (final ReflectiveOperationException ex) {
            return getDefaultHandler(jobPropertiesEnum, handlerClassName);
        }
    }

    private Object getDefaultHandler(final JobProperties.JobPropertiesEnum jobPropertiesEnum, final String
            handlerClassName) {
        log.warn("Cannot instantiation class '{}', use default '{}' class.", handlerClassName, jobPropertiesEnum
                .getKey());
        try {
            return Class.forName(jobPropertiesEnum.getDefaultValue()).newInstance();
        } catch (final ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            throw new JobSystemException(e);
        }
    }

    /**
     * 执行作业.
     */
//    public final void execute() {
//        try {
//            jobFacade.checkJobExecutionEnvironment();
//        } catch (final JobExecutionEnvironmentException cause) {
//            jobExceptionHandler.handleException(jobName, cause);
//        }
//        ShardingContexts shardingContexts = jobFacade.getShardingContexts();
//        if (shardingContexts.isAllowSendJobEvent()) {
//            jobFacade.postJobStatusTraceEvent(shardingContexts.getTaskId(), JobStatusTraceEvent.State.TASK_STAGING,
//                    String.format("Job '%s' execute begin.", jobName));
//        }
//        if (jobFacade.misfireIfRunning(shardingContexts.getShardingItemParameters().keySet())) {
//            if (shardingContexts.isAllowSendJobEvent()) {
//                jobFacade.postJobStatusTraceEvent(shardingContexts.getTaskId(), JobStatusTraceEvent.State
//                        .TASK_FINISHED, String.format(
//                        "Previous job '%s' - shardingItems '%s' is still running, misfired job will start after " +
//                                "previous job completed.", jobName,
//                        shardingContexts.getShardingItemParameters().keySet()));
//            }
//            return;
//        }
//        jobFacade.cleanPreviousExecutionInfo();
//        try {
//            jobFacade.beforeJobExecuted(shardingContexts);
//            //CHECKSTYLE:OFF
//        } catch (final Throwable cause) {
//            //CHECKSTYLE:ON
//            jobExceptionHandler.handleException(jobName, cause);
//        }
//        execute(shardingContexts, JobExecutionEvent.ExecutionSource.NORMAL_TRIGGER);
//        while (jobFacade.isExecuteMisfired(shardingContexts.getShardingItemParameters().keySet())) {
//            jobFacade.clearMisfire(shardingContexts.getShardingItemParameters().keySet());
//            execute(shardingContexts, JobExecutionEvent.ExecutionSource.MISFIRE);
//        }
//        jobFacade.failoverIfNecessary();
//        try {
//            jobFacade.afterJobExecuted(shardingContexts);
//            //CHECKSTYLE:OFF
//        } catch (final Throwable cause) {
//            //CHECKSTYLE:ON
//            jobExceptionHandler.handleException(jobName, cause);
//        }
//    }

//    private void execute(final ShardingContexts shardingContexts, final JobExecutionEvent.ExecutionSource
//            executionSource) {
//        if (shardingContexts.getShardingItemParameters().isEmpty()) {
//            if (shardingContexts.isAllowSendJobEvent()) {
//                jobFacade.postJobStatusTraceEvent(shardingContexts.getTaskId(), JobStatusTraceEvent.State
//                        .TASK_FINISHED, String.format("Sharding item for job '%s' is empty.", jobName));
//            }
//            return;
//        }
//        jobFacade.registerJobBegin(shardingContexts);
//        String taskId = shardingContexts.getTaskId();
//        if (shardingContexts.isAllowSendJobEvent()) {
//            jobFacade.postJobStatusTraceEvent(taskId, JobStatusTraceEvent.State.TASK_RUNNING, "");
//        }
//        try {
//            process(shardingContexts, executionSource);
//        } finally {
//            // TODO 考虑增加作业失败的状态，并且考虑如何处理作业失败的整体回路
//            jobFacade.registerJobCompleted(shardingContexts);
//            if (itemErrorMessages.isEmpty()) {
//                if (shardingContexts.isAllowSendJobEvent()) {
//                    jobFacade.postJobStatusTraceEvent(taskId, JobStatusTraceEvent.State.TASK_FINISHED, "");
//                }
//            } else {
//                if (shardingContexts.isAllowSendJobEvent()) {
//                    jobFacade.postJobStatusTraceEvent(taskId, JobStatusTraceEvent.State.TASK_ERROR, itemErrorMessages
//                            .toString());
//                }
//            }
//        }
//    }

//    private void process(final ShardingContexts shardingContexts, final JobExecutionEvent.ExecutionSource
//            executionSource) {

//        JobExecutionEvent jobExecutionEvent = new JobExecutionEvent(shardingContexts.getTaskId(), jobName,
//                executionSource);
//        process(shardingContexts, jobExecutionEvent);
//        return;

//    }
//
//    private void process(final ShardingContexts shardingContexts, final JobExecutionEvent startEvent) {
//        if (shardingContexts.isAllowSendJobEvent()) {
//            jobFacade.postJobExecutionEvent(startEvent);
//        }
//        log.trace("Job '{}' executing.", jobName);
//        JobExecutionEvent completeEvent;
//        try {
//            process(new ShardingContext(shardingContexts));
//            completeEvent = startEvent.executionSuccess();
//            log.trace("Job '{}' executed .", jobName);
//            if (shardingContexts.isAllowSendJobEvent()) {
//                jobFacade.postJobExecutionEvent(completeEvent);
//            }
//            // CHECKSTYLE:OFF
//        } catch (final Throwable cause) {
//            // CHECKSTYLE:ON
//            completeEvent = startEvent.executionFailure(cause);
//            jobFacade.postJobExecutionEvent(completeEvent);
////            itemErrorMessages.put(item, ExceptionUtil.transform(cause));
//            jobExceptionHandler.handleException(jobName, cause);
//        }
//    }

    protected abstract void process(ShardingContext shardingContext);
}