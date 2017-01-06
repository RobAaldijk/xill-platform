/**
 * Copyright (C) 2014 Xillio (support@xillio.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.xillio.xill.webservice.model;

import nl.xillio.xill.webservice.exceptions.BaseException;
import nl.xillio.xill.webservice.exceptions.CompileException;
import nl.xillio.xill.webservice.exceptions.RobotNotFoundException;

import java.nio.file.Path;
import java.util.Map;

/**
 * Represents an instance of the Xill environment able to run a single robot with low latency.
 *
 * This implementation allows for {@link Runtime runtimes} to be kept in a pool, preventing
 * unnecessary recreation of environments.
 *
 * @author Geert Konijnendijk
 */
public interface Runtime extends AutoCloseable {

    /**
     * Compiles a robot for future execution.
     *
     * @param workDirectory the working directory
     * @param robotFQN the robot's fully qualified name
     * @throws CompileException if the robot could not be (re) compiled
     * @throws RobotNotFoundException if the robot can not be found
     */
    void compile(Path workDirectory, String robotFQN) throws BaseException;

    /**
     * Runs the robot that was compiled by calling {@link #compile(Path, String)}.
     *
     * This method runs a single robot with low latency in a blocking fashion.
     *
     * @param parameters the parameters to be passed as the robot's arguments
     * @return the robot's return value
     */
    Object runRobot(Map<String, Object> parameters);

    /**
     * Aborts a currently running robot.
     *
     * This method will effectively stop a call to {@link #runRobot(Map)}
     * before the robot has fully finished running.
     */
    void abortRobot();

    /**
     * Shuts down the this runtime and associated resources.
     */
    @Override
    void close();
}