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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.postgresql.Driver;

import eu.f4sten.server.core.utils.PostgresConnector;

public class PostgresConnectorImpl implements PostgresConnector {

	private final String dbUrl;
	private final String user;
	private final boolean shouldAutocommit;

	public PostgresConnectorImpl(String dbUrl, String user, boolean shouldAutocommit) {
		this.dbUrl = dbUrl;
		this.user = user;
		this.shouldAutocommit = shouldAutocommit;

	}

	@Override
	public Connection getNewConnection() {
		if (!new Driver().acceptsURL(dbUrl)) {
			throw new IllegalArgumentException("Driver does not accept database URL: " + dbUrl);
		}
		var pwd = System.getenv(PASSWORD_ENV_VAR);
		if (pwd == null) {
			var err = "Postgres password missing. Provide through use ENV variable %s.";
			throw new IllegalArgumentException(String.format(err, PASSWORD_ENV_VAR));
		}

		try {
			var connection = DriverManager.getConnection(dbUrl, user, pwd);
			connection.setAutoCommit(this.shouldAutocommit);
			return connection;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
}