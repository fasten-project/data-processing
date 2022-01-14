/*
 * Copyright 2021 Delft University of Technology
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.f4sten.server.core;

import java.security.InvalidParameterException;
import java.util.List;
import java.util.function.Function;

import com.beust.jcommander.DefaultUsageFormatter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterDescription;

public class AssertArgs {

    public static <T> ArgsAssertChain<T> assertFor(T argObj) {
        return new ArgsAssertChain<T>(argObj);
    }

    public static <T> void notNull(T argObj, Function<T, Object> accessor, String hint) {
        new ArgsAssertChain<>(argObj).notNull(accessor, hint);
    }

    public static <T> void that(T argObj, Function<T, Boolean> checker, String hint) {
        new ArgsAssertChain<>(argObj).that(checker, hint);
    }

    public static class ArgsAssertChain<T> {

        private T argObj;

        public ArgsAssertChain(T argObj) {
            this.argObj = argObj;
        }

        public ArgsAssertChain<T> notNull(Function<T, Object> accessor, String hint) {
            var prop = accessor.apply(argObj);
            if (prop == null) {
                failWithUsage(argObj, "A requested argument is null", hint);
            }
            return this;
        }

        public ArgsAssertChain<T> that(Function<T, Boolean> checker, String hint) {
            if (!checker.apply(argObj)) {
                failWithUsage(argObj, "A requested argument is invalid", hint);
            }
            return this;
        }

        private void failWithUsage(T argObj, String errType, String hint) {
            var errMsg = String.format("%s (%s)", errType, hint);
            Object defaultArgObj = newInstance(argObj);
            System.out.printf("\n-------------------------\n\n");
            System.out.printf("Insufficient startup arguments:\n-> %s\n\n", errMsg);
            System.out.printf("The *subset* of related arguments that might get requested at runtime:\n");
            var jc = new JCommander(defaultArgObj);
            jc.setUsageFormatter(new MyUsageFormatter(jc));
            jc.usage();
            System.exit(1);
        }

        private static <T> Object newInstance(T obj) {
            try {
                return obj.getClass().getConstructor().newInstance();
            } catch (Exception e) {
                var msg = String.format("Cannot instantiate %s", obj.getClass());
                throw new InvalidParameterException(msg);
            }
        }
    }

    private static final class MyUsageFormatter extends DefaultUsageFormatter {

        private MyUsageFormatter(JCommander commander) {
            super(commander);
        }

        @Override
        public void appendMainLine(StringBuilder out, boolean hasOptions, boolean hasCommands, int indentCount,
                String indent) {
            // skip main line
        }

        @Override
        public void appendAllParametersDetails(StringBuilder out, int indentCount, String indent,
                List<ParameterDescription> sortedParameters) {
            // TODO Auto-generated method stub
            super.appendAllParametersDetails(out, indentCount, indent, sortedParameters);
        }
    }
}