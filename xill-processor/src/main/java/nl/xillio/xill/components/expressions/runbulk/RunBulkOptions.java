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
package nl.xillio.xill.components.expressions.runbulk;

import nl.xillio.xill.api.NullDebugger;
import nl.xillio.xill.api.components.ExpressionDataType;
import nl.xillio.xill.api.components.MetaExpression;
import nl.xillio.xill.api.components.Processable;
import nl.xillio.xill.api.errors.RobotRuntimeException;

import java.util.Map;

/**
 * Parses options of the {@link RunBulkExpression}
 */
class RunBulkOptions {

    private final Processable options;

    private boolean stopOnError;
    private int maxThreadsVal;

    /**
     * @param options The processable to parse options from
     */
    public RunBulkOptions(Processable options) {
        this.options = options;
        this.stopOnError = false;
        this.maxThreadsVal = 0;
        parseOptions();
    }

    /**
     * Get the stopOnError option
     * @return Whether to stop threads when an error occurs
     */
    public boolean shouldStopOnError() {
        return stopOnError;
    }

    /**
     * Get the maxThreadsVal option
     * @return How many workers to start
     */
    public int getMaxThreadsVal() {
        return maxThreadsVal;
    }

    /**
     * Parse the option expression
     */
    private void parseOptions() {
        if (options == null) {
            return;
        }
        MetaExpression optionVar = options.process(new NullDebugger()).get();
        if (optionVar.isNull()) {
            return;
        }

        if (optionVar.getType() != ExpressionDataType.OBJECT) {
            throw new RobotRuntimeException("Invalid options type");
        }

        parseOptions(optionVar);
    }

    /**
     * Parse supported options from an OBJECT {@link MetaExpression}
     * @param optionVar The options OBJECT
     */
    private void parseOptions(MetaExpression optionVar) {
        Map<String, MetaExpression> optionParameters = optionVar.getValue();

        for (Map.Entry<String, MetaExpression> entry : optionParameters.entrySet()) {
            switch (entry.getKey()) {
                case "maxThreads":
                    parseMaxThreads(entry.getValue());
                    break;
                case "stopOnError":
                    parseStopOnError(entry.getValue());
                    break;
                default:
                    throw new RobotRuntimeException("Invalid option");
            }
        }
    }

    /**
     * Parse the stopOnError option from a {@link MetaExpression}
     * @param value The option value
     */
    private void parseStopOnError(MetaExpression value) {
        String stringValue = value.getStringValue();
        if ("yes".equals(stringValue)) {
            stopOnError = true;
        } else if ("no".equals(stringValue)) {
            stopOnError = false;
        } else {
            throw new RobotRuntimeException("Invalid onError value");
        }
    }

    /**
     * Parse the maxThreads option from a {@link MetaExpression}
     * @param value The option value
     */
    private void parseMaxThreads(MetaExpression value) {
        maxThreadsVal = value.getNumberValue().intValue();
        if (maxThreadsVal < 1) {
            throw new RobotRuntimeException("Invalid maxThreads value");
        }
    }

}
