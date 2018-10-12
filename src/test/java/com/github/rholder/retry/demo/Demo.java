package com.github.rholder.retry.demo;

import com.github.rholder.retry.*;
import org.junit.Test;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class Demo {
    Logger LOGGER = Logger.getLogger("demo1");

    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.S");

    private <T> T run(Retryer<T> retryer, Callable<T> callable) {
        try {
            return retryer.call(callable);
        } catch (RetryException | ExecutionException e) {
//            LOGGER.trace(ExceptionUtils.getFullStackTrace(e));        LOGGER.warn(e.getMessage());
            e.printStackTrace();
        }
        return null;

    }

    /**
     * 根据结果判断是否重试
     */
    /**
     * 场景：如果counter值小于5则抛出异常，等于5则正常返回停止重试；
     *
     * @return
     */
    private Callable<String> callableWithResult() {
        return new Callable<String>() {
            int counter = 0;

            @Override
            public String call() throws Exception {
                counter++;

                LOGGER.info("=== "+dateFormat.format(new Date())+"do sth : " + counter);
                if (counter < 5) {
                    return "sorry";
                }
                return "good";
            }
        };
    }


    @Test
    public void test1() {
        Retryer<String> retryer = RetryerBuilder.<String>newBuilder()
                .retryIfResult(result -> !result.contains("good"))
                .withStopStrategy(StopStrategies.neverStop())
                .build();
        run(retryer, callableWithResult());
    }


    /**
     * 根据异常判断是否重试
     * 场景：如果counter值小于5则抛出异常，等于5则正常返回停止重试；
     */
    private Callable<String> callableWithException() {
        return new Callable<String>() {
            int counter = 0;

            public String call() throws Exception {
                counter++;
                LOGGER.info("=== "+dateFormat.format(new Date())+"do sth : " + counter);
                if (counter < 5) {
                    throw new RuntimeException("sorry");
                }
                return "good";
            }
        };
    }

    @Test
    public void retryWithException() {
        Retryer<String> retryer = RetryerBuilder.<String>newBuilder()
                .retryIfRuntimeException()
                .withStopStrategy(StopStrategies.neverStop()).build();
        LOGGER.info("result : " + run(retryer, callableWithException()));
    }


    /**
     * 重试策略——设定无限重试
     * 场景：在有异常情况下，无限重试，直到返回正常有效结果
     */

    @Test
    public void retryNeverStop() {
        Retryer<String> retryer = RetryerBuilder.<String>newBuilder()
                .retryIfRuntimeException()
                .withStopStrategy(StopStrategies.neverStop()).build();
        LOGGER.info("result : " + run(retryer, callableWithException()));
    }

    /**
     * 重试策略——设定最大的重试次数
     * 场景：在有异常情况下，最多重试3次，如果超过3次则会抛出异常；
     */

    @Test
    public void retryStopAfterAttempt() {
        Retryer<String> retryer = RetryerBuilder.<String>newBuilder()
                .retryIfRuntimeException()
                .withStopStrategy(StopStrategies.stopAfterAttempt(3))
                .withWaitStrategy(WaitStrategies.fixedWait(1000, TimeUnit.MILLISECONDS)).build();
        LOGGER.info("result : " + run(retryer, callableWithException()));
    }


    /**
     * 等待策略——设定重试等待固定时长策略
     * 景：设定每次重试等待间隔固定
     */

    @Test
    public void retryWaitFixStrategy() {
        Retryer<String> retryer = RetryerBuilder.<String>newBuilder()
                .retryIfRuntimeException()
                .withStopStrategy(StopStrategies.neverStop())
                .withWaitStrategy(WaitStrategies.fixedWait(100, TimeUnit.MILLISECONDS)).build();
        LOGGER.info("result : " + run(retryer, callableWithException()));
    }

    /**
     * 等待策略——设定重试等待时长固定增长策略
     * 场景：设定初始等待时长值，并设定固定增长步长，但不设定最大等待时长；
     * 等待时长 1 ， 2 ，4 ，8 ， 16
     */
//    @Test
    public void retryWaitIncreaseStrategy() {
        Retryer<String> retryer = RetryerBuilder.<String>newBuilder()
                .retryIfRuntimeException()
                .withStopStrategy(StopStrategies.neverStop())
                .withWaitStrategy(WaitStrategies.incrementingWait(1000, TimeUnit.MILLISECONDS, 100, TimeUnit.MILLISECONDS)).build();
        LOGGER.info("result : " + run(retryer, callableWithException()));
    }

    /**
     * 待策略——设定重试等待时长按指数增长策略
     * <p>
     * 场景：根据multiplier值按照指数级增长等待时长，并设定最大等待时长；
     */
    @Test
    public void retryWaitExponentialStrategy() {
        Retryer<String> retryer = RetryerBuilder.<String>newBuilder()
                .retryIfRuntimeException()
                .withStopStrategy(StopStrategies.neverStop())
                .withWaitStrategy(WaitStrategies.exponentialWait(100, 1000, TimeUnit.MILLISECONDS)).build();
        LOGGER.info("result : " + run(retryer, callableWithException()));
    }

    /**
     * 等待策略——设定重试等待时长按斐波那契数列增长策略
     * 场景：根据multiplier值按照斐波那契数列增长等待时长，并设定最大等待时长，斐波那契数列：1、1、2、3、5、8、13、21、34、……
     */

    @Test
    public void retryWaitFibonacciStrategy() {
        Retryer<String> retryer = RetryerBuilder.<String>newBuilder()
                .retryIfRuntimeException()
                .withStopStrategy(StopStrategies.neverStop())
                .withWaitStrategy(WaitStrategies.fibonacciWait(100, 1000, TimeUnit.MILLISECONDS)).build();
        LOGGER.info("result : " + run(retryer, callableWithException()));
    }

    /**
     * 等待策略——组合重试等待时长策略
     * 场景：组合ExponentialWaitStrategy和FixedWaitStrategy策略。
     * 每次重试等待的时间=固定等待时长+指数等待时长的每次增长后的时长
     */

    @Test
    public void retryWaitJoinStrategy() {
        Retryer<String> retryer = RetryerBuilder.<String>newBuilder()
                .retryIfRuntimeException()
                .withStopStrategy(StopStrategies.neverStop())
                .withWaitStrategy(WaitStrategies.join(
                        WaitStrategies.exponentialWait(25, 500, TimeUnit.MILLISECONDS)
                        , WaitStrategies.fixedWait(50, TimeUnit.MILLISECONDS)))
                .build();
        LOGGER.info("result : " + run(retryer, callableWithException()));
    }

    /**
     * 监听器——RetryListener实现重试过程细节处理
     * 场景：定义两个监听器，分别打印重试过程中的细节，未来可更多的用于异步日志记录，亦或是特殊处理。
     * RetryListener会根据注册顺序执行。
     */

    private RetryListener myRetryListener() {
        return new RetryListener() {
            @Override
            public <T> void onRetry(Attempt<T> attempt) {
                // 第几次重试,(注意:第一次重试其实是第一次调用)
                LOGGER.info("[retry]time=" + attempt.getAttemptNumber());
                // 距离第一次重试的延迟
                LOGGER.info(",delay=" + attempt.getDelaySinceFirstAttempt());
                // 重试结果: 是异常终止, 还是正常返回
                LOGGER.info(",hasException=" + attempt.hasException());
                LOGGER.info(",hasResult=" + attempt.hasResult());
                // 是什么原因导致异常
                if (attempt.hasException()) {
                    LOGGER.info(",causeBy=" + attempt.getExceptionCause().toString());
                } else {
                    // 正常返回时的结果
                    LOGGER.info(",result=" + attempt.getResult());
                }
                // 增加了额外的异常处理代码
                try {
                    T result = attempt.get();
                    LOGGER.info(",rude get=" + result);
                } catch (ExecutionException e) {
                    LOGGER.info("this attempt produce exception." + e.getCause().toString());
                }
            }
        };
    }

    private RetryListener myRetryListener2() {
        return new RetryListener() {
            @Override
            public <T> void onRetry(Attempt<T> attempt) {
                LOGGER.info("myRetryListener2 : [retry]time=" + attempt.getAttemptNumber());
            }
        };
    }

    private <T> T runWithFixRetryAndListener(Callable<T> callable) {
        Retryer<T> retryer = RetryerBuilder.<T>newBuilder()
                .retryIfRuntimeException()
                .withStopStrategy(StopStrategies.neverStop())
                .withRetryListener(myRetryListener())
                .withRetryListener(myRetryListener2()).build();
        return run(retryer, callable);
    }

    @Test
    public void retryWithRetryListener() {
        LOGGER.info("result : " + runWithFixRetryAndListener(callableWithException()));
    }


}
