package org.example;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class NetworkScheduler {
    public static void main(String[] args){
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        scheduler.scheduleAtFixedRate(()->{
            try{
                InterfaceProgram.main(new String[]{});
            }
            catch(Exception e){
                e.printStackTrace();
            }
        },0,5,TimeUnit.MINUTES);

        Runtime.getRuntime().addShutdownHook(new Thread(()->{
            scheduler.shutdown();
        }));
    }
}
