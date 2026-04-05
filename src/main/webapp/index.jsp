<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.google.appengine.api.datastore.DatastoreService" %>
<%@ page import="com.google.appengine.api.datastore.DatastoreServiceFactory" %>
<%@ page import="com.google.appengine.api.datastore.Entity" %>
<%@ page import="com.google.appengine.api.datastore.Query" %>
<%@ page import="com.google.appengine.api.datastore.FetchOptions" %>
<%@ page import="java.util.List" %>
<!DOCTYPE html>
<html>
<head>
    <title>PushQueue Demo</title>
    <style>
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: #333;
            margin: 0;
            padding: 20px;
            display: flex;
            justify-content: center;
            align-items: center;
            min-height: 100vh;
        }
        .container {
            background: rgba(255, 255, 255, 0.95);
            padding: 30px;
            border-radius: 15px;
            box-shadow: 0 10px 25px rgba(0,0,0,0.2);
            max-width: 600px;
            width: 100%;
        }
        h1 {
            color: #4a148c;
            text-align: center;
            margin-bottom: 25px;
        }
        .feature-card {
            background: #fff;
            border: 1px solid #e0e0e0;
            border-radius: 8px;
            padding: 15px;
            margin-bottom: 15px;
            transition: transform 0.2s;
        }
        .feature-card:hover {
            transform: translateY(-3px);
            box-shadow: 0 5px 15px rgba(0,0,0,0.1);
        }
        h3 {
            margin-top: 0;
            color: #7b1fa2;
        }
        form {
            display: flex;
            flex-direction: column;
            gap: 10px;
        }
        label {
            font-weight: bold;
            font-size: 0.9em;
        }
        input[type="text"], input[type="number"] {
            padding: 8px;
            border: 1px solid #ccc;
            border-radius: 4px;
        }
        button {
            background: #7b1fa2;
            color: white;
            border: none;
            padding: 10px 15px;
            border-radius: 5px;
            cursor: pointer;
            font-weight: bold;
            transition: background 0.2s;
        }
        button:hover {
            background: #4a148c;
        }
        .success-msg {
            color: green;
            text-align: center;
            font-weight: bold;
        }
    </style>
</head>
<body>
    <div class="container">
        <h1>PushQueue Feature Demo</h1>
        
        <% if (request.getParameter("success") != null) { %>
            <div class="success-msg">
                Success! Action: <%= request.getParameter("action") %>
                <% if (request.getParameter("taskId") != null && !request.getParameter("taskId").isEmpty()) { %>
                     | Task ID: <%= request.getParameter("taskId") %>
                <% } %>
            </div>
        <% } %>

        <% if (request.getParameter("error") != null) { %>
            <div class="error-msg" style="color: #d32f2f; text-align: center; font-weight: bold; margin-bottom: 20px; padding: 10px; background: #ffebee; border-radius: 5px;">
                Error: <%= request.getParameter("error") %>
            </div>
        <% } %>

        <div class="feature-card">
            <h3>1. Simple Task (Default Queue)</h3>
            <form action="push" method="POST">
                <input type="hidden" name="action" value="simple">
                <label style="display: flex; align-items: center; gap: 10px;">
                    <input type="checkbox" name="async" value="true">
                    Perform Asynchronously
                </label>
                <button type="submit">Add Simple Task</button>
            </form>
        </div>

        <div class="feature-card">
            <h3>2. Task with Payload</h3>
            <form action="push" method="POST">
                <input type="hidden" name="action" value="payload">
                <label for="payload">Payload Data:</label>
                <input type="text" id="payload" name="payload" value="Hello from PushQueue!" required>
                <label style="display: flex; align-items: center; gap: 10px;">
                    <input type="checkbox" name="async" value="true">
                    Perform Asynchronously
                </label>
                <button type="submit">Add Task with Payload</button>
            </form>
        </div>

        <div class="feature-card">
            <h3>3. Delayed Task</h3>
            <form action="push" method="POST">
                <input type="hidden" name="action" value="delay">
                <label for="delay">Delay (seconds):</label>
                <input type="number" id="delay" name="delay" value="10" required>
                <label style="display: flex; align-items: center; gap: 10px;">
                    <input type="checkbox" name="async" value="true">
                    Perform Asynchronously
                </label>
                <button type="submit">Add Delayed Task</button>
            </form>
        </div>

        <div class="feature-card">
            <h3>4. Task to Custom Queue</h3>
            <form action="push" method="POST">
                <input type="hidden" name="action" value="custom-queue">
                <label style="display: flex; align-items: center; gap: 10px;">
                    <input type="checkbox" name="async" value="true">
                    Perform Asynchronously
                </label>
                <button type="submit">Add Task to 'demo-queue'</button>
            </form>
        </div>

        <div class="feature-card">
            <h3>5. Task with Custom Header</h3>
            <form action="push" method="POST">
                <input type="hidden" name="action" value="header">
                <label for="header-val">X-Custom-Header Value:</label>
                <input type="text" id="header-val" name="header-val" value="MyCustomValue" required>
                <label style="display: flex; align-items: center; gap: 10px;">
                    <input type="checkbox" name="async" value="true">
                    Perform Asynchronously
                </label>
                <button type="submit">Add Task with Header</button>
            </form>
        </div>

        <div class="feature-card">
            <h3>6. Batch Create Tasks</h3>
            <form action="push" method="POST">
                <input type="hidden" name="action" value="batch-create">
                <label style="display: flex; align-items: center; gap: 10px;">
                    <input type="checkbox" name="async" value="true">
                    Perform Asynchronously
                </label>
                <button type="submit">Create Batch of Tasks</button>
            </form>
        </div>

        <div class="feature-card">
            <h3>7. Trigger Retry Task (Failing task)</h3>
            <form action="push" method="POST">
                <input type="hidden" name="action" value="retry-task">
                <label for="retry-limit">Task Retry Limit:</label>
                <input type="number" id="retry-limit" name="retry-limit" value="3" required>
                <label style="display: flex; align-items: center; gap: 10px;">
                    <input type="checkbox" name="async" value="true">
                    Perform Asynchronously
                </label>
                <button type="submit">Trigger Failing Task</button>
            </form>
        </div>

        <div class="feature-card">
            <h3>8. Delete Task</h3>
            <form action="push" method="POST">
                <input type="hidden" name="action" value="delete-task">
                <label for="task-name">Task Name (or ID) to Delete:</label>
                <input type="text" id="task-name" name="task-name" placeholder="Task Name" required>
                <label style="display: flex; align-items: center; gap: 10px;">
                    <input type="checkbox" name="async" value="true">
                    Perform Asynchronously
                </label>
                <button type="submit">Delete Task</button>
            </form>
        </div>

        <div class="feature-card">
            <h3>9. View Queue Statistics</h3>
            <form action="push" method="POST">
                <input type="hidden" name="action" value="stats">
                <label style="display: flex; align-items: center; gap: 10px;">
                    <input type="checkbox" name="async" value="true">
                    Perform Asynchronously
                </label>
                <button type="submit">Fetch Stats</button>
            </form>
        </div>

        <div class="feature-card">
            <h3>10. Transactional Task Demo</h3>
            <form action="push" method="POST">
                <input type="hidden" name="action" value="transactional-task">
                <label style="display: flex; align-items: center; gap: 10px;">
                    <input type="checkbox" name="simulate-fail" value="true">
                    Simulate Transaction Failure (Rollback)
                </label>
                <label style="display: flex; align-items: center; gap: 10px;">
                    <input type="checkbox" name="async" value="true">
                    Perform Asynchronously
                </label>
                <button type="submit">Push Transactional Task</button>
            </form>
        </div>

        <div class="feature-card">
            <h3>11. Named Task Deduplication</h3>
            <form action="push" method="POST">
                <input type="hidden" name="action" value="named-task">
                <label for="task-name-input">Task Name (User specified):</label>
                <input type="text" id="task-name-input" name="task-name" placeholder="my-named-task-123" required>
                <label style="display: flex; align-items: center; gap: 10px;">
                    <input type="checkbox" name="async" value="true">
                    Perform Asynchronously
                </label>
                <button type="submit">Push Named Task</button>
            </form>
        </div>

        <div class="feature-card">
            <h3>12. Scheduled ETA Task</h3>
            <form action="push" method="POST">
                <input type="hidden" name="action" value="eta-task">
                <label style="display: flex; align-items: center; gap: 10px;">
                    <input type="checkbox" name="async" value="true">
                    Perform Asynchronously
                </label>
                <button type="submit">Push Task (Runs in 1 Min)</button>
            </form>
        </div>

        <div class="feature-card">
            <h3>13. Purge Queue</h3>
            <form action="push" method="POST">
                <input type="hidden" name="action" value="purge-queue">
                <label for="purge-queue-name">Queue Name:</label>
                <input type="text" id="purge-queue-name" name="queue-name" value="demo-queue" required>
                <button type="submit" style="background: #e53935;">Purge Queue</button>
            </form>
        </div>

        <div class="feature-card">
            <h3>14. Task with Age Limit (queue.xml)</h3>
            <form action="push" method="POST">
                <input type="hidden" name="action" value="age-limit-task">
                <label style="display: flex; align-items: center; gap: 10px;">
                    <input type="checkbox" name="async" value="true">
                    Perform Asynchronously
                </label>
                <button type="submit">Push Task to age-limit-queue (Fails & Stops in 1 Min)</button>
            </form>
        </div>

        <div class="feature-card">
            <h3>15. List Tasks (Delayed Tasks)</h3>
            <form action="push" method="POST">
                <input type="hidden" name="action" value="list-tasks">
                <label for="list-queue-name">Queue Name:</label>
                <input type="text" id="list-queue-name" name="queue-name" value="default" required>
                <button type="submit">List Tasks</button>
            </form>
        </div>

        <% if (request.getAttribute("taskNames") != null) { %>
            <div class="feature-card" style="background: #e8f5e9;">
                <h3>Current Tasks in Queue</h3>
                <%
                    List<String> taskNames = (List<String>) request.getAttribute("taskNames");
                    List<String> scheduleTimes = (List<String>) request.getAttribute("scheduleTimes");
                    if (taskNames.isEmpty()) {
                %>
                    <p>No tasks found in queue.</p>
                <% } else { %>
                    <table style="width:100%; border-collapse: collapse; font-size: 0.8em;">
                        <thead>
                            <tr style="background: #c8e6c9;">
                                <th style="padding: 5px; border: 1px solid #a5d6a7;">Task Name</th>
                                <th style="padding: 5px; border: 1px solid #a5d6a7;">Schedule Time</th>
                                <th style="padding: 5px; border: 1px solid #a5d6a7;">Action</th>
                            </tr>
                        </thead>
                        <tbody>
                            <% for (int i = 0; i < taskNames.size(); i++) { %>
                                <tr>
                                    <td style="padding: 5px; border: 1px solid #e0e0e0;"><%= taskNames.get(i) %></td>
                                    <td style="padding: 5px; border: 1px solid #e0e0e0;"><%= i < scheduleTimes.size() ? scheduleTimes.get(i) : "N/A" %></td>
                                    <td style="padding: 5px; border: 1px solid #e0e0e0;">
                                        <form action="push" method="POST" style="display:inline;">
                                            <input type="hidden" name="action" value="delete-task">
                                            <input type="hidden" name="task-name" value="<%= taskNames.get(i) %>">
                                            <button type="submit" style="background: #e53935; padding: 2px 5px; font-size: 0.8em;">Delete</button>
                                        </form>
                                    </td>
                                </tr>
                            <% } %>
                        </tbody>
                    </table>
                <% } %>
            </div>
        <% } %>

        <% if (request.getAttribute("stats_numTasks") != null) { %>
            <div class="feature-card" style="background: #e1bee7;">
                <h3>Current Stats</h3>
                <p><strong>Approx Tasks in Queue:</strong> <%= request.getAttribute("stats_numTasks") %></p>
                <p><strong>Executed Last Min:</strong> <%= request.getAttribute("stats_execLastMin") %></p>
                <p><strong>In Flight Requests:</strong> <%= request.getAttribute("stats_inFlight") %></p>
            </div>
        <% } %>

        <div class="feature-card" style="background: #e3f2fd;">
            <h3>Asynchronous Processing Events (Last 10)</h3>
            <%
                DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
                Query q = new Query("TaskEvent").addSort("timestamp", Query.SortDirection.DESCENDING);
                List<Entity> events = ds.prepare(q).asList(FetchOptions.Builder.withLimit(10));
                if (events.isEmpty()) {
            %>
                <p>No events recorded yet. Push some tasks!</p>
            <% } else { %>
                <table style="width:100%; border-collapse: collapse; font-size: 0.8em;">
                    <thead>
                        <tr style="background: #bbdefb;">
                            <th style="padding: 5px; border: 1px solid #90caf9;">Task Name</th>
                            <th style="padding: 5px; border: 1px solid #90caf9;">Feature</th>
                            <th style="padding: 5px; border: 1px solid #90caf9;">Payload</th>
                            <th style="padding: 5px; border: 1px solid #90caf9;">Status</th>
                            <th style="padding: 5px; border: 1px solid #90caf9;">Retry</th>
                            <th style="padding: 5px; border: 1px solid #90caf9;">Created</th>
                            <th style="padding: 5px; border: 1px solid #90caf9;">Processed</th>
                        </tr>
                    </thead>
                    <tbody>
                        <% for (Entity event : events) { 
                            long cTime = 0;
                            String cTimeStr = (String) event.getProperty("creationTime");
                            if (cTimeStr != null) {
                                cTime = Long.parseLong(cTimeStr);
                            }
                            java.util.Date createdDate = cTime > 0 ? new java.util.Date(cTime) : null;
                        %>
                            <tr>
                                <td style="padding: 5px; border: 1px solid #e0e0e0;"><%= event.getProperty("taskName") %></td>
                                <td style="padding: 5px; border: 1px solid #e0e0e0;"><%= event.getProperty("featureTested") %></td>
                                <td style="padding: 5px; border: 1px solid #e0e0e0; max-width: 150px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;"><%= event.getProperty("payload") %></td>
                                <td style="padding: 5px; border: 1px solid #e0e0e0; font-weight: bold; color: <%= "FAILED".equals(event.getProperty("status")) ? "#d32f2f" : "#388e3c" %>;"><%= event.getProperty("status") %></td>
                                <td style="padding: 5px; border: 1px solid #e0e0e0;"><%= event.getProperty("retryCount") %></td>
                                <td style="padding: 5px; border: 1px solid #e0e0e0;"><%= createdDate != null ? createdDate : "N/A" %></td>
                                <td style="padding: 5px; border: 1px solid #e0e0e0;"><%= event.getProperty("timestamp") %></td>
                            </tr>
                        <% } %>
                    </tbody>
                </table>
            <% } %>
        </div>
    </div>
</body>
</html>
