package com.blazemeter.jmeter.debugger.engine;

import org.apache.jmeter.samplers.Sampler;
import org.apache.jmeter.threads.*;
import org.apache.jorphan.collections.HashTree;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

import java.lang.reflect.Field;

public class DebuggingThread extends JMeterThread {
    private static final Logger log = LoggingManager.getLoggerForClass();
    private DebuggerCompiler replacedCompiler;
    private final HashTree test;

    private JMeterContext threadContext;


    public DebuggingThread(HashTree test, JMeterThreadMonitor monitor, ListenerNotifier note, JMeterContext ctx) {
        super(test, monitor, note);
        this.test = test;
        threadContext = ctx;
        replacedCompiler = new DebuggerCompiler(test);
        try {
            replaceCompiler();
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to replace test replacedCompiler", e);
        }
    }

    private void replaceCompiler() throws NoSuchFieldException, IllegalAccessException {
        Field field = JMeterThread.class.getDeclaredField("compiler");
        if (!field.isAccessible()) {
            log.debug("Making field accessable: " + field);
            field.setAccessible(true);
        }
        field.set(this, replacedCompiler);
    }

    public Sampler getCurrentSampler() {
        DebuggerSamplerPackage lastSamplePackage = replacedCompiler.getLastSamplePackage();
        if (lastSamplePackage == null) {
            return null;
        }
        return lastSamplePackage.getSampler();
    }

    @Override
    public void run() {
        if (replacedCompiler == null) {
            throw new IllegalStateException("Compiler was not overridden");
        }
        JMeterContextService.replaceContext(threadContext);
        super.run();
    }
}
