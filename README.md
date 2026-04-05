# PushQueue Demo App

This application demonstrates the use of App Engine TaskQueue and its zero-code-change migration to Google Cloud Tasks using a forked SDK.

## Features & What to Test
*   **Simple Task:** Enqueue a simple task to the default queue.
*   **Task with Payload:** Enqueue a task with a custom string payload.
*   **Delayed Task:** Enqueue a task with a countdown delay (in seconds).
*   **Task to Custom Queue:** Enqueue a task to a non-default queue (`demo-queue`).
*   **Task with Custom Header:** Enqueue a task with a custom HTTP header.
*   **Batch Create Tasks:** Enqueue multiple tasks in a single RPC call.
*   **Delete Task:** Delete a specific task by name.
*   **Batch Delete Tasks:** Delete multiple tasks by name.
*   **Fetch Queue Statistics:** Retrieve stats like number of tasks and in-flight requests.
*   **List Tasks:** List tasks in a queue (calls Cloud Tasks API directly).
*   **Task with Retry Options:** Enqueue a task with custom retry limits.
*   **Transactional Task:** Tasks added within a Datastore transaction are only dispatched upon successful commit. Supports simulated failure.
*   **Named Task:** Enqueue a task with a specific name.
*   **Task with ETA:** Enqueue a task with a specific execution time (ETA).
*   **Purge Queue:** Remove all tasks from a specific queue.
*   **Task to Age Limit Queue:** Enqueue a task to a queue with age limits configured.
*   **Async/Sync Methods:** All operations support both synchronous and asynchronous execution.

## Forked SDK for Cloud Task Routing
This app uses a custom-built version of the App Engine Java SDK (`appengine-api-1.0-sdk`). The SDK has been modified to intercept TaskQueue calls and transparently route them to Google Cloud Tasks when the environment variable `GAE_PUSHQUEUE_BACKEND=CLOUD_TASK` is set.

It also includes a Transactional Outbox pattern implementation:
*   Tasks are stored in Datastore (`_AE_PendingCloudTask`) during a transaction.
*   Dispatched to Cloud Tasks after commit.
*   Includes an automatic sweeper servlet at `/_ah/cloudtask/sweep` for task recovery.

## How to Run Locally (DevAppServer)

1.  **Build the SDK:** (If not already built)
    Navigate to the SDK directory (`/Users/gajjarc/Desktop/java-taskqueue/appengine-java-standard`) and run:
    ```bash
    JAVA_HOME=/opt/homebrew/Cellar/openjdk/25.0.2/libexec/openjdk.jdk/Contents/Home ./mvnw clean install -DskipTests -Dmaven.javadoc.skip=true
    ```
2.  **Build the Demo App:**
    Navigate to the `pushqueue-demo` directory and run:
    ```bash
    JAVA_HOME=/opt/homebrew/Cellar/openjdk/25.0.2/libexec/openjdk.jdk/Contents/Home ../appengine-java-standard/mvnw clean package
    ```
3.  **Run the Server:**
    To run with Cloud Tasks enabled:
    ```bash
    GAE_PUSHQUEUE_BACKEND=CLOUD_TASK JAVA_HOME=/opt/homebrew/Cellar/openjdk/25.0.2/libexec/openjdk.jdk/Contents/Home ../appengine-java-standard/mvnw appengine:run
    ```
    To run with legacy TaskQueue:
    ```bash
    JAVA_HOME=/opt/homebrew/Cellar/openjdk/25.0.2/libexec/openjdk.jdk/Contents/Home ../appengine-java-standard/mvnw appengine:run
    ```
    *(Note: You can also configure this in `src/main/webapp/WEB-INF/appengine-web.xml`)*

## How to Deploy

To deploy this application to production App Engine while supporting the forked SDK, run the following commands from the `pushqueue-demo` directory:

1.  **Package the application:**
    Ensure the `pom.xml` points to the custom SDK version (e.g., `5.0.1-SNAPSHOT`).
    ```bash
    ../appengine-java-standard/mvnw clean package
    ```
2.  **Deploy using Maven:**
    Use the App Engine Maven plugin to deploy:
    ```bash
    ../appengine-java-standard/mvnw appengine:deploy
    ```
3.  **Ensure Environment Variables:**
    Make sure `GAE_PUSHQUEUE_BACKEND=CLOUD_TASK` is set in your `appengine-web.xml` for production if you want to enable Cloud Tasks routing there.
