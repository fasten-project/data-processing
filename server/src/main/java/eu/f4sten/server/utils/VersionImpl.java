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
package eu.f4sten.server.utils;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.FileUtils.readFileToString;

import java.io.File;
import java.io.IOException;

import eu.f4sten.server.core.utils.Version;

public class VersionImpl implements Version {

	@Override
	public String get() {
		try {
			var url = getClass().getClassLoader().getResource("version.txt");
			if (url == null) {
				return "n/a";
			}
			String version = readFileToString(new File(url.getFile()), UTF_8);
			return version.trim();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}