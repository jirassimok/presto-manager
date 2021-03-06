/*
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
 */
package com.teradata.prestomanager.agent;

import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import io.airlift.log.Logger;

import javax.annotation.concurrent.ThreadSafe;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import static java.lang.String.format;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static java.util.concurrent.TimeUnit.SECONDS;

@ThreadSafe
public final class CommandExecutor
{
    private static final Logger LOGGER = Logger.get(CommandExecutor.class);

    private final int longTimeout;
    private final int shortTimeout;

    @Inject
    private CommandExecutor(AgentConfig config)
    {
        this.longTimeout = config.getLongSubprocessTimeout();
        this.shortTimeout = config.getShortSubprocessTimeout();
    }

    public CommandResult getCommandResult(String... command)
            throws PrestoManagerException
    {
        return execute(command, shortTimeout);
    }

    public CommandResult getLongCommandResult(String... command)
            throws PrestoManagerException
    {
        return execute(command, longTimeout);
    }

    public int runCommand(String... command)
            throws PrestoManagerException
    {
        return execute(command, shortTimeout).getExitValue();
    }

    public int runLongCommand(String... command)
            throws PrestoManagerException
    {
        return execute(command, longTimeout).getExitValue();
    }

    private CommandResult execute(String[] commandArray, int timeout)
            throws PrestoManagerException
    {
        if (commandArray.length == 0) {
            throw new IllegalArgumentException("Command array is empty; no command given");
        }

        String commandString = String.join(" ", commandArray);
        LOGGER.debug("Command to be executed: %s", commandString);

        ProcessBuilder processBuilder = new ProcessBuilder(commandArray);
        processBuilder.redirectErrorStream(true);
        try {
            Process process = processBuilder.start();
            ExecutorService executor = newSingleThreadExecutor();
            Future<String> outputThread = executor.submit(() -> getProcessOutput(process));
            String output;
            try {
               output =  outputThread.get(timeout, SECONDS);
            }
            catch (TimeoutException e) {
                process.destroyForcibly();
                throw new PrestoManagerException(format("Command timed out: %s", commandString), e);
            }
            finally {
                executor.shutdownNow();
            }
            LOGGER.info("Output from command: %s\n%s", commandString, output);
            int exitValue = process.exitValue();
            return new CommandResult(output, exitValue);
        }
        catch (IOException | ExecutionException | InterruptedException e) {
            throw new PrestoManagerException(format("Error executing command: %s", commandString), e);
        }
    }

    private static String getProcessOutput(Process process)
    {
        try (InputStream processStream = process.getInputStream()) {
            String result = new String(ByteStreams.toByteArray(processStream));
            process.waitFor();
            return result;
        }
        catch (IOException e) {
            LOGGER.error(e, "Failed to retrieve the process output");
        }
        catch (InterruptedException e) {
            LOGGER.error(e, "Process failed to complete.");
        }
        return null;
    }

    public static final class CommandResult
    {
        private final String output;
        private final int exitValue;

        public CommandResult(String output, int exitValue)
        {
            this.output = output;
            this.exitValue = exitValue;
        }

        public int getExitValue()
        {
            return exitValue;
        }

        public String getOutput()
        {
            return output;
        }
    }
}
