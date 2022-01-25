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
package eu.f4sten.loader.utils;

import static java.lang.String.format;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import com.beust.jcommander.JCommander;

public class ArgsParser {

    private final Map<String, Class<?>> parameters = new HashMap<>();
    private final String[] rawArgs;

    public ArgsParser(String[] rawArgs) {
        this.rawArgs = rawArgs;
    }

    public <T> T parse(Class<T> paramType) {
        var argsObj = initDefault(paramType);
        var jc = JCommander.newBuilder() //
                .addObject(argsObj) //
                .acceptUnknownOptions(true) //
                .build();
        jc.parse(rawArgs);
        checkForDuplicates(paramType, jc);
        return argsObj;
    }

    private <T> void checkForDuplicates(Class<T> c, JCommander jc) {
        for (var f : jc.getFields().values()) {
            for (var n : f.getParameter().names()) {
                if (!parameters.containsKey(n)) {
                    parameters.put(n, c);
                    continue;
                }
                var c2 = parameters.get(n);
                if (c.equals(c2)) {
                    // the same argsObj is being loaded again
                    continue;
                }
                var msg = format("The parameter %s is defined in both %s and %s", n, c.getName(), c2.getName());
                throw new IllegalStateException(msg);
            }
        }
    }

    private static <T> T initDefault(Class<T> type) {
        String msg;
        try {
            return type.getConstructor().newInstance();
        } catch (NoSuchMethodException e) {
            msg = format("Argument parsing class %s does not have an (implicit) default constructor.", type.getName());
        } catch (InvocationTargetException e) {
            var causingClass = e.getCause().getClass().getName();
            var causingMsg = e.getCause().getMessage();
            msg = format("Default constructor of %s failed with %s: %s", type.getName(), causingClass, causingMsg);
        } catch (Exception e) {
            msg = format("Failed to initialize %s", type);
        }
        throw new IllegalStateException(msg);
    }
}