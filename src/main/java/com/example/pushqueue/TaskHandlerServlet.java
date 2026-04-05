package com.example.pushqueue;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Date;
import java.util.logging.Logger;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;

public class TaskHandlerServlet extends HttpServlet {
    private static final Logger log = Logger.getLogger(TaskHandlerServlet.class.getName());

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String taskName = req.getHeader("X-AppEngine-TaskName");
        if (taskName == null) {
            taskName = "Unknown-Task";
        }

        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

        String feature = req.getHeader("X-Feature");
        String creationTimeStr = req.getHeader("X-Creation-Time");
        String executionCountStr = req.getHeader("X-AppEngine-TaskExecutionCount");
        int retryCount = 0;
        if (executionCountStr != null) {
            retryCount = Integer.parseInt(executionCountStr);
        }

        String failParam = req.getParameter("fail");
        if ("true".equals(failParam)) {
            log.warning("Intentionally failing task to trigger retry.");
            
            Entity event = new Entity("TaskEvent");
            event.setProperty("taskName", taskName);
            event.setProperty("status", "FAILED");
            event.setProperty("timestamp", new Date());
            event.setProperty("payload", "Intentionally failed");
            event.setProperty("featureTested", feature);
            event.setProperty("creationTime", creationTimeStr);
            event.setProperty("retryCount", retryCount);
            datastore.put(event);

            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Intentionally failed");
            return;
        }

        log.info("TaskHandlerServlet received a task.");

        // Read custom header
        String customHeader = req.getHeader("X-Custom-Header");
        if (customHeader != null) {
            log.info("Received custom header X-Custom-Header: " + customHeader);
        }

        // Read payload (body)
        StringBuilder buffer = new StringBuilder();
        BufferedReader reader = req.getReader();
        String line;
        while ((line = reader.readLine()) != null) {
            buffer.append(line);
        }
        String payload = buffer.toString();

        if (!payload.isEmpty()) {
            log.info("Received payload: " + payload);
        } else {
            log.info("No payload received.");
        }

        // Record success event
        Entity event = new Entity("TaskEvent");
        event.setProperty("taskName", taskName);
        event.setProperty("status", "SUCCESS");
        event.setProperty("timestamp", new Date());
        event.setProperty("payload", payload);
        event.setProperty("featureTested", feature);
        event.setProperty("creationTime", creationTimeStr);
        event.setProperty("retryCount", retryCount);
        datastore.put(event);

        // Respond with 200 OK to acknowledge task completion
        resp.setStatus(HttpServletResponse.SC_OK);
    }
}
