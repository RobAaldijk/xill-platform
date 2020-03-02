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
package nl.xillio.xill.plugins.mssql;

import com.microsoft.sqlserver.jdbc.SQLServerDriver;

import nl.xillio.xill.plugins.jdbc.services.ConnectionStringFactory;


/**
 * This is the simple connection factory for mssql databases. It fetched the driver and uses jdbc to parseExpression the connection string.
 *
 * @author Thomas Biesaart
 */
class MSSQLConnectionStringFactory extends ConnectionStringFactory {

     @Override
     protected Class<SQLServerDriver> driver() {
         return SQLServerDriver.class;
    }
}
