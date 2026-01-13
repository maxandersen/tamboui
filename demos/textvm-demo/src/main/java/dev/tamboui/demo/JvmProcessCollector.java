/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Collects information about JVM processes using the Java Attach API.
 */
class JvmProcessCollector {

    /**
     * Collects a list of all running JVM processes.
     */
    List<JTextVM.JvmProcessInfo> collectProcesses() {
        var processes = new ArrayList<JTextVM.JvmProcessInfo>();
        
        try {
            // Use VirtualMachine.list() to get all JVM processes
            var vmClass = Class.forName("com.sun.tools.attach.VirtualMachine");
            var listMethod = vmClass.getMethod("list");
            @SuppressWarnings("rawtypes")
            List vms = (List) listMethod.invoke(null);
            
            for (var vm : vms) {
                try {
                    var descriptorClass = vm.getClass();
                    var pidMethod = descriptorClass.getMethod("id");
                    var displayNameMethod = descriptorClass.getMethod("displayName");
                    
                    String pidStr = (String) pidMethod.invoke(vm);
                    String displayName = (String) displayNameMethod.invoke(vm);
                    
                    int pid = Integer.parseInt(pidStr);
                    
                    // Try to get more details
                    String mainClass = extractMainClass(displayName);
                    String arguments = extractArguments(displayName);
                    
                    processes.add(new JTextVM.JvmProcessInfo(
                        pid,
                        mainClass,
                        arguments,
                        displayName
                    ));
                } catch (Exception e) {
                    // Skip processes we can't access
                }
            }
        } catch (Exception e) {
            // If attach API is not available, at least show current process
            var runtime = ManagementFactory.getRuntimeMXBean();
            var pid = getCurrentPid();
            var name = runtime.getName();
            processes.add(new JTextVM.JvmProcessInfo(
                pid,
                extractMainClass(name),
                extractArguments(name),
                name
            ));
        }
        
        return processes;
    }

    /**
     * Collects detailed information about a specific JVM process.
     */
    JTextVM.ProcessDetails collectDetails(JTextVM.JvmProcessInfo process) throws Exception {
        // For the current process, use local MBeans
        var currentPid = getCurrentPid();
        if (process.pid() == currentPid) {
            return collectCurrentProcessDetails();
        }
        
        // For other processes, try to attach
        try {
            return collectRemoteProcessDetails(process.pid());
        } catch (Exception e) {
            // If we can't attach, return minimal details
            return new JTextVM.ProcessDetails(
                0, 0, 0, 0,
                0, 0, 0,
                0,
                "Unknown", "Unknown", "Unknown",
                new Properties()
            );
        }
    }

    private JTextVM.ProcessDetails collectCurrentProcessDetails() {
        var memoryBean = ManagementFactory.getMemoryMXBean();
        var runtimeBean = ManagementFactory.getRuntimeMXBean();
        var threadBean = ManagementFactory.getThreadMXBean();
        
        var heapUsage = memoryBean.getHeapMemoryUsage();
        var nonHeapUsage = memoryBean.getNonHeapMemoryUsage();
        
        // Convert Map<String, String> to Properties
        var props = new Properties();
        var systemProps = runtimeBean.getSystemProperties();
        if (systemProps != null) {
            props.putAll(systemProps);
        }
        
        return new JTextVM.ProcessDetails(
            heapUsage.getUsed(),
            heapUsage.getMax(),
            heapUsage.getCommitted(),
            nonHeapUsage.getUsed(),
            threadBean.getThreadCount(),
            threadBean.getPeakThreadCount(),
            threadBean.getDaemonThreadCount(),
            runtimeBean.getUptime(),
            runtimeBean.getVmName(),
            runtimeBean.getVmVersion(),
            runtimeBean.getVmVendor(),
            props
        );
    }

    private JTextVM.ProcessDetails collectRemoteProcessDetails(int pid) throws Exception {
        var vmClass = Class.forName("com.sun.tools.attach.VirtualMachine");
        var attachMethod = vmClass.getMethod("attach", String.class);
        var vm = attachMethod.invoke(null, String.valueOf(pid));
        
        try {
            // This is simplified - in reality, we'd need to use JMX to connect
            // For now, return minimal details
            // Note: Remote process monitoring would require JMX connection setup
            return new JTextVM.ProcessDetails(
                0, 0, 0, 0,
                0, 0, 0,
                0,
                "Remote", "Unknown", "Unknown",
                new Properties()
            );
        } finally {
            var detachMethod = vmClass.getMethod("detach");
            detachMethod.invoke(vm);
        }
    }

    private int getCurrentPid() {
        var runtime = ManagementFactory.getRuntimeMXBean();
        var name = runtime.getName();
        // Format is typically "pid@hostname"
        try {
            return Integer.parseInt(name.split("@")[0]);
        } catch (Exception e) {
            return -1;
        }
    }

    private String extractMainClass(String displayName) {
        if (displayName == null || displayName.isEmpty()) {
            return "Unknown";
        }
        // Try to extract main class from display name
        // Format varies: "com.example.Main", "com.example.Main arg1 arg2", etc.
        var parts = displayName.trim().split("\\s+");
        if (parts.length > 0) {
            return parts[0];
        }
        return displayName;
    }

    private String extractArguments(String displayName) {
        if (displayName == null || displayName.isEmpty()) {
            return "";
        }
        var parts = displayName.trim().split("\\s+", 2);
        if (parts.length > 1) {
            return parts[1];
        }
        return "";
    }
}

