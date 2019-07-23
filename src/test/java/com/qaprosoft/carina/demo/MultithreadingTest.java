package com.qaprosoft.carina.demo;

import com.qaprosoft.carina.core.foundation.utils.Configuration;
import com.qaprosoft.carina.core.foundation.utils.ownership.MethodOwner;
import org.apache.log4j.Logger;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.IntStream;

import static javax.crypto.Cipher.ENCRYPT_MODE;

public class MultithreadingTest {

    private static final Logger LOGGER = Logger.getLogger(MultithreadingTest.class);
    private static final List<CompletableFuture<Void>> THREADS_FUTURES = new ArrayList<>();
    private Integer threadsCount;
    private Integer threadTtl;

    @BeforeMethod
    public void setup() {
        threadsCount = Integer.valueOf(Configuration.getEnvArg("threads_count"));
        threadTtl = Integer.valueOf(Configuration.getEnvArg("thread_ttl"));
        LOGGER.info("Performance test was started with " + threadsCount + " threads");
        LOGGER.info("Each thread ttl is " + threadTtl + " milliseconds");
    }

    @Test
    @MethodOwner(owner = "qpsdemo")
    public void test() {
        IntStream.range(0, threadsCount).forEach(i -> {
            LOGGER.info("Thread with num " + i + " is starting");
            createThread(() -> {
                doWork();
                LOGGER.info("Thread with num " + i + " is finishing");
            });
        });
        waitAllFutures();
    }

    private void createThread(Runnable runnable) {
        CompletableFuture<Void> completableFuture = CompletableFuture.runAsync(runnable);
        THREADS_FUTURES.add(completableFuture);
    }

    private void doWork() {
        long threadStartedAt = System.currentTimeMillis();
        long currentCPUUtilizationMillis = 0;
        while(currentCPUUtilizationMillis <= threadTtl) {
            cryptoLoad();
            currentCPUUtilizationMillis = System.currentTimeMillis() - threadStartedAt;
        }
    }

    private void cryptoLoad() {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            SecretKeySpec AESkeySpec = new SecretKeySpec(generateCryptoKey(), "AES");
            cipher.init(ENCRYPT_MODE,AESkeySpec);
            cipher.update(generateCryptoKey());
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    private byte[] generateCryptoKey() {
        byte[] key = null;
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256);
            SecretKey secretKey = keyGen.generateKey();
            key = secretKey.getEncoded();
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return key;
    }

    private void waitAllFutures() {
        CompletableFuture<Void> allCompletableFuture = CompletableFuture.allOf(THREADS_FUTURES.toArray(new CompletableFuture[0]));
        waitCompletableFeature(allCompletableFuture);
    }

    private void waitCompletableFeature(CompletableFuture completableFuture) {
        try {
            long maxWaitingValue = threadTtl * new Double(Math.floor(threadsCount / 5)).longValue();
            LOGGER.info("Max waiting will be " + maxWaitingValue);
            completableFuture.get(maxWaitingValue, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

}
