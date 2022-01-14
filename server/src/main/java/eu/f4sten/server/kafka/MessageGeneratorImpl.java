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
package eu.f4sten.server.kafka;

import javax.inject.Inject;

import org.apache.commons.lang3.exception.ExceptionUtils;

import eu.f4sten.server.ServerArgs;
import eu.f4sten.server.core.kafka.Message;
import eu.f4sten.server.core.kafka.Message.Error;
import eu.f4sten.server.core.kafka.MessageGenerator;
import eu.f4sten.server.core.utils.HostName;
import eu.f4sten.server.core.utils.Version;

public class MessageGeneratorImpl implements MessageGenerator {

	private final ServerArgs args;
	private final HostName host;
	private final Version version;

	@Inject
	public MessageGeneratorImpl(ServerArgs args, HostName host, Version version) {
		this.args = args;
		this.host = host;
		this.version = version;
	}

	@Override
	public <Output> Message<?, Output> getStd(Output output) {
		return getStd(null, output);
	}

	@Override
	public <Input, Output> Message<Input, Output> getStd(Input input, Output output) {
		var m = fill(new Message<Input, Output>());
		m.input = input;
		m.payload = output;
		return m;
	}

	@Override
	public <Input> Message<Input, ?> getErr(Input input, Throwable t) {
		var m = fill(new Message<Input, Object>());
		m.input = input;
		m.error = getError(t);
		return m;
	}

	private <Input, Output> Message<Input, Output> fill(Message<Input, Output> m) {
		m.host = host.get();
		m.plugin = args.plugin;
		m.version = version.get();
		return m;
	}

	private static Error getError(Throwable t) {
		var e = new Message.Error();
		e.message = t.getMessage();
		e.type = t.getClass().getSimpleName();
		e.stacktrace = ExceptionUtils.getStackTrace(t);
		return e;
	}
}