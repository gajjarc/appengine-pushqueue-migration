package com.example.pushqueue;

import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.appengine.api.taskqueue.RetryOptions;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.net.URLEncoder;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;
import com.google.appengine.api.taskqueue.QueueStatistics;
import com.google.appengine.api.taskqueue.TaskHandle;
import com.google.appengine.api.taskqueue.TaskAlreadyExistsException;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Transaction;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import com.google.appengine.api.appidentity.AppIdentityService;
import com.google.appengine.api.appidentity.AppIdentityServiceFactory;

import static com.google.appengine.api.taskqueue.TaskOptions.Builder.withUrl;

public class PushQueueDemoServlet extends HttpServlet {
    private static final Logger log = Logger.getLogger(PushQueueDemoServlet.class.getName());

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action");
        boolean isAsync = "true".equals(req.getParameter("async"));
        log.info("Handling action: " + action + " (Async: " + isAsync + ")");

        Queue queue = QueueFactory.getDefaultQueue();
        String taskId = "";
        TaskOptions options = withUrl("/task-handler")
                .header("X-Feature", action)
                .header("X-Creation-Time", String.valueOf(System.currentTimeMillis()));

        try {
            if ("simple".equals(action)) {
                if (isAsync) {
                    Future<TaskHandle> future = queue.addAsync(options);
                    taskId = future.get().getName();
                } else {
                    TaskHandle h = queue.add(options);
                    taskId = h.getName();
                }
            } else if ("payload".equals(action)) {
                String payload = req.getParameter("payload");
                options = options.payload(payload);
                if (isAsync) {
                    Future<TaskHandle> future = queue.addAsync(options);
                    taskId = future.get().getName();
                } else {
                    TaskHandle h = queue.add(options);
                    taskId = h.getName();
                }
            } else if ("delay".equals(action)) {
                String delayStr = req.getParameter("delay");
                int delaySeconds = Integer.parseInt(delayStr);
                options = options.countdownMillis(delaySeconds * 1000L);
                if (isAsync) {
                    Future<TaskHandle> future = queue.addAsync(options);
                    taskId = future.get().getName();
                } else {
                    TaskHandle h = queue.add(options);
                    taskId = h.getName();
                }
            } else if ("custom-queue".equals(action)) {
                Queue customQueue = QueueFactory.getQueue("demo-queue");
                if (isAsync) {
                    Future<TaskHandle> future = customQueue.addAsync(options);
                    taskId = future.get().getName();
                } else {
                    TaskHandle h = customQueue.add(options);
                    taskId = h.getName();
                }
            } else if ("header".equals(action)) {
                String headerVal = req.getParameter("header-val");
                options = options.header("X-Custom-Header", headerVal);
                if (isAsync) {
                    Future<TaskHandle> future = queue.addAsync(options);
                    taskId = future.get().getName();
                } else {
                    TaskHandle h = queue.add(options);
                    taskId = h.getName();
                }
            } else if ("batch-create".equals(action)) {
                String now = String.valueOf(System.currentTimeMillis());
                List<TaskOptions> tasks = new ArrayList<>();
                tasks.add(withUrl("/task-handler").header("X-Feature", action).header("X-Creation-Time", now).payload("Batch task 1"));
                tasks.add(withUrl("/task-handler").header("X-Feature", action).header("X-Creation-Time", now).payload("Batch task 2"));
                if (isAsync) {
                    Future<List<TaskHandle>> future = queue.addAsync(tasks);
                    List<TaskHandle> handles = future.get();
                    if (!handles.isEmpty()) {
                        taskId = handles.get(0).getName() + " (and others)";
                    }
                } else {
                    List<TaskHandle> handles = queue.add(tasks);
                    if (!handles.isEmpty()) {
                        taskId = handles.get(0).getName() + " (and others)";
                    }
                }
            } else if ("delete-task".equals(action)) {
                String taskName = req.getParameter("task-name");
                if (isAsync) {
                    Future<Boolean> future = queue.deleteTaskAsync(taskName);
                    future.get();
                    taskId = taskName;
                } else {
                    queue.deleteTask(taskName);
                    taskId = taskName;
                }
            } else if ("batch-delete".equals(action)) {
                String taskNamesStr = req.getParameter("task-names");
                List<String> taskNames = Arrays.asList(taskNamesStr.split(","));
                if (isAsync) {
                    List<Future<Boolean>> futures = new ArrayList<>();
                    for (String name : taskNames) {
                        futures.add(queue.deleteTaskAsync(name.trim()));
                    }
                    for (Future<Boolean> f : futures) {
                        f.get();
                    }
                    taskId = "Batch deleted";
                } else {
                    for (String name : taskNames) {
                        queue.deleteTask(name.trim());
                    }
                    taskId = "Batch deleted";
                }
            } else if ("stats".equals(action)) {
                QueueStatistics stats;
                if (isAsync) {
                    Future<QueueStatistics> future = queue.fetchStatisticsAsync(60.0);
                    stats = future.get();
                } else {
                    stats = queue.fetchStatistics();
                }
                req.setAttribute("stats_numTasks", stats.getNumTasks());
                req.setAttribute("stats_execLastMin", stats.getExecutedLastMinute());
                req.setAttribute("stats_inFlight", stats.getRequestsInFlight());
                req.getRequestDispatcher("index.jsp").forward(req, resp);
                return; // Don't redirect
            } else if ("list-tasks".equals(action)) {
                AppIdentityService appIdentityService = AppIdentityServiceFactory.getAppIdentityService();
                AppIdentityService.GetAccessTokenResult tokenResult = appIdentityService.getAccessToken(Arrays.asList("https://www.googleapis.com/auth/cloud-platform"));
                String token = tokenResult.getAccessToken();
                
                String projectId = "gae-direct-vpc";
                String location = "us-east1";
                String queueName = req.getParameter("queue-name");
                if (queueName == null || queueName.isEmpty()) queueName = "default";
                String fullQueueName = "projects/" + projectId + "/locations/" + location + "/queues/" + queueName;
                
                List<String> taskNames = new ArrayList<>();
                List<String> scheduleTimes = new ArrayList<>();
                
                try {
                    java.net.URL url = new java.net.URL("https://cloudtasks.googleapis.com/v2beta3/" + fullQueueName + "/tasks");
                    java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("Authorization", "Bearer " + token);
                    
                    int responseCode = conn.getResponseCode();
                    if (responseCode == 200) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        String inputLine;
                        StringBuilder responseContent = new StringBuilder();
                        while ((inputLine = in.readLine()) != null) {
                            responseContent.append(inputLine);
                        }
                        in.close();
                        
                        String json = responseContent.toString();
                        
                        Pattern p = Pattern.compile("\"name\":\\s*\"([^\"]+)\"");
                        Matcher m = p.matcher(json);
                        while (m.find()) {
                            String fullName = m.group(1);
                            String shortName = fullName.substring(fullName.lastIndexOf("/") + 1);
                            taskNames.add(shortName);
                        }
                        
                        p = Pattern.compile("\"scheduleTime\":\\s*\"([^\"]+)\"");
                        m = p.matcher(json);
                        while (m.find()) {
                            scheduleTimes.add(m.group(1));
                        }
                    } else {
                        System.err.println("CLOUDTASK: List tasks failed with code " + responseCode);
                    }
                } catch (Exception e) {
                    System.err.println("CLOUDTASK: Failed to list tasks: " + e.getMessage());
                }
                
                req.setAttribute("taskNames", taskNames);
                req.setAttribute("scheduleTimes", scheduleTimes);
                req.getRequestDispatcher("index.jsp").forward(req, resp);
                return;
            } else if ("retry-task".equals(action)) {
                String limitStr = req.getParameter("retry-limit");
                int limit = 3;
                if (limitStr != null && !limitStr.isEmpty()) {
                    limit = Integer.parseInt(limitStr);
                }
                RetryOptions retryOptions = RetryOptions.Builder.withTaskRetryLimit(limit);
                options = options.param("fail", "true").retryOptions(retryOptions);
                if (isAsync) {
                    Future<TaskHandle> future = queue.addAsync(options);
                    taskId = future.get().getName();
                } else {
                    TaskHandle h = queue.add(options);
                    taskId = h.getName();
                }
            } else if ("transactional-task".equals(action)) {
                boolean failTxn = "true".equals(req.getParameter("simulate-fail"));
                DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
                Transaction txn = ds.beginTransaction();
                try {
                    options = options.header("X-Transactional", "true");
                    TaskHandle h;
                    if (isAsync) {
                        Future<TaskHandle> future = queue.addAsync(txn, options);
                        h = future.get();
                    } else {
                        h = queue.add(txn, options);
                    }
                    taskId = h.getName();
                    
                    if (failTxn) {
                        throw new RuntimeException("Simulated Transaction Failure");
                    }
                    txn.commit();
                } catch (Exception e) {
                    if (txn.isActive()) {
                        txn.rollback();
                    }
                    log.warning("Transaction failed or rolled back: " + e.getMessage());
                    resp.sendRedirect("index.jsp?error=" + URLEncoder.encode("Transaction failed: " + e.getMessage(), "UTF-8"));
                    return; // Suppress redirect to success page
                }
            } else if ("named-task".equals(action)) {
                String name = req.getParameter("task-name");
                if (name == null || name.isEmpty()) {
                    name = "unnamed-task-" + System.currentTimeMillis();
                }
                options = options.taskName(name);
                try {
                    if (isAsync) {
                        Future<TaskHandle> future = queue.addAsync(options);
                        taskId = future.get().getName();
                    } else {
                        TaskHandle h = queue.add(options);
                        taskId = h.getName();
                    }
                } catch (TaskAlreadyExistsException e) {
                    log.warning("Task already exists: " + name);
                    resp.sendRedirect("index.jsp?error=" + URLEncoder.encode("Task already exists with name: " + name, "UTF-8"));
                    return; // Suppress redirect to success page
                }
            } else if ("eta-task".equals(action)) {
                options = options.etaMillis(System.currentTimeMillis() + 60000);
                if (isAsync) {
                    Future<TaskHandle> future = queue.addAsync(options);
                    taskId = future.get().getName();
                } else {
                    TaskHandle h = queue.add(options);
                    taskId = h.getName();
                }
            } else if ("purge-queue".equals(action)) {
                String qName = req.getParameter("queue-name");
                if (qName == null || qName.isEmpty()) qName = "default";
                Queue demoQueue = QueueFactory.getQueue(qName);
                demoQueue.purge();
                taskId = qName + " purged";
            } else if ("age-limit-task".equals(action)) {
                Queue ageQueue = QueueFactory.getQueue("age-limit-queue");
                options = options.param("fail", "true").header("X-Feature", "age-limit-task");
                if (isAsync) {
                    Future<TaskHandle> future = ageQueue.addAsync(options);
                    taskId = future.get().getName();
                } else {
                    TaskHandle h = ageQueue.add(options);
                    taskId = h.getName();
                }
            } else {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown action");
                return;
            }
        } catch (InterruptedException | ExecutionException e) {
            log.warning("Async operation failed: " + e.getMessage());
            resp.sendRedirect("index.jsp?error=" + URLEncoder.encode("Async operation failed: " + e.getMessage(), "UTF-8"));
            return;
        }

        // Redirect back to index.jsp with success message and details
        resp.sendRedirect("index.jsp?success=true&action=" + action + "&taskId=" + taskId);
    }
}
